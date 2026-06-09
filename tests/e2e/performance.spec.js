const { test, expect, chromium } = require('@playwright/test');
const fs = require('node:fs');

const baseURL = process.env.KRAFT_E2E_BASE_URL || 'http://localhost:18080';
const MIN_PERFORMANCE_SCORE = 0.85;
const LIGHTHOUSE_MAX_ATTEMPTS = 3;
const LIGHTHOUSE_ATTEMPT_TIMEOUT_MS = 75_000;
const LIGHTHOUSE_RUN_TIMEOUT_MS = LIGHTHOUSE_MAX_ATTEMPTS * LIGHTHOUSE_ATTEMPT_TIMEOUT_MS + 30_000;

test.describe('performance smoke', () => {
  test('home page Lighthouse performance score is at least 0.8', async ({}, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Lighthouse performance gate runs once on desktop Chromium');
    test.setTimeout(LIGHTHOUSE_RUN_TIMEOUT_MS);

    const [{ default: lighthouse }, chromeLauncher] = await Promise.all([
      import('lighthouse'),
      import('chrome-launcher'),
    ]);
    const chromePath = resolveChromePath();

    expect(chromePath, 'Chrome executable is required for the Lighthouse performance gate').toBeTruthy();
    const url = new URL('/', baseURL).toString();
    let score;
    let lastError;

    for (let attempt = 1; attempt <= LIGHTHOUSE_MAX_ATTEMPTS; attempt += 1) {
      const chrome = await chromeLauncher.launch({
        chromePath,
        chromeFlags: [
          '--headless',
          '--no-sandbox',
          '--disable-gpu',
          '--disable-dev-shm-usage',
        ],
      });

      try {
        const result = await runLighthouseWithTimeout(lighthouse, url, chrome.port, LIGHTHOUSE_ATTEMPT_TIMEOUT_MS);
        score = result.lhr.categories.performance.score;
        break;
      } catch (error) {
        lastError = error;
        const retriable = isRetriableLighthouseError(error);
        if (attempt === LIGHTHOUSE_MAX_ATTEMPTS || !retriable) {
          throw error;
        }
        await sleep(attempt * 1000);
      } finally {
        await killChromeIgnoringWindowsTempEperm(chrome);
      }
    }

    expect(lastError, 'Lighthouse run should succeed before score assertion').toBeUndefined();
    expect(score, `Lighthouse performance score for ${url}`).toBeGreaterThanOrEqual(MIN_PERFORMANCE_SCORE);
  });
});

function resolveChromePath() {
  if (process.env.KRAFT_E2E_CHROME_PATH) {
    return process.env.KRAFT_E2E_CHROME_PATH;
  }

  const playwrightChromium = chromium.executablePath();
  if (fs.existsSync(playwrightChromium)) {
    return playwrightChromium;
  }

  return undefined;
}

function runLighthouseWithTimeout(lighthouse, url, port, timeoutMs) {
  const runPromise = lighthouse(url, {
    port,
    output: 'json',
    logLevel: 'error',
    onlyCategories: ['performance'],
  }, {
    extends: 'lighthouse:default',
    settings: {
      formFactor: 'desktop',
      screenEmulation: { disabled: true },
      throttlingMethod: 'provided',
    },
  });

  return withTimeout(runPromise, timeoutMs, `Lighthouse attempt timed out after ${timeoutMs}ms`);
}

function withTimeout(promise, timeoutMs, message) {
  let timer;
  const timeoutPromise = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Error(message)), timeoutMs);
  });
  return Promise.race([promise, timeoutPromise]).finally(() => clearTimeout(timer));
}

function isRetriableLighthouseError(error) {
  const message = flattenErrorMessages(error);
  return message.includes('ECONNRESET')
    || message.includes('Failed to fetch browser webSocket URL')
    || message.includes('fetch failed')
    || message.includes('timed out');
}

function flattenErrorMessages(error) {
  const parts = [];
  let cursor = error;
  while (cursor && typeof cursor === 'object') {
    if (cursor.message) {
      parts.push(String(cursor.message));
    }
    cursor = cursor.cause;
  }
  return parts.join(' | ');
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function killChromeIgnoringWindowsTempEperm(chrome) {
  try {
    await chrome.kill();
  } catch (error) {
    if (isWindowsTempCleanupEperm(error)) {
      return;
    }
    throw error;
  }
}

function isWindowsTempCleanupEperm(error) {
  if (!error || typeof error !== 'object') {
    return false;
  }
  const message = error.message || '';
  return message.includes('EPERM') && message.includes('lighthouse.');
}

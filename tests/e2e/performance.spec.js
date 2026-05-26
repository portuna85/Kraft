const { test, expect, chromium } = require('@playwright/test');
const fs = require('node:fs');

const baseURL = process.env.KRAFT_E2E_BASE_URL || 'http://localhost:18080';
const MIN_PERFORMANCE_SCORE = 0.8;

test.describe('performance smoke', () => {
  test('home page Lighthouse performance score is at least 0.8', async ({}, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Lighthouse performance gate runs once on desktop Chromium');

    const [{ default: lighthouse }, chromeLauncher] = await Promise.all([
      import('lighthouse'),
      import('chrome-launcher'),
    ]);
    const chromePath = resolveChromePath();

    expect(chromePath, 'Chrome executable is required for the Lighthouse performance gate').toBeTruthy();
    const chrome = await chromeLauncher.launch({
      chromePath,
      chromeFlags: [
        '--headless',
        '--no-sandbox',
        '--disable-gpu',
      ],
    });

    try {
      const url = new URL('/', baseURL).toString();
      const result = await lighthouse(url, {
        port: chrome.port,
        output: 'json',
        logLevel: 'error',
        onlyCategories: ['performance'],
      }, {
        extends: 'lighthouse:default',
        settings: {
          formFactor: 'desktop',
          screenEmulation: { disabled: true },
        },
      });
      const score = result.lhr.categories.performance.score;

      expect(score, `Lighthouse performance score for ${url}`).toBeGreaterThanOrEqual(MIN_PERFORMANCE_SCORE);
    } finally {
      await chrome.kill();
    }
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

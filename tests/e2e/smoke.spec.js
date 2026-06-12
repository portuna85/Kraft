// @ts-check
const { test, expect } = require('@playwright/test');

const publicRoutes = [
  { path: '/', marker: '[data-testid="recommend-section"]' },
  { path: '/latest', marker: '[data-testid="latest-page"]' },
  { path: '/frequency', marker: '[data-testid="frequency-page"]' },
  { path: '/rounds', marker: '[data-testid="rounds-page"]' },
  { path: '/stats', marker: '[data-testid="stats-page"]' },
  { path: '/analysis', marker: '[data-testid="analysis-page"]' },
  { path: '/companion', marker: '[data-testid="companion-page"]' },
  { path: '/saved', marker: '[data-testid="saved-page"]' },
  { path: '/info/faq', marker: 'main h1' },
];

test.describe('public pages', () => {
  test.beforeEach(({ }, testInfo) => {
    test.skip(testInfo.project.name.includes('mobile'), 'covered by mobile-specific smoke checks');
  });

  for (const route of publicRoutes) {
    test(`renders ${route.path}`, async ({ page }) => {
      const response = await page.goto(route.path);

      expect(response?.ok(), `${route.path} should return a successful response`).toBe(true);
      await expect(page.locator(route.marker).first()).toBeVisible();
      await expect(page.locator('main')).toBeVisible();
      await expect(page.locator('footer')).toBeVisible();
    });
  }

  test('sets core metadata on public pages', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle(/KRAFT Lotto/i);
    await expect(page.locator('meta[name="description"]')).toHaveAttribute('content', /.+/);
    await expect(page.locator('html')).toHaveAttribute('lang', 'ko');
  });

  test('does not emit CSP console errors on the home page', async ({ page }) => {
    const cspErrors = [];
    page.on('console', (message) => {
      if (message.type() === 'error' && message.text().includes('Content Security Policy')) {
        cspErrors.push(message.text());
      }
    });

    await page.goto('/');
    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();

    expect(cspErrors, `CSP violations:\n${cspErrors.join('\n')}`).toHaveLength(0);
  });

  test('does not use prohibited prediction copy', async ({ page }) => {
    await page.goto('/');

    const bodyText = await page.locator('body').innerText();
    expect(bodyText).not.toContain('최고의 번호');
    expect(bodyText).not.toContain('당첨 보장');
    expect(bodyText).not.toContain('확률 높은 번호');
  });
});

test.describe('recommendations', () => {
  test.beforeEach(({ }, testInfo) => {
    test.skip(testInfo.project.name.includes('mobile'), 'covered on desktop to avoid duplicate API load');
  });

  test('loads latest draw and requests recommendations', async ({ page }) => {
    const latestResponse = page.waitForResponse((response) => (
      response.url().includes('/api/v1/rounds/latest') && response.ok()
    ));
    await page.goto('/');

    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();
    await latestResponse;
    await expect(page.locator('[data-testid="latest-draw"]')).toBeVisible();

    const section = page.locator('[data-testid="recommend-section"]');
    await expect(section.locator('#count-range')).toHaveValue('5');

    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/api/v1/numbers/recommend') && response.ok()),
      section.locator('button').click(),
    ]);

    await expect(section.locator('li')).toHaveCount(5);
  });
});

test.describe('round lookup', () => {
  test.beforeEach(({ }, testInfo) => {
    test.skip(testInfo.project.name.includes('mobile'), 'covered on desktop to avoid duplicate API load');
  });

  test('navigates from round search to detail mode', async ({ page }) => {
    await page.goto('/rounds');

    await expect(page.locator('[data-testid="rounds-page"] input[type="number"]')).toBeVisible();
    await page.locator('[data-testid="rounds-page"] input[type="number"]').fill('1');
    await page.locator('[data-testid="rounds-page"] .btn-primary').click();

    await expect(page).toHaveURL(/\/rounds\/?\?id=1$/);
    await expect(page.locator('main')).toContainText('1');
  });
});

test.describe('analysis tools', () => {
  test.beforeEach(({ }, testInfo) => {
    test.skip(testInfo.project.name.includes('mobile'), 'covered on desktop to avoid duplicate API load');
  });

  test('selects six balls and posts the analysis request', async ({ page }) => {
    await page.goto('/analysis');

    const pageRoot = page.locator('[data-testid="analysis-page"]');
    await expect(pageRoot).toBeVisible();

    for (const ball of [1, 2, 3, 4, 5, 6]) {
      await pageRoot.getByRole('button', { name: String(ball), exact: true }).click();
    }

    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/api/v1/stats/analysis') && response.ok()),
      pageRoot.locator('button').last().click(),
    ]);

    await expect(pageRoot.locator('section.card')).toBeVisible();
  });

  test('selects a base ball and loads companion data', async ({ page }) => {
    await page.goto('/companion');

    const pageRoot = page.locator('[data-testid="companion-page"]');
    await expect(pageRoot).toBeVisible();

    await Promise.all([
      page.waitForResponse((response) => response.url().includes('/api/v1/stats/companion?target=7') && response.ok()),
      pageRoot.getByRole('button', { name: '7', exact: true }).click(),
    ]);

    await expect(pageRoot.locator('section.card')).toBeVisible();
  });
});

test.describe('navigation mobile', () => {
  test.beforeEach(({ }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'), 'mobile-only smoke checks');
  });

  test('shows bottom navigation mobile', async ({ page }) => {
    await page.goto('/');

    const nav = page.locator('[data-testid="bottom-nav"]');
    await expect(nav).toBeVisible();
    await expect(nav.locator('a')).toHaveCount(8);
  });

  test('renders admin login form mobile', async ({ page }) => {
    await page.goto('/admin/login');

    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });
});

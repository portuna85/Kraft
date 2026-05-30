const { test, expect } = require('@playwright/test');
const AxeBuilder = require('@axe-core/playwright').default;

test.describe('home smoke', () => {
  test('renders home page', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/kraft-lotto|최고의 번호/i);
  });

  test('shows latest card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#latest')).toBeVisible();
  });

  test('renders recommend card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#recommend .kraft-combo').first()).toBeVisible();
  });

  test('renders frequency card', async ({ page }) => {
    await page.goto('/frequency');
    await expect(page.locator('#frequency .card-title')).toBeVisible();
  });

  test('renders rounds card', async ({ page }) => {
    await page.goto('/rounds');
    await expect(page.locator('#rounds .card-title')).toBeVisible();
  });

  test('/recommend redirects to home', async ({ page }) => {
    await page.goto('/recommend');
    await expect(page).toHaveURL('/');
  });

  test('has no CSP violations in console', async ({ page }) => {
    const violations = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error' && msg.text().includes('Content Security Policy')) {
        violations.push(msg.text());
      }
    });

    await page.goto('/');
    await page.waitForTimeout(1000);

    expect(violations, `CSP violations: ${violations.join('\n')}`).toHaveLength(0);
  });

  test('does not inject htmx indicator inline style into head', async ({ page }) => {
    await page.goto('/');
    const headStyles = await page.locator('head style').allTextContents();
    const hasHtmxInlineIndicator = headStyles.some((text) => text.includes('htmx-indicator'));
    expect(hasHtmxInlineIndicator).toBeFalsy();
  });

  test('valid round query renders round search block', async ({ page }) => {
    await page.goto('/?round=1');
    await expect(page.locator('#round-search')).toBeVisible();
  });

  test('invalid out-of-range round query returns 400', async ({ page }) => {
    const response = await page.goto('/?round=9999');
    expect(response.status()).toBe(400);
  });

  test('home page renders correctly without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/');
    await expect(page.locator('#recommend .kraft-combo').first()).toBeVisible();
    await expect(page.locator('#latest')).toBeVisible();
  });
});

test.describe('responsive smoke', () => {
  test('shows bottom navigation on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/');
    await expect(page.locator('.kraft-bottom-nav')).toBeVisible();
  });

  test('renders ops filters on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/admin/ops');
    await expect(page.locator('form[action="/admin/ops"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Apply' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Reset' })).toBeVisible();
    await expect(page.locator('.ops-reason-quick')).toHaveCount(3);
  });
});

test.describe('accessibility smoke', () => {
  test('home has no critical accessibility violations', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#latest')).toBeVisible();
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });

  test('ops page has no critical accessibility violations on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/admin/ops');
    await expect(page.locator('form[action="/admin/ops"]')).toBeVisible();
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });
});

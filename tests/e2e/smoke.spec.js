const { test, expect } = require('@playwright/test');
const AxeBuilder = require('@axe-core/playwright').default;

test.describe('home smoke', () => {
  test('renders home page', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/KRAFT Lotto/i);
  });

  test('shows latest draw section', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('[data-testid="latest-draw"]')).toBeVisible({ timeout: 10000 });
  });

  test('renders recommend section', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();
  });

  test('renders frequency page', async ({ page }) => {
    await page.goto('/frequency');
    await expect(page.locator('[data-testid="frequency-page"]')).toBeVisible();
  });

  test('renders rounds page', async ({ page }) => {
    await page.goto('/rounds');
    await expect(page.locator('[data-testid="rounds-page"]')).toBeVisible();
  });

  test('has no CSP violations in console', async ({ page }) => {
    const violations = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error' && msg.text().includes('Content Security Policy')) {
        violations.push(msg.text());
      }
    });

    await page.goto('/');
    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();

    expect(violations, `CSP violations: ${violations.join('\n')}`).toHaveLength(0);
  });

  test('does not inject htmx indicator inline style into head', async ({ page }) => {
    await page.goto('/');
    const headStyles = await page.locator('head style').allTextContents();
    const hasHtmxInlineIndicator = headStyles.some((text) => text.includes('htmx-indicator'));
    expect(hasHtmxInlineIndicator).toBeFalsy();
  });
});


test.describe('responsive smoke', () => {
  test('shows bottom navigation on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/');
    await expect(page.locator('[data-testid="bottom-nav"]')).toBeVisible();
  });

  test('admin login page renders on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/admin/login');
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
    await expect(page.locator('input[name="username"]')).toHaveValue('admin');
  });
});

test.describe('brand copy regression', () => {
  test('does not show overclaiming brand copy', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).not.toContainText('최고의 번호');
    await expect(page.locator('body')).not.toContainText('당첨 보장');
    await expect(page.locator('body')).not.toContainText('확률 높은 번호');
  });
});

test.describe('accessibility smoke', () => {
  test('home has no critical accessibility violations', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });

  test('admin login page has no critical accessibility violations on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/admin/login');
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });
});

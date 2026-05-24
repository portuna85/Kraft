const { test, expect } = require('@playwright/test');

test.describe('home smoke', () => {
  test('renders home page', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/kraft-lotto|최고의 번호/i);
  });

  test('shows latest card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#latest')).toBeVisible();
  });

  test('lazy-loads recommend card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#recommend .kraft-combo').first()).toBeVisible({ timeout: 15000 });
  });

  test('lazy-loads frequency card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#frequency .card-title')).toBeVisible({ timeout: 15000 });
  });

  test('lazy-loads rounds card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#rounds .card-title')).toBeVisible({ timeout: 15000 });
  });

  test('has no CSP violations in console', async ({ page }) => {
    const violations = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error' && msg.text().includes('Content Security Policy')) {
        violations.push(msg.text());
      }
    });

    await page.goto('/');
    await page.waitForTimeout(3000);

    expect(violations, `CSP violations: ${violations.join('\n')}`).toHaveLength(0);
  });

  test('valid round query renders round search block', async ({ page }) => {
    await page.goto('/?round=1');
    await expect(page.locator('#round-search')).toBeVisible();
  });

  test('invalid out-of-range round query returns 400', async ({ page }) => {
    const response = await page.goto('/?round=9999');
    expect(response.status()).toBe(400);
  });

  test('loads fragments via fetch fallback when htmx script is blocked', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/');
    await expect(page.locator('#recommend .kraft-combo').first()).toBeVisible({ timeout: 15000 });
    await expect(page.locator('#frequency .card-title')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('#rounds .card-title')).toBeVisible({ timeout: 15000 });
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

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
    await expect(page.locator('#round-search .kraft-balls')).toBeVisible();
  });

  test('invalid out-of-range round query returns 400', async ({ page }) => {
    const response = await page.goto('/?round=9999');
    expect(response.status()).toBe(400);
  });
});

test.describe('mobile nav', () => {
  test('shows bottom navigation on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');
    await expect(page.locator('.kraft-bottom-nav')).toBeVisible();
  });
});

const { test, expect } = require('@playwright/test');

const ADMIN_PASS = 'kraft-e2e-admin';

async function adminLogin(page) {
  await page.goto('/admin/login');
  await page.fill('input[name="password"]', ADMIN_PASS);
  await page.click('button[type="submit"]');
  await page.waitForURL((url) => !url.toString().includes('/admin/login'));
}

test.describe('admin: login', () => {
  test('successful login redirects away from login page', async ({ page }) => {
    await adminLogin(page);
    await expect(page).not.toHaveURL(/\/admin\/login/);
  });

  test('wrong password stays on login page with error', async ({ page }) => {
    await page.goto('/admin/login');
    await page.fill('input[name="password"]', 'wrong-password');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/admin\/login/);
  });
});

test.describe('admin: no CSP violations', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  for (const path of ['/admin/ops', '/admin/ops/collection', '/admin/ops/news', '/admin/ops/cache']) {
    test(`${path}`, async ({ page }) => {
      const violations = [];
      page.on('console', (m) => {
        if (m.type() === 'error' && m.text().includes('Content Security Policy')) {
          violations.push(m.text());
        }
      });
      await page.goto(path);
      await expect(page.locator('body')).toBeVisible();
      expect(violations, `CSP violations:\n${violations.join('\n')}`).toHaveLength(0);
    });
  }
});

test.describe('admin: confirmation dialogs', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('cache full reset — cancel prevents submission', async ({ page }) => {
    await page.goto('/admin/ops/cache');
    let requested = false;
    page.on('request', (req) => {
      if (req.url().includes('/cache/evict-all')) requested = true;
    });
    page.on('dialog', (d) => d.dismiss());
    await page.locator('form[data-confirm] button[type="submit"]').click();
    await page.waitForTimeout(300);
    expect(requested).toBe(false);
  });

  test('cache full reset — confirm submits', async ({ page }) => {
    await page.goto('/admin/ops/cache');
    page.on('dialog', (d) => d.accept());
    const responsePromise = page.waitForResponse(
      (r) => r.url().includes('/cache/evict-all') && r.request().method() === 'POST',
      { timeout: 5000 },
    );
    await page.locator('form[data-confirm] button[type="submit"]').click();
    const response = await responsePromise;
    expect(response.status()).toBeLessThan(400);
  });

  test('collection latest — cancel prevents submission', async ({ page }) => {
    await page.goto('/admin/ops/collection');
    let requested = false;
    page.on('request', (req) => {
      if (req.url().includes('/collection/latest')) requested = true;
    });
    page.on('dialog', (d) => d.dismiss());
    await page.locator('button[data-confirm]').click();
    await page.waitForTimeout(300);
    expect(requested).toBe(false);
  });

  test('news keyword block — cancel prevents submission', async ({ page }) => {
    await page.goto('/admin/ops/news');
    let requested = false;
    page.on('request', (req) => {
      if (req.url().includes('/block-keyword')) requested = true;
    });
    await page.fill('input[name="keyword"]', 'e2e-test-keyword');
    page.on('dialog', (d) => d.dismiss());
    await page.locator('form[data-confirm] button[type="submit"].btn-warning').click();
    await page.waitForTimeout(300);
    expect(requested).toBe(false);
  });
});

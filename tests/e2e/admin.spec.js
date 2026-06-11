// @ts-check
const { test, expect } = require('@playwright/test');

const ADMIN_PASS = 'kraft-e2e-admin';

async function adminLogin(page) {
  await page.goto('/admin/login');
  await page.fill('input[name="password"]', ADMIN_PASS);
  await page.click('button[type="submit"]');
  await page.waitForURL((url) => !url.pathname.startsWith('/admin/login'), { timeout: 10000 });
}

// ─── 로그인 ────────────────────────────────────────────────────────
test.describe('관리자 로그인', () => {
  test('올바른 비밀번호 → 로그인 페이지에서 이탈', async ({ page }) => {
    await adminLogin(page);
    await expect(page).not.toHaveURL(/\/admin\/login/);
  });

  test('잘못된 비밀번호 → 로그인 페이지 유지', async ({ page }) => {
    await page.goto('/admin/login');
    await page.fill('input[name="password"]', 'wrong-password');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/admin\/login/);
  });
});

// ─── 관리자 페이지 렌더링 ───────────────────────────────────────────
test.describe('관리자 페이지', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('운영 대시보드 렌더링', async ({ page }) => {
    await page.goto('/admin/ops');
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('main')).not.toBeEmpty();
  });

  test('캐시 관리 페이지 렌더링', async ({ page }) => {
    await page.goto('/admin/ops/cache');
    await expect(page.locator('h1')).toContainText('캐시 관리');
  });

  test('수집 관리 페이지 렌더링', async ({ page }) => {
    await page.goto('/admin/ops/collection');
    await expect(page.locator('h1')).toContainText('수집 관리');
  });
});

// ─── CSP 위반 없음 ─────────────────────────────────────────────────
test.describe('관리자 CSP', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  for (const path of ['/admin/ops', '/admin/ops/collection', '/admin/ops/cache']) {
    test(`CSP 위반 없음: ${path}`, async ({ page }) => {
      const violations = [];
      page.on('console', (m) => {
        if (m.type() === 'error' && m.text().includes('Content Security Policy')) {
          violations.push(m.text());
        }
      });
      await page.goto(path);
      await expect(page.locator('body')).toBeVisible();
      expect(violations, `CSP 위반:\n${violations.join('\n')}`).toHaveLength(0);
    });
  }
});

// ─── 확인 다이얼로그 ────────────────────────────────────────────────
test.describe('확인 다이얼로그', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('캐시 전체 초기화 — 취소 시 요청 없음', async ({ page }) => {
    await page.goto('/admin/ops/cache');
    // admin-confirm.js 가 DOMContentLoaded 후 이벤트 등록
    await page.waitForLoadState('domcontentloaded');

    let evictRequested = false;
    page.on('request', (req) => {
      if (req.url().includes('/cache/evict-all')) evictRequested = true;
    });
    page.on('dialog', (d) => d.dismiss());

    await page.locator('form[data-confirm] button[type="submit"]').click();
    // dialog dismiss 처리 후 충분한 대기
    await page.waitForTimeout(500);

    expect(evictRequested).toBe(false);
  });

  test('캐시 전체 초기화 — 확인 시 POST 요청 발생', async ({ page }) => {
    await page.goto('/admin/ops/cache');
    await page.waitForLoadState('domcontentloaded');

    page.on('dialog', (d) => d.accept());
    const responsePromise = page.waitForResponse(
      (r) => r.url().includes('/cache/evict-all') && r.request().method() === 'POST',
      { timeout: 8000 },
    );
    await page.locator('form[data-confirm] button[type="submit"]').click();
    const response = await responsePromise;
    expect(response.status()).toBeLessThan(400);
  });

  test('수집 최신 — 취소 시 요청 없음', async ({ page }) => {
    await page.goto('/admin/ops/collection');
    await page.waitForLoadState('domcontentloaded');

    let collectRequested = false;
    page.on('request', (req) => {
      if (req.url().includes('/collection/latest')) collectRequested = true;
    });
    page.on('dialog', (d) => d.dismiss());

    await page.locator('button[data-confirm]').click();
    await page.waitForTimeout(500);

    expect(collectRequested).toBe(false);
  });
});

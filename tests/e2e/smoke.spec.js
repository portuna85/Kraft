const { test, expect } = require('@playwright/test');
const AxeBuilder = require('@axe-core/playwright').default;

test.describe('home smoke', () => {
  test('renders home page', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/KRAFT Lotto/i);
  });

  test('shows latest card', async ({ page }) => {
    await page.goto('/latest');
    await expect(page.locator('.latest-draw-meta')).toBeVisible();
  });

  test('renders recommend card', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#recommend .set-card').first()).toBeVisible();
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
    await expect(page.locator('#recommend')).toBeVisible();

    expect(violations, `CSP violations: ${violations.join('\n')}`).toHaveLength(0);
  });

  test('does not inject htmx indicator inline style into head', async ({ page }) => {
    await page.goto('/');
    const headStyles = await page.locator('head style').allTextContents();
    const hasHtmxInlineIndicator = headStyles.some((text) => text.includes('htmx-indicator'));
    expect(hasHtmxInlineIndicator).toBeFalsy();
  });

  test('valid round query renders round search block', async ({ page }) => {
    await page.goto('/rounds?round=1');
    await expect(page.locator('#round-search')).toBeVisible();
  });

  test('invalid out-of-range round query returns 400', async ({ page }) => {
    const response = await page.goto('/?round=10000');
    expect(response.status()).toBe(400);
  });

  test('home page renders correctly without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/');
    await expect(page.locator('#recommend .set-card').first()).toBeVisible();
  });

  test('recommend filters update without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/');

    await page.locator('#filterOddBtns .filter-btn[data-value="3"]').click();

    await expect(page.locator('#filterOddBtns .filter-btn.active')).toHaveAttribute('data-value', '3');
    await expect(page.locator('#recommend .set-card').first()).toBeVisible();
  });

  test('frequency period tabs update without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/frequency');

    await page.locator('.btn-period').filter({ hasText: '최근 50회' }).click();

    await expect(page.locator('.btn-period.active')).toContainText('최근 50회');
    await expect(page.locator('#freq-list .freq-item')).toHaveCount(45);
  });

  test('round page size updates without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/rounds');

    await page.locator('.page-size-btn').filter({ hasText: '50' }).click();

    await expect(page.locator('.page-size-btn.active')).toHaveText('50');
    await expect(page.locator('#rounds .round-item').first()).toBeVisible();
  });

  test('analysis picker fetches fragment without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/analysis');

    for (const number of ['1', '7', '15', '23', '38', '45']) {
      await page.locator(`#analysis-picker .ball-picker-item[data-number="${number}"]`).click();
    }

    await expect(page.locator('#analysis-result')).not.toBeEmpty();
  });

  test('companion picker fetches fragment without htmx', async ({ page }) => {
    await page.route('**/vendor/htmx/htmx.min.js', (route) => route.abort());
    await page.goto('/companion');
    await page.locator('#companion-picker .ball-picker-item[data-number="7"]').click();

    await expect(page.locator('#companion-result')).not.toBeEmpty();
  });
});


test.describe('responsive smoke', () => {
  test('shows bottom navigation on mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'));
    await page.goto('/');
    await expect(page.locator('.kraft-bottom-nav')).toBeVisible();
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
    await expect(page.locator('#recommend')).toBeVisible();
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

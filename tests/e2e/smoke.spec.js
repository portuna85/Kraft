// @ts-check
const { test, expect } = require('@playwright/test');

// ─── 공개 페이지 렌더링 ────────────────────────────────────────────
test.describe('페이지 렌더링', () => {
  test('홈 — 타이틀 및 번호 추천 섹션', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/KRAFT Lotto/i);
    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();
  });

  test('출현 빈도 페이지', async ({ page }) => {
    await page.goto('/frequency');
    await expect(page.locator('[data-testid="frequency-page"]')).toBeVisible();
  });

  test('회차 검색 페이지', async ({ page }) => {
    await page.goto('/rounds');
    await expect(page.locator('[data-testid="rounds-page"]')).toBeVisible();
  });

  test('패턴 통계 페이지', async ({ page }) => {
    await page.goto('/stats');
    await expect(page.locator('[data-testid="stats-page"]')).toBeVisible();
    // API 응답 후 h1 표시 대기 (빈 DB라도 h1은 렌더됨)
    await expect(page.locator('[data-testid="stats-page"] h1')).toContainText('패턴 통계', { timeout: 15000 });
  });

  test('번호 분석 페이지', async ({ page }) => {
    await page.goto('/analysis');
    await expect(page.locator('[data-testid="analysis-page"]')).toBeVisible();
    await expect(page.locator('h1')).toContainText('번호 조합 분석');
  });

  test('동반 출현 페이지', async ({ page }) => {
    await page.goto('/companion');
    await expect(page.locator('[data-testid="companion-page"]')).toBeVisible();
    await expect(page.locator('h1')).toContainText('동반 출현 기록');
  });

  test('최신 회차 페이지 — 빈 DB에서 안정 처리', async ({ page }) => {
    await page.goto('/latest');
    await expect(page.locator('[data-testid="latest-page"]')).toBeVisible();
    // 로딩 완료 후 빈 DB이므로 "데이터가 없습니다" 표시
    await expect(page.locator('[data-testid="latest-page"] p')).toBeVisible({ timeout: 15000 });
  });

  test('FAQ 정보 페이지', async ({ page }) => {
    await page.goto('/info/faq');
    await expect(page.locator('h1')).toContainText('FAQ');
  });
});

// ─── 홈 번호 추천 기능 ─────────────────────────────────────────────
test.describe('번호 추천', () => {
  test('번호 생성 버튼 클릭 → 로딩 후 완료', async ({ page }) => {
    await page.goto('/');
    const btn = page.locator('button:has-text("번호 생성")');
    await expect(btn).toBeEnabled();
    await btn.click();
    // 로딩 중 버튼 텍스트가 "생성 중…" 으로 변경됐다가 완료 후 복원
    await expect(btn).toBeEnabled({ timeout: 10000 });
  });

  test('범위 슬라이더로 조합 수 조절 가능', async ({ page }) => {
    await page.goto('/');
    const slider = page.locator('#count-range');
    await expect(slider).toBeVisible();
    await expect(slider).toBeEnabled();
  });
});

// ─── 회차 검색 기능 ────────────────────────────────────────────────
test.describe('회차 검색', () => {
  test('검색 입력 및 이동 버튼 표시', async ({ page }) => {
    await page.goto('/rounds');
    await expect(page.locator('[data-testid="rounds-page"] input[type="number"]')).toBeVisible();
    await expect(page.locator('[data-testid="rounds-page"] button:has-text("이동")')).toBeVisible();
  });
});

// ─── 네비게이션 ────────────────────────────────────────────────────
test.describe('네비게이션', () => {
  test('데스크탑 헤더 nav 링크 표시', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name === 'mobile-chrome', '데스크탑 전용 테스트');
    await page.goto('/');
    // desktop nav는 data-testid 없음, bottom-nav는 data-testid="bottom-nav" 로 구분
    // 두 nav 모두 같은 링크를 가지므로 nav:not([data-testid])로 데스크탑 nav만 타겟
    await expect(page.locator('nav:not([data-testid]) a:has-text("출현 빈도")')).toBeVisible();
    await expect(page.locator('nav:not([data-testid]) a:has-text("회차 검색")')).toBeVisible();
  });

  test('모바일 하단 탭 표시 mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'), '모바일 전용 테스트');
    await page.goto('/');
    await expect(page.locator('[data-testid="bottom-nav"]')).toBeVisible();
  });

  test('관리자 로그인 폼 렌더링 mobile', async ({ page }, testInfo) => {
    test.skip(!testInfo.project.name.includes('mobile'), '모바일 전용 테스트');
    await page.goto('/admin/login');
    await expect(page.locator('input[name="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
  });
});

// ─── 브랜드 무결성 ─────────────────────────────────────────────────
test.describe('브랜드 무결성', () => {
  test('과장 광고 문구 없음', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).not.toContainText('최고의 번호');
    await expect(page.locator('body')).not.toContainText('당첨 보장');
    await expect(page.locator('body')).not.toContainText('확률 높은 번호');
  });

  test('홈 CSP 콘솔 위반 없음', async ({ page }) => {
    const violations = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error' && msg.text().includes('Content Security Policy')) {
        violations.push(msg.text());
      }
    });
    await page.goto('/');
    await expect(page.locator('[data-testid="recommend-section"]')).toBeVisible();
    expect(violations, `CSP 위반:\n${violations.join('\n')}`).toHaveLength(0);
  });

  test('head에 htmx-indicator 인라인 스타일 없음', async ({ page }) => {
    await page.goto('/');
    const headStyles = await page.locator('head style').allTextContents();
    expect(headStyles.some((t) => t.includes('htmx-indicator'))).toBe(false);
  });
});

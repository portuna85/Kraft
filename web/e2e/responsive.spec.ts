import { test, expect, type Page } from "@playwright/test";

// .site-header의 backdrop-filter가 자식 fixed 요소(.nav-backdrop/.nav-mobile-wrap)의
// containing block이 되어 bottom:0/inset:0이 뷰포트가 아니라 헤더 자신의 높이 기준으로
// 계산되던 버그가 있었다 — top==bottom이 되어 드로어가 찌그러졌다(npm run dev 육안 확인으로
// 발견). #nav-mobile의 "보임" 여부만으로는 이 레이아웃 붕괴를 잡지 못하므로, 배경·패널의
// 실제 bounding box가 뷰포트 전체를 덮는지 직접 검사한다.
async function expectDrawerCoversViewport(page: Page) {
  const viewport = page.viewportSize();
  if (!viewport) throw new Error("viewport size unavailable");

  const backdropBox = await page.locator(".nav-backdrop").boundingBox();
  const wrapBox = await page.locator(".nav-mobile-wrap").boundingBox();
  expect(backdropBox).not.toBeNull();
  expect(wrapBox).not.toBeNull();

  // 서브픽셀 반올림 오차 허용치
  const TOLERANCE = 2;
  expect(Math.abs(backdropBox!.y)).toBeLessThan(TOLERANCE);
  expect(Math.abs(backdropBox!.y + backdropBox!.height - viewport.height)).toBeLessThan(TOLERANCE);

  // 드로어 패널은 헤더 아래부터 시작해 뷰포트 하단까지 닿아야 한다
  expect(wrapBox!.y).toBeGreaterThan(0);
  expect(Math.abs(wrapBox!.y + wrapBox!.height - viewport.height)).toBeLessThan(TOLERANCE);
}

// ─────────────────────────────────────────────────────────────────────────────
// 가로 스크롤 없음 — 3개 대표 너비에서 확인
// ─────────────────────────────────────────────────────────────────────────────
const VIEWPORTS = [
  { width: 320, height: 568, label: "320px" },
  { width: 768, height: 1024, label: "768px" },
  { width: 1280, height: 800, label: "1280px" },
] as const;

for (const vp of VIEWPORTS) {
  test(`${vp.label} 뷰포트에서 가로 스크롤 없음`, async ({ page }) => {
    await page.setViewportSize({ width: vp.width, height: vp.height });
    await page.goto("/");
    // html/body의 overflow-x: clip은 루트 scrollWidth를 clientWidth로 수렴시켜
    // 콘텐츠가 잘려도 통과해버린다(docs/improvement.md §6-1) — 요소 단위로 실제
    // 뷰포트를 벗어나는 엘리먼트가 있는지 검사한다.
    const overflowing = await page.evaluate(() =>
      [...document.querySelectorAll("body *")]
        .filter((el) => {
          const r = el.getBoundingClientRect();
          if (r.width === 0 && r.height === 0) return false;
          return r.right > window.innerWidth + 1 || r.left < -1;
        })
        .filter((el) => !el.closest("[data-allow-overflow]"))
        .map((el) => el.className || el.tagName),
    );
    expect(overflowing).toEqual([]);
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// 모바일 네비게이션 드로어
// ─────────────────────────────────────────────────────────────────────────────
test.describe("모바일 내비게이션 드로어", () => {
  // Pixel 5 너비 = 393px — CSS 기준 640px 미만 → 햄버거 메뉴
  test.use({ viewport: { width: 393, height: 851 } });

  test("햄버거 버튼이 보이고 데스크톱 내비게이션은 숨겨진다", async ({ page }) => {
    await page.goto("/");

    const toggle = page.getByRole("button", { name: "메뉴 열기" });
    await expect(toggle).toBeVisible();
    await expect(page.locator(".nav-desktop")).toBeHidden();
  });

  test("햄버거 클릭 → 드로어 열림", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();
    await expect(page.getByRole("button", { name: "메뉴 닫기" })).toBeVisible();
  });

  test("드로어 배경·패널이 뷰포트 전체를 덮는다 (backdrop-filter containing block 회귀 방지)", async ({
    page,
  }) => {
    await page.goto("/");
    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();

    await expectDrawerCoversViewport(page);
  });

  test("탈출 키로 드로어 닫힘", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();

    await page.keyboard.press("Escape");
    await expect(page.locator("#nav-mobile")).not.toBeVisible();
    await expect(page.getByRole("button", { name: "메뉴 열기" })).toBeVisible();
  });

  test("뒷배경 클릭으로 드로어 닫힘", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();

    await page.locator(".nav-mobile-wrap").evaluate((element) => {
      (element as HTMLElement).click();
    });
    await expect(page.locator("#nav-mobile")).not.toBeVisible();
  });

  test("데스크톱 너비로 리사이즈 시 드로어 자동 닫힘", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();

    // matchMedia change 이벤트를 트리거하기 위해 1024px 이상으로 리사이즈
    await page.setViewportSize({ width: 1280, height: 800 });
    await expect(page.locator("#nav-mobile")).not.toBeVisible();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 태블릿 네비게이션 드로어
// ─────────────────────────────────────────────────────────────────────────────
test.describe("태블릿 내비게이션 드로어", () => {
  // 640px ≤ width < 1024px → 햄버거 메뉴 (전폭 드로어)
  test.use({ viewport: { width: 768, height: 1024 } });

  test("햄버거 버튼이 보이고 데스크톱 내비게이션은 숨겨진다", async ({ page }) => {
    await page.goto("/");

    await expect(page.getByRole("button", { name: "메뉴 열기" })).toBeVisible();
    await expect(page.locator(".nav-desktop")).toBeHidden();
  });

  test("햄버거 클릭 → 전폭 드로어 열림", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();
  });

  test("드로어 배경·패널이 뷰포트 전체를 덮는다 (backdrop-filter containing block 회귀 방지)", async ({
    page,
  }) => {
    await page.goto("/");
    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();

    await expectDrawerCoversViewport(page);
  });

  test("탈출 키로 드로어 닫힘", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();

    await page.keyboard.press("Escape");
    await expect(page.locator("#nav-mobile")).not.toBeVisible();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 데스크톱: 데스크톱 nav 보임, 햄버거 숨겨짐
// ─────────────────────────────────────────────────────────────────────────────
test.describe("데스크톱 내비게이션", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("데스크톱 내비게이션이 보이고 햄버거 버튼은 숨겨진다", async ({ page }) => {
    await page.goto("/");

    await expect(page.locator(".nav-desktop")).toBeVisible();
    await expect(page.getByRole("button", { name: "메뉴 열기" })).toBeHidden();
  });
});

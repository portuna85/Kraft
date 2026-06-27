import { test, expect } from "@playwright/test";

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
    const ok = await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    );
    expect(ok).toBe(true);
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// 모바일 네비게이션 드로어
// ─────────────────────────────────────────────────────────────────────────────
test.describe("모바일 nav 드로어", () => {
  // Pixel 5 너비 = 393px — CSS 기준 640px 미만 → 햄버거 메뉴
  test.use({ viewport: { width: 393, height: 851 } });

  test("햄버거 버튼이 보이고 데스크톱 nav는 숨겨진다", async ({ page }) => {
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

  test("Escape 키로 드로어 닫힘", async ({ page }) => {
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

    await page.locator(".nav-backdrop").click();
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
test.describe("태블릿 nav 드로어", () => {
  // 640px ≤ width < 1024px → 햄버거 메뉴 (전폭 드로어)
  test.use({ viewport: { width: 768, height: 1024 } });

  test("햄버거 버튼이 보이고 데스크톱 nav는 숨겨진다", async ({ page }) => {
    await page.goto("/");

    await expect(page.getByRole("button", { name: "메뉴 열기" })).toBeVisible();
    await expect(page.locator(".nav-desktop")).toBeHidden();
  });

  test("햄버거 클릭 → 전폭 드로어 열림", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "메뉴 열기" }).click();
    await expect(page.locator("#nav-mobile")).toBeVisible();
  });

  test("Escape 키로 드로어 닫힘", async ({ page }) => {
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
test.describe("데스크톱 nav", () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test("데스크톱 nav가 보이고 햄버거 버튼은 숨겨진다", async ({ page }) => {
    await page.goto("/");

    await expect(page.locator(".nav-desktop")).toBeVisible();
    await expect(page.getByRole("button", { name: "메뉴 열기" })).toBeHidden();
  });
});

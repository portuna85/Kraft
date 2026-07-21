import { test, expect } from "@playwright/test";

// 이 E2E 환경엔 백엔드가 없다(playwright.config.ts 참고). 서버 컴포넌트 페이지가
// 이미 구현된 폴백/에러 UI로 정상 렌더되는지만 확인하는 스모크 테스트다.

test("출현 통계 페이지는 백엔드 응답이 없으면 에러 경계를 보여준다", async ({ page }) => {
  await page.goto("/frequency");

  await expect(page.getByText("페이지를 불러오지 못했습니다")).toBeVisible();
  await page.getByRole("link", { name: "홈으로 이동" }).click();
  await expect(page).toHaveURL("/");
});

test("동반 출현 페이지는 백엔드 응답이 없으면 에러 경계를 보여준다", async ({ page }) => {
  await page.goto("/companion");

  await expect(page.getByText("페이지를 불러오지 못했습니다")).toBeVisible();
});

test("패턴 통계 페이지는 백엔드 응답이 없으면 폴백 문구를 보여준다", async ({ page }) => {
  await page.goto("/stats");

  await expect(page.getByRole("heading", { name: "통계를 준비 중입니다" })).toBeVisible();
});

test("회차 목록 페이지는 최신 회차·목록 조회가 모두 실패하면 에러 경계를 보여준다", async ({ page }) => {
  await page.goto("/rounds");

  await expect(page.getByText("페이지를 불러오지 못했습니다")).toBeVisible();
});

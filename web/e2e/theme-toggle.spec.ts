import { test, expect } from "@playwright/test";

test("테마 토글을 누르면 다크 모드로 전환되고 새로고침 후에도 유지된다", async ({ page }) => {
  await page.goto("/");

  const toggle = page.getByRole("button", { name: "다크 모드로 전환" });
  await expect(page.locator("html")).not.toHaveAttribute("data-theme", "dark");

  await toggle.click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await expect(page.getByRole("button", { name: "라이트 모드로 전환" })).toBeVisible();

  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
});

test("다크 모드에서 다시 누르면 라이트 모드로 돌아간다", async ({ page }) => {
  await page.goto("/");

  await page.getByRole("button", { name: "다크 모드로 전환" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");

  await page.getByRole("button", { name: "라이트 모드로 전환" }).click();
  await expect(page.locator("html")).not.toHaveAttribute("data-theme", "dark");
});

import { test, expect } from "@playwright/test";

test("추천 생성 후 저장하면 저장됨으로 표시된다", async ({ page }) => {
  await page.route("**/api/v1/numbers/recommend", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        recommendations: [
          [1, 2, 3, 4, 5, 6],
          [7, 8, 9, 10, 11, 12],
          [13, 14, 15, 16, 17, 18],
          [19, 20, 21, 22, 23, 24],
          [25, 26, 27, 28, 29, 30],
        ],
      }),
    });
  });
  await page.route("**/api/v1/saved", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 201,
        contentType: "application/json",
        body: JSON.stringify({ created: true }),
      });
      return;
    }
    await route.continue();
  });

  await page.goto("/recommend");

  const firstCard = page.locator(".recommend-card").first();
  await expect(firstCard).toBeVisible();

  await firstCard.getByRole("button", { name: "저장" }).click();
  await expect(firstCard.getByRole("button", { name: "저장됨" })).toBeVisible();
});

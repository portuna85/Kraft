import { test, expect } from "@playwright/test";

test("저장 번호 목록은 최신 회차와 대조 결과를 보여주고 삭제할 수 있다", async ({ page }) => {
  await page.route("**/api/v1/saved", async (route) => {
    if (route.request().method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id: 1,
            numbers: [1, 2, 3, 4, 5, 6],
            label: "테스트 번호",
            source: "MANUAL",
            createdAt: "2026-01-01T00:00:00Z",
          },
        ]),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/api/v1/rounds/latest", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        round: 1234,
        drawDate: "2026-06-20",
        numbers: [1, 2, 3, 7, 8, 9],
        bonusNumber: 10,
        firstPrizeAmount: 2_000_000_000,
        secondPrize: 50_000_000,
        secondWinners: 10,
        totalSales: 80_000_000_000,
        firstAccumAmount: 2_000_000_000,
      }),
    });
  });
  await page.route("**/api/v1/saved/1", async (route) => {
    await route.fulfill({ status: 204 });
  });

  await page.goto("/saved");

  await expect(page.getByText("테스트 번호")).toBeVisible();
  await expect(page.getByText("1234회 기준 3개 일치")).toBeVisible();

  await page.getByRole("button", { name: "삭제" }).click();
  await expect(page.getByText("테스트 번호")).toHaveCount(0);
});

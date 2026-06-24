import { test, expect } from "@playwright/test";

test("저장 번호 목록을 표시하고 삭제할 수 있다", async ({ page }) => {
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
  await page.route("**/api/v1/saved/1", async (route) => {
    await route.fulfill({ status: 204 });
  });

  await page.goto("/saved");

  // 볼 번호가 렌더링되는지 확인
  const list = page.locator(".saved-list");
  await expect(list).toBeVisible();
  await expect(list.locator(".saved-item")).toHaveCount(1);

  // 삭제 버튼 클릭 후 목록에서 제거되는지 확인
  await page.getByRole("button", { name: "삭제" }).click();
  await expect(list.locator(".saved-item")).toHaveCount(0);
  await expect(page.getByText("삭제했습니다.")).toBeVisible();
});

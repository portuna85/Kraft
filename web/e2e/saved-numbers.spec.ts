import { test, expect } from "@playwright/test";

const SAVED_ITEM = {
  id: 1,
  numbers: [1, 2, 3, 4, 5, 6],
  label: "테스트 번호",
  source: "MANUAL",
  createdAt: "2026-01-01T00:00:00Z",
};

function mockSavedApi(
  page: import("@playwright/test").Page,
  options: { deleteStatus?: number } = {},
) {
  const deleteStatus = options.deleteStatus ?? 204;

  page.route("**/api/v1/saved", (route) => {
    if (route.request().method() === "GET") {
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([SAVED_ITEM]),
      });
    } else {
      route.continue();
    }
  });

  page.route("**/api/v1/saved/matches**", (route) => {
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  const deleteCalls: string[] = [];
  page.route("**/api/v1/saved/1", (route) => {
    deleteCalls.push(route.request().method());
    route.fulfill({ status: deleteStatus });
  });

  return { deleteCalls };
}

test("저장 번호 목록을 표시한다", async ({ page }) => {
  mockSavedApi(page);
  await page.goto("/saved");

  const list = page.locator(".saved-list");
  await expect(list).toBeVisible();
  await expect(list.locator(".saved-item")).toHaveCount(1);
});

test("삭제 버튼 클릭 시 행이 즉시 제거되고 삭제 요청이 전송된다", async ({ page }) => {
  const { deleteCalls } = mockSavedApi(page);
  await page.goto("/saved");

  const list = page.locator(".saved-list");
  await expect(list.locator(".saved-item")).toHaveCount(1);

  await page.getByRole("button", { name: "삭제" }).click();
  await expect(list.locator(".saved-item")).toHaveCount(0);

  await page.waitForTimeout(300);
  expect(deleteCalls.filter((m) => m === "DELETE")).toHaveLength(1);
});

test("삭제 실패 시 삭제 요청이 전송됐고 행이 복구된다", async ({ page }) => {
  const { deleteCalls } = mockSavedApi(page, { deleteStatus: 500 });
  await page.goto("/saved");

  const list = page.locator(".saved-list");
  await page.getByRole("button", { name: "삭제" }).click();

  // mock이 즉각 응답하므로 삭제+복구가 거의 동시에 발생 — 최종 상태만 검증
  await page.waitForTimeout(300);
  expect(deleteCalls.filter((m) => m === "DELETE")).toHaveLength(1);
  await expect(list.locator(".saved-item")).toHaveCount(1);
});

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

test("삭제 후 실행 취소하면 DELETE가 전송되지 않고 행이 복구된다", async ({ page }) => {
  const { deleteCalls } = mockSavedApi(page);
  await page.goto("/saved");

  const list = page.locator(".saved-list");
  await expect(list.locator(".saved-item")).toHaveCount(1);

  // 삭제: 낙관적으로 즉시 제거
  await page.getByRole("button", { name: "삭제" }).click();
  await expect(list.locator(".saved-item")).toHaveCount(0);
  await expect(page.getByText("삭제했습니다.")).toBeVisible();

  // 실행 취소 → 행 복구, DELETE 미전송
  await page.getByRole("button", { name: "실행 취소" }).click();
  await expect(list.locator(".saved-item")).toHaveCount(1);
  expect(deleteCalls.filter((m) => m === "DELETE")).toHaveLength(0);
});

test("실행 취소 없이 undo 창이 닫히면 DELETE가 전송된다", async ({ page }) => {
  const { deleteCalls } = mockSavedApi(page);
  await page.goto("/saved");

  await page.getByRole("button", { name: "삭제" }).click();
  await expect(page.locator(".saved-list .saved-item")).toHaveCount(0);

  // NEXT_PUBLIC_UNDO_WINDOW_MS=200 으로 빌드된 테스트 환경에서 대기
  const undoWindowMs = Number(process.env.NEXT_PUBLIC_UNDO_WINDOW_MS ?? 5000);
  await page.waitForTimeout(undoWindowMs + 300);

  expect(deleteCalls.filter((m) => m === "DELETE")).toHaveLength(1);
});

test("DELETE 실패 시 행이 복구되고 에러 메시지가 표시된다", async ({ page }) => {
  mockSavedApi(page, { deleteStatus: 500 });
  await page.goto("/saved");

  const list = page.locator(".saved-list");
  await page.getByRole("button", { name: "삭제" }).click();
  await expect(list.locator(".saved-item")).toHaveCount(0);

  // undo 창이 닫히도록 대기
  const undoWindowMs = Number(process.env.NEXT_PUBLIC_UNDO_WINDOW_MS ?? 5000);
  await page.waitForTimeout(undoWindowMs + 300);

  // DELETE 실패 → 행 복구 + 에러 메시지
  await expect(list.locator(".saved-item")).toHaveCount(1);
  await expect(page.getByText("삭제에 실패했습니다.")).toBeVisible();
});

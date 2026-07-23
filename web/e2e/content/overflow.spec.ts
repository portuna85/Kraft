import { test, expect, type Page } from "@playwright/test";
import { expectNoOverflow } from "../lib/expect-no-overflow";

// §6-2: playwright.content.config.ts로만 실행된다(픽스처 백엔드가 떠 있는 상태).
// 기본 설정(playwright.config.ts)의 responsive.spec.ts는 백엔드가 없다는 전제라
// 이 라우트들에서 항상 error.tsx만 검사해왔다 — 여기서는 실제 렌더된 콘텐츠를
// 먼저 확인한 뒤 오버플로를 검사해, 픽스처가 깨져 조용히 에러 화면을 검사하게
// 되는 상황(§6-2가 원래 겪었던 문제)을 재발 방지한다.
const WIDTHS = [320, 768, 1024, 1280] as const;

// /companion, /frequency, /stats는 각각 route-level loading.tsx 스켈레톤을 두고 있는데,
// 실콘텐츠와 같은 클래스명(.companion-list, .frequency-grid, .pattern-list)을 그대로
// 재사용한다. networkidle까지 기다려도 스트리밍 전환 프레임에 스켈레톤 li와 실콘텐츠 li가
// 잠깐 함께 DOM에 남아 strict-mode 위반이 나던 걸 실제로 확인했다 — 스켈레톤 특유의
// shimmer 요소(.skeleton-line/.skeleton-ball)가 완전히 사라졌는지까지 명시적으로 기다린다.
async function gotoAndWaitForRealContent(page: Page, url: string) {
  await page.goto(url, { waitUntil: "networkidle" });
  await expect(page.locator(".skeleton-line, .skeleton-ball")).toHaveCount(0);
}

for (const width of WIDTHS) {
  test.describe(`${width}px`, () => {
    test.use({ viewport: { width, height: 900 } });

    test("/ — 홈 히어로 실렌더 후 오버플로 없음", async ({ page }) => {
      await gotoAndWaitForRealContent(page, "/");
      await expect(page.locator(".result-panel .balls")).toBeVisible();
      await expect(page.getByRole("heading", { name: "1189회" })).toBeVisible();
      await expectNoOverflow(page);
    });

    test("/frequency — 출현 통계 실렌더 후 오버플로 없음", async ({ page }) => {
      await gotoAndWaitForRealContent(page, "/frequency");
      await expect(page.locator(".freq-summary")).toBeVisible();
      await expect(page.locator(".frequency-grid .frequency-item").first()).toBeVisible();
      await expectNoOverflow(page);
    });

    test("/stats — 패턴 통계 실렌더 후 오버플로 없음", async ({ page }) => {
      await gotoAndWaitForRealContent(page, "/stats");
      await expect(page.locator(".pattern-list").first()).toBeVisible();
      await expectNoOverflow(page);
    });

    test("/companion — 동반 출현 실렌더 후 오버플로 없음", async ({ page }) => {
      await gotoAndWaitForRealContent(page, "/companion");
      await expect(page.locator(".companion-list")).toBeVisible();
      await expectNoOverflow(page);
    });

    // §6-3: /status는 기존 e2e(status.spec.ts)가 "백엔드 없음 → 폴백 문구" 상태만
    // 검증해왔다 — 여기서는 정상 데이터(freshness+incidents) 상태를 검증한다.
    test("/status — 정상 데이터 실렌더 후 오버플로 없음", async ({ page }) => {
      await gotoAndWaitForRealContent(page, "/status");
      await expect(page.locator(".status-incident-list")).toBeVisible();
      await expectNoOverflow(page);
    });
  });
}

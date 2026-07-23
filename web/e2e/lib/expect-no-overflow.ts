import { expect, type Page } from "@playwright/test";

// 과거엔 html/body에 overflow-x: clip이 걸려 있어 루트 scrollWidth/clientWidth 비교가
// 콘텐츠 잘림을 은폐했다(docs/improvement.md §6-1). clip은 제거됐지만(검사망이 충분히
// 넓어진 뒤 R-27과 함께 제거) 요소 단위 검사가 어차피 더 정확해 그대로 유지한다.
// responsive.spec.ts(백엔드 없음 전제)와 content/*.spec.ts(§6-2 픽스처 백엔드,
// 실콘텐츠 전제) 양쪽이 공유한다.
export async function expectNoOverflow(page: Page) {
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
}

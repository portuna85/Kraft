import { expect, type Page } from "@playwright/test";

// html/body의 overflow-x: clip은 루트 scrollWidth를 clientWidth로 수렴시켜 콘텐츠가
// 잘려도 통과해버린다(docs/improvement.md §6-1) — 요소 단위로 실제 뷰포트를 벗어나는
// 엘리먼트가 있는지 검사한다. responsive.spec.ts(백엔드 없음 전제)와
// content/*.spec.ts(§6-2 픽스처 백엔드, 실콘텐츠 전제) 양쪽이 공유한다.
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

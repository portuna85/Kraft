import { beforeEach, describe, expect, test, vi } from 'vitest';

// fallback 경로를 테스트하기 위해 htmx 미설치 상태로 IIFE 실행
delete window.htmx;

import '../../main/resources/static/js/fragment-loader.js';

let uid = 0;

/**
 * 고유 id로 target div를 생성한다.
 * 각 테스트마다 다른 id를 사용해 inFlightByTarget 키 충돌을 방지한다.
 */
function buildTarget(opts = {}) {
  const id = opts.id || ('section-' + (++uid));
  const path = opts.path || ('/fragments/' + id);
  document.body.innerHTML = `
    <div id="fragment-live-region" aria-live="polite"></div>
    <div id="${id}"
         hx-get="${path}"
         hx-trigger="load"
         hx-swap="outerHTML"
         aria-busy="false">
      <div data-state="loading" hidden>로딩 중…</div>
      <div data-state="success" hidden>콘텐츠</div>
      <div data-state="error" hidden>오류</div>
      ${opts.extra || ''}
    </div>
  `;
  return { id, path, el: document.getElementById(id) };
}

function click(path) {
  const source = document.querySelector(`[hx-get="${path}"]`);
  source.dispatchEvent(new MouseEvent('click', { bubbles: true }));
}

describe('loadFragmentFallback — fetch 성공', () => {
  test('outerHTML swap 후 id 로 갱신된 target 을 재탐색한다', async () => {
    const { id, path } = buildTarget();
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve({
        ok: true,
        text: () => Promise.resolve(`<div id="${id}"><p class="loaded">loaded</p></div>`),
      })
    ));

    click(path);

    // vi.waitFor 는 fn 내부의 expect 가 throw 를 멈출 때까지 재시도한다
    await vi.waitFor(() => {
      const el = document.getElementById(id);
      expect(el).not.toBeNull();
      expect(el.querySelector('.loaded')).not.toBeNull();
    });

    expect(document.getElementById(id).querySelector('.loaded').textContent).toBe('loaded');
  });
});

describe('loadFragmentFallback — fetch 실패', () => {
  test('fetch 실패 시 error state 를 표시한다', async () => {
    const { path, el } = buildTarget();
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('Network error'))));

    click(path);

    await vi.waitFor(() => {
      expect(el.querySelector('[data-state="error"]').hidden).toBe(false);
    });

    expect(el.querySelector('[data-state="loading"]').hidden).toBe(true);
  });

  test('HTTP 에러 응답 시 error state 를 표시한다', async () => {
    const { path, el } = buildTarget();
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve({ ok: false, status: 503, text: () => Promise.resolve('') })
    ));

    click(path);

    await vi.waitFor(() => {
      expect(el.querySelector('[data-state="error"]').hidden).toBe(false);
    });
  });
});

describe('retry backoff', () => {
  test('retry button 이 backoff 중이면 fetch 를 다시 호출하지 않는다', async () => {
    const { id, path } = buildTarget();
    const retryBtn = document.createElement('button');
    retryBtn.className = 'kraft-retry-btn';
    retryBtn.setAttribute('data-target', id);
    document.body.appendChild(retryBtn);

    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('fail'))));

    // 첫 번째 클릭 — fetch 실패 → backoff 시작
    click(path);
    await vi.waitFor(() => {
      expect(fetch.mock.calls.length).toBeGreaterThanOrEqual(1);
    });

    const callsBefore = fetch.mock.calls.length;

    // retry button 즉시 재클릭 — backoff 중이므로 fetch 호출 안 됨
    retryBtn.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await new Promise(r => setTimeout(r, 30));

    expect(fetch.mock.calls.length).toBe(callsBefore);
  });
});

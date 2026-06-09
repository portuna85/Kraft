import { beforeEach, describe, expect, test, vi } from 'vitest';

// IIFE 실행 전 submit/htmx 를 막아 테스트 루프 방지
HTMLFormElement.prototype.submit = vi.fn();

import '../../main/resources/static/js/main.js';

const RULE_A = 'BirthdayBiasRule';
const RULE_B = 'LongRunRule';

function buildDom({ checkedA = true, checkedB = false } = {}) {
  document.body.innerHTML = `
    <form id="recommendForm">
      <input type="number" name="count" value="5">
      <input type="hidden" name="oddCount" id="filterOddCount" value="">
      <input type="hidden" name="sumMin" id="filterSumMin" value="">
      <input type="hidden" name="sumMax" id="filterSumMax" value="">
    </form>
    <div>
      <input type="checkbox" class="recommend-rule-checkbox"
             data-rule-name="${RULE_A}" ${checkedA ? 'checked' : ''}>
      <input type="checkbox" class="recommend-rule-checkbox"
             data-rule-name="${RULE_B}" ${checkedB ? 'checked' : ''}>
    </div>
    <div class="filter-btn-row">
      <button type="button" class="filter-btn active" data-target="filterOddCount" data-value=""></button>
      <button type="button" class="filter-btn" data-target="filterOddCount" data-value="3"></button>
    </div>
  `;
}

describe('syncDisabledRuleInputs', () => {
  beforeEach(() => { buildDom(); });

  test('체크 해제된 규칙에 대해 hidden input 을 생성한다', () => {
    const form = document.getElementById('recommendForm');
    form.dispatchEvent(new Event('submit', { bubbles: true }));

    const inputs = form.querySelectorAll('[data-generated-disabled-rule]');
    expect(inputs).toHaveLength(1);
    expect(inputs[0].name).toBe('disabledRules');
    expect(inputs[0].value).toBe(RULE_B);
  });

  test('모든 규칙이 체크되면 hidden input 을 생성하지 않는다', () => {
    buildDom({ checkedA: true, checkedB: true });
    const form = document.getElementById('recommendForm');
    form.dispatchEvent(new Event('submit', { bubbles: true }));

    expect(form.querySelectorAll('[data-generated-disabled-rule]')).toHaveLength(0);
  });

  test('중복 submit 시 이전 hidden input 을 제거하고 새로 생성한다', () => {
    const form = document.getElementById('recommendForm');
    form.dispatchEvent(new Event('submit', { bubbles: true }));
    form.dispatchEvent(new Event('submit', { bubbles: true }));

    expect(form.querySelectorAll('[data-generated-disabled-rule]')).toHaveLength(1);
  });
});

describe('filter button', () => {
  beforeEach(() => { buildDom(); });

  test('filter-btn 클릭 시 data-target input 값을 갱신한다', () => {
    const btn = document.querySelector('[data-value="3"]');
    btn.dispatchEvent(new MouseEvent('click', { bubbles: true }));

    expect(document.getElementById('filterOddCount').value).toBe('3');
  });

  test('filter-btn 클릭 시 active 클래스를 해당 버튼으로 이동한다', () => {
    const btn = document.querySelector('[data-value="3"]');
    btn.dispatchEvent(new MouseEvent('click', { bubbles: true }));

    expect(btn.classList.contains('active')).toBe(true);
    expect(document.querySelector('[data-value=""]').classList.contains('active')).toBe(false);
  });
});

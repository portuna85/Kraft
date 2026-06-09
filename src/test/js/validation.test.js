import { beforeEach, describe, expect, test } from 'vitest';

import '../../main/resources/static/js/validation.js';

function buildInputWithFeedback({ id = 'countInput', min = 1, max = 10, value = '' } = {}) {
  document.body.innerHTML = `
    <form id="testForm">
      <label for="${id}">세트 수</label>
      <input id="${id}" type="number" min="${min}" max="${max}" value="${value}" aria-describedby="${id}Feedback">
      <div id="${id}Feedback" class="visually-hidden"></div>
    </form>
  `;
  // kraft:fragmentLoaded 로 validation binding 재실행
  document.dispatchEvent(new Event('kraft:fragmentLoaded'));
  return {
    input: document.getElementById(id),
    feedback: document.getElementById(id + 'Feedback'),
  };
}

describe('number input validation', () => {
  beforeEach(() => { document.body.innerHTML = ''; });

  test('유효한 값 입력 시 is-invalid 클래스가 없다', () => {
    const { input } = buildInputWithFeedback({ value: '5' });
    input.dispatchEvent(new Event('blur'));
    expect(input.classList.contains('is-invalid')).toBe(false);
  });

  test('빈 값 입력 시 is-invalid 클래스가 추가된다', () => {
    const { input, feedback } = buildInputWithFeedback({ value: '' });
    input.dispatchEvent(new Event('blur'));
    expect(input.classList.contains('is-invalid')).toBe(true);
    expect(feedback.classList.contains('visually-hidden')).toBe(false);
  });

  test('범위 초과 값 입력 시 is-invalid 클래스가 추가된다', () => {
    const { input, feedback } = buildInputWithFeedback({ value: '99' });
    input.dispatchEvent(new Event('blur'));
    expect(input.classList.contains('is-invalid')).toBe(true);
    expect(feedback.textContent).toMatch(/1부터 10/);
  });

  test('최솟값과 최댓값 경계는 유효하다', () => {
    const { input: inputMin } = buildInputWithFeedback({ id: 'countInput', value: '1' });
    inputMin.dispatchEvent(new Event('blur'));
    expect(inputMin.classList.contains('is-invalid')).toBe(false);

    const { input: inputMax } = buildInputWithFeedback({ id: 'countInput', value: '10' });
    inputMax.dispatchEvent(new Event('blur'));
    expect(inputMax.classList.contains('is-invalid')).toBe(false);
  });

  test('소수점 값 입력 시 is-invalid 클래스가 추가된다', () => {
    const { input } = buildInputWithFeedback({ value: '3.5' });
    input.dispatchEvent(new Event('blur'));
    expect(input.classList.contains('is-invalid')).toBe(true);
  });

  test('유효하지 않은 값 수정 후 유효값 입력 시 오류 상태가 해제된다', () => {
    const { input, feedback } = buildInputWithFeedback({ value: '0' });
    input.dispatchEvent(new Event('blur'));
    expect(input.classList.contains('is-invalid')).toBe(true);

    input.value = '5';
    input.dispatchEvent(new Event('input'));
    expect(input.classList.contains('is-invalid')).toBe(false);
    expect(feedback.classList.contains('visually-hidden')).toBe(true);
  });
});

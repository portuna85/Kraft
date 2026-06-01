(function () {
  'use strict';

  // document에 단일 위임 리스너 — HTMX outerHTML 스왑 후에도 유지됨
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('.filter-btn');
    if (!btn) { return; }

    // 같은 그룹 내 active 해제 후 현재 버튼 활성화
    btn.closest('.filter-btn-row').querySelectorAll('.filter-btn').forEach(function (b) {
      b.classList.remove('active');
    });
    btn.classList.add('active');

    // 홀짝 버튼: data-target="filterOddCount"
    if (btn.dataset.target) {
      var input = document.getElementById(btn.dataset.target);
      if (input) { input.value = btn.dataset.value || ''; }
    } else {
      // 합산 버튼: data-sum-min / data-sum-max
      var minInput = document.getElementById('filterSumMin');
      var maxInput = document.getElementById('filterSumMax');
      if (minInput) { minInput.value = btn.dataset.sumMin || ''; }
      if (maxInput) { maxInput.value = btn.dataset.sumMax || ''; }
    }

    // 폼 재제출 (항상 현재 DOM에서 찾음)
    var form = document.getElementById('recommendForm');
    if (form && window.htmx) {
      window.htmx.trigger(form, 'submit');
    }
  });
}());

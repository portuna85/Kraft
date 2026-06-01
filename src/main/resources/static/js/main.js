(function () {
  'use strict';

  function initRecommendFilters(root) {
    root.querySelectorAll('.filter-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var target = btn.dataset.target;

        if (target) {
          // 홀짝 버튼
          btn.closest('.filter-btn-row').querySelectorAll('.filter-btn').forEach(function (b) {
            b.classList.remove('active');
          });
          btn.classList.add('active');
          var input = document.getElementById(target);
          if (input) { input.value = btn.dataset.value; }
        } else {
          // 합산 버튼
          btn.closest('.filter-btn-row').querySelectorAll('.filter-btn').forEach(function (b) {
            b.classList.remove('active');
          });
          btn.classList.add('active');
          var minInput = document.getElementById('filterSumMin');
          var maxInput = document.getElementById('filterSumMax');
          if (minInput) { minInput.value = btn.dataset.sumMin; }
          if (maxInput) { maxInput.value = btn.dataset.sumMax; }
        }

        // 자동 재생성
        var form = document.getElementById('recommendForm');
        if (form && window.htmx) {
          window.htmx.trigger(form, 'submit');
        }
      });
    });
  }

  // 초기 로드
  document.addEventListener('DOMContentLoaded', function () {
    initRecommendFilters(document);
  });

  // HTMX swap 후 재초기화
  document.addEventListener('htmx:afterSwap', function (evt) {
    initRecommendFilters(evt.detail.target || document);
  });
}());

(function () {
  'use strict';

  // 현재 URL 기반으로 상단 nav와 하단 nav의 active 클래스를 동기화
  (function syncNavActive() {
    var path = window.location.pathname;
    var isHome = path === '/' || path === '/recommend';

    document.querySelectorAll('.navbar-nav .nav-link').forEach(function (link) {
      var href = link.getAttribute('href');
      var shouldBeActive = href === path || (href === '/' && isHome);
      link.classList.toggle('active', shouldBeActive);
      if (shouldBeActive) {
        link.setAttribute('aria-current', 'page');
      } else {
        link.removeAttribute('aria-current');
      }
    });

    document.querySelectorAll('.kraft-bottom-nav-item').forEach(function (link) {
      var href = link.getAttribute('href');
      var shouldBeActive = href === path || (href === '/' && isHome);
      link.classList.toggle('active', shouldBeActive);
      if (shouldBeActive) {
        link.setAttribute('aria-current', 'page');
      } else {
        link.removeAttribute('aria-current');
      }
    });
  }());

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

  // 규칙 체크박스 변경 시 폼 재제출
  document.addEventListener('change', function (e) {
    if (!e.target.classList.contains('recommend-rule-checkbox')) { return; }
    var form = document.getElementById('recommendForm');
    if (form && window.htmx) {
      window.htmx.trigger(form, 'submit');
    }
  });

  // HTMX 요청 직전에 비활성 규칙을 파라미터에 추가
  document.addEventListener('htmx:configRequest', function (e) {
    if (!e.target || e.target.id !== 'recommendForm') { return; }
    var checkboxes = document.querySelectorAll('.recommend-rule-checkbox');
    var disabled = [];
    checkboxes.forEach(function (cb) {
      if (!cb.checked) { disabled.push(cb.dataset.ruleName); }
    });
    if (disabled.length > 0) {
      e.detail.parameters['disabledRules'] = disabled;
    }
  });
}());

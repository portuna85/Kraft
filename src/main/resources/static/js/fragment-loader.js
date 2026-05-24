(function () {
  'use strict';

  function announce(message) {
    var region = document.getElementById('fragment-live-region');
    if (!region) return;
    region.textContent = '';
    setTimeout(function () { region.textContent = message; }, 0);
  }

  function setUiState(target, state, message) {
    if (!target) return;
    target.setAttribute('aria-busy', state === 'loading' ? 'true' : 'false');
    Array.prototype.forEach.call(target.querySelectorAll('[data-state]'), function (el) {
      el.hidden = el.getAttribute('data-state') !== state;
    });
    if (message) announce(message);
  }

  function stateMessage(id) {
    if (id === 'frequency') return '번호 출현 빈도를 불러왔습니다.';
    if (id === 'rounds') return '회차 목록을 불러왔습니다.';
    if (id === 'recommend') return '추천 결과를 업데이트했습니다.';
    return '콘텐츠를 불러왔습니다.';
  }

  function notifyLoaded() {
    document.dispatchEvent(new CustomEvent('kraft:fragmentLoaded'));
  }

  function loadFragmentFallback(target) {
    if (!target) return Promise.resolve();
    var url = target.getAttribute('hx-get');
    if (!url) return Promise.resolve();
    setUiState(target, 'loading');
    return fetch(url, { headers: { 'HX-Request': 'true' } })
      .then(function (response) {
        if (!response.ok) throw new Error('fragment load failed');
        return response.text();
      })
      .then(function (fragmentHtml) {
        target.outerHTML = fragmentHtml;
        announce('콘텐츠를 불러왔습니다.');
        notifyLoaded();
      })
      .catch(function () {
        setUiState(target, 'error', '콘텐츠를 불러오지 못했습니다.');
      });
  }

  if (window.htmx) {
    document.body.addEventListener('htmx:beforeRequest', function (event) {
      setUiState(event.target, 'loading');
    });
    document.body.addEventListener('htmx:afterSwap', function (event) {
      setUiState(event.target, 'success', stateMessage(event.target.id));
      notifyLoaded();
    });
    document.body.addEventListener('htmx:responseError', function (event) {
      var target = event.detail && event.detail.elt ? event.detail.elt : event.target;
      setUiState(target, 'error', '콘텐츠를 불러오지 못했습니다.');
    });
  } else {
    Array.prototype.forEach.call(document.querySelectorAll('[hx-trigger="load"][hx-get]'), function (target) {
      loadFragmentFallback(target);
    });
  }

  document.body.addEventListener('click', function (event) {
    var retryBtn = event.target.closest('.kraft-retry-btn');
    if (!retryBtn) return;
    var target = document.getElementById(retryBtn.getAttribute('data-target'));
    if (!target) return;
    if (window.htmx) {
      setUiState(target, 'loading');
      window.htmx.trigger(target, 'load');
      return;
    }
    loadFragmentFallback(target);
  });
}());

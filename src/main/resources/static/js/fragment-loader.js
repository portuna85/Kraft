(function () {
  'use strict';

  var inFlightByTarget = Object.create(null);
  var retryAttemptByTarget = Object.create(null);
  var retryBlockedUntilByTarget = Object.create(null);

  function announce(message) {
    var region = document.getElementById('fragment-live-region');
    if (!region) return;
    region.textContent = '';
    setTimeout(function () { region.textContent = message; }, 0);
  }

  function focusLoadedSection(target) {
    if (!target) return;
    var heading = target.querySelector('h2, .card-title, [data-focus-target]');
    if (!heading) return;
    heading.setAttribute('tabindex', '-1');
    heading.focus();
  }

  function notifyLoaded() {
    document.dispatchEvent(new CustomEvent('kraft:fragmentLoaded'));
  }

  function targetKey(target) {
    if (!target) return null;
    return target.id || target.getAttribute('hx-get') || null;
  }

  function retryKey(target) {
    return targetKey(target) || '';
  }

  function resetRetryState(target) {
    var key = retryKey(target);
    if (!key) return;
    retryAttemptByTarget[key] = 0;
    retryBlockedUntilByTarget[key] = 0;
  }

  function calcBackoffMs(target) {
    var key = retryKey(target);
    if (!key) return 0;
    var attempt = (retryAttemptByTarget[key] || 0) + 1;
    retryAttemptByTarget[key] = attempt;
    var delay = Math.min(1000 * Math.pow(2, attempt - 1), 8000);
    retryBlockedUntilByTarget[key] = Date.now() + delay;
    return delay;
  }

  function isRetryBlocked(target) {
    var key = retryKey(target);
    if (!key) return false;
    return Date.now() < (retryBlockedUntilByTarget[key] || 0);
  }

  function setUiState(target, state, message) {
    if (!target) return;
    target.setAttribute('aria-busy', state === 'loading' ? 'true' : 'false');
    Array.prototype.forEach.call(target.querySelectorAll('[data-state]'), function (el) {
      el.hidden = el.getAttribute('data-state') !== state;
    });
    if (message) announce(message);
  }

  function errorMessage(statusCode, backoffMs) {
    var statusText = statusCode ? ('HTTP ' + statusCode) : '네트워크 오류';
    var retryText = backoffMs > 0 ? (' ' + Math.ceil(backoffMs / 1000) + '초 후 다시 시도해 주세요.') : '';
    return '콘텐츠를 불러오지 못했습니다 (' + statusText + ').' + retryText;
  }

  function stateMessage(id) {
    if (id === 'frequency') return '번호 출현 빈도를 불러왔습니다.';
    if (id === 'rounds') return '회차 목록을 불러왔습니다.';
    if (id === 'recommend') return '추천 결과를 갱신했습니다.';
    return '콘텐츠를 불러왔습니다.';
  }

  function resolveHtmxTarget(event) {
    var detail = event && event.detail;
    if (detail && detail.target && detail.target.nodeType === 1) {
      return detail.target;
    }
    var source = detail && detail.elt && detail.elt.nodeType === 1 ? detail.elt : event.target;
    if (!source || source.nodeType !== 1) {
      return null;
    }
    var selector = source.getAttribute('hx-target');
    if (selector) {
      return document.querySelector(selector);
    }
    return source;
  }

  function resolveTargetFromSource(source) {
    if (!source || source.nodeType !== 1) {
      return null;
    }
    var selector = source.getAttribute('hx-target');
    return selector ? document.querySelector(selector) : source;
  }

  function buildUrlWithFormData(baseUrl, form) {
    var url = new URL(baseUrl, window.location.origin);
    var data = new FormData(form);
    data.forEach(function (value, key) {
      if (value !== '') {
        url.searchParams.append(key, value);
      }
    });
    return url.pathname + url.search;
  }

  function clearInFlight(target) {
    var key = targetKey(target);
    if (!key) return;
    delete inFlightByTarget[key];
  }

  function shouldFocusOnSwap(event) {
    var detail = event && event.detail;
    var requestConfig = detail && detail.requestConfig;
    var triggeringEvent = requestConfig && requestConfig.triggeringEvent;
    return !!triggeringEvent;
  }

  function loadFragmentFallback(target, focusAfterLoad, requestUrl, swapStyle) {
    if (!target) return Promise.resolve();
    var key = targetKey(target);
    if (!key) return Promise.resolve();
    if (inFlightByTarget[key]) return inFlightByTarget[key];

    var url = requestUrl || target.getAttribute('hx-get');
    if (!url) return Promise.resolve();
    var targetId = target.id;
    var swap = swapStyle || target.getAttribute('hx-swap') || 'innerHTML';

    setUiState(target, 'loading');
    inFlightByTarget[key] = fetch(url, { headers: { 'HX-Request': 'true' } })
      .then(function (response) {
        if (!response.ok) {
          var httpError = new Error('fragment load failed');
          httpError.statusCode = response.status;
          throw httpError;
        }
        return response.text();
      })
      .then(function (fragmentHtml) {
        if (swap.indexOf('outerHTML') >= 0) {
          target.outerHTML = fragmentHtml;
        } else {
          target.innerHTML = fragmentHtml;
        }
        var updated = targetId ? document.getElementById(targetId) : target;
        resetRetryState(updated || target);
        if (focusAfterLoad) {
          focusLoadedSection(updated);
        }
        announce('콘텐츠를 불러왔습니다.');
        notifyLoaded();
      })
      .catch(function (error) {
        var backoffMs = calcBackoffMs(target);
        setUiState(target, 'error', errorMessage(error && error.statusCode, backoffMs));
      })
      .finally(function () {
        clearInFlight(target);
      });

    return inFlightByTarget[key];
  }

  function bindFallbackInteractions() {
    document.body.addEventListener('click', function (event) {
      var source = event.target.closest('[hx-get]');
      if (!source || source.tagName === 'FORM') return;
      if (source.closest('.disabled,[aria-disabled="true"]')) {
        event.preventDefault();
        return;
      }
      var target = resolveTargetFromSource(source);
      var url = source.getAttribute('hx-get');
      if (!target || !url) return;
      event.preventDefault();
      loadFragmentFallback(target, true, url, source.getAttribute('hx-swap'));
    });

    document.body.addEventListener('submit', function (event) {
      var form = event.target;
      if (!form || !form.matches('form[hx-get]')) return;
      var target = resolveTargetFromSource(form);
      var url = buildUrlWithFormData(form.getAttribute('hx-get'), form);
      if (!target || !url) return;
      event.preventDefault();
      loadFragmentFallback(target, true, url, form.getAttribute('hx-swap'));
    });
  }

  if (window.htmx) {
    document.body.addEventListener('htmx:beforeRequest', function (event) {
      var target = resolveHtmxTarget(event);
      var key = targetKey(target);
      if (!key) return;
      if (inFlightByTarget[key]) {
        event.preventDefault();
        return;
      }
      inFlightByTarget[key] = true;
      setUiState(target, 'loading');
    });

    document.body.addEventListener('htmx:afterSwap', function (event) {
      var target = resolveHtmxTarget(event);
      if (!target) return;
      setUiState(target, 'success', stateMessage(target.id));
      resetRetryState(target);
      if (shouldFocusOnSwap(event)) {
        focusLoadedSection(target);
      }
      clearInFlight(target);
      notifyLoaded();
    });

    document.body.addEventListener('htmx:responseError', function (event) {
      var target = resolveHtmxTarget(event);
      if (!target) return;
      var statusCode = event && event.detail && event.detail.xhr ? event.detail.xhr.status : null;
      var backoffMs = calcBackoffMs(target);
      setUiState(target, 'error', errorMessage(statusCode, backoffMs));
      clearInFlight(target);
    });
  } else {
    Array.prototype.forEach.call(document.querySelectorAll('[hx-trigger="load"][hx-get]'), function (target) {
      loadFragmentFallback(target, false);
    });
    bindFallbackInteractions();
  }

  document.body.addEventListener('click', function (event) {
    var retryBtn = event.target.closest('.kraft-retry-btn');
    if (!retryBtn) return;
    var target = document.getElementById(retryBtn.getAttribute('data-target'));
    if (!target) return;
    if (isRetryBlocked(target)) return;
    if (window.htmx) {
      var key = targetKey(target);
      if (key && inFlightByTarget[key]) return;
      setUiState(target, 'loading');
      window.htmx.trigger(target, 'load');
      return;
    }
    loadFragmentFallback(target, true);
  });
}());

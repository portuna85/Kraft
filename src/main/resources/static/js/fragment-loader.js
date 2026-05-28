(function () {
  'use strict';

  // 1. State variables
  var inFlightByTarget = Object.create(null);
  var retryAttemptByTarget = Object.create(null);
  var retryBlockedUntilByTarget = Object.create(null);

  // 2. Accessibility helpers
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

  // 3. Retry/backoff helpers
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

  // 4. UI state helpers
  function setUiState(target, state, message) {
    if (!target) return;
    target.setAttribute('aria-busy', state === 'loading' ? 'true' : 'false');
    Array.prototype.forEach.call(target.querySelectorAll('[data-state]'), function (el) {
      el.hidden = el.getAttribute('data-state') !== state;
    });
    if (message) announce(message);
  }

  function errorMessage(statusCode, backoffMs) {
    var statusText = statusCode ? ('HTTP ' + statusCode) : '?ㅽ듃?뚰겕 ?ㅻ쪟';
    var retryText = backoffMs > 0 ? (' ??' + Math.ceil(backoffMs / 1000) + '珥????ъ떆?꾪빐 二쇱꽭??') : '';
    return '肄섑뀗痢좊? 遺덈윭?ㅼ? 紐삵뻽?듬땲?? (' + statusText + ').' + retryText;
  }

  function stateMessage(id) {
    if (id === 'frequency') return '踰덊샇 異쒗쁽 鍮덈룄瑜?遺덈윭?붿뒿?덈떎.';
    if (id === 'rounds') return '?뚯감 紐⑸줉??遺덈윭?붿뒿?덈떎.';
    if (id === 'recommend') return '異붿쿇 寃곌낵瑜?媛깆떊?덉뒿?덈떎.';
    return '肄섑뀗痢좊? 遺덈윭?붿뒿?덈떎.';
  }

  // 5. Target resolution helpers
  function targetKey(target) {
    if (!target) return null;
    return target.id || target.getAttribute('hx-get') || null;
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

  // 6. Fallback fetch loader
  function loadFragmentFallback(target, focusAfterLoad) {
    if (!target) return Promise.resolve();
    var key = targetKey(target);
    if (!key) return Promise.resolve();
    if (inFlightByTarget[key]) return inFlightByTarget[key];

    var url = target.getAttribute('hx-get');
    if (!url) return Promise.resolve();

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
        target.outerHTML = fragmentHtml;
        var updated = document.getElementById(target.id);
        resetRetryState(updated || target);
        if (focusAfterLoad) {
          focusLoadedSection(updated);
        }
        announce('肄섑뀗痢좊? 遺덈윭?붿뒿?덈떎.');
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

  // 7. HTMX event bindings
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
  }

  // 8. Retry button binding
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

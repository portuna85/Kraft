(function () {
  'use strict';

  var html = document.documentElement;
  var btn = document.getElementById('theme-btn');
  var navMain = document.getElementById('navMain');
  var navToggleBtn = document.getElementById('navToggleBtn');

  function safeSetLocalStorage(key, value) {
    try {
      localStorage.setItem(key, value);
    } catch (e) {
      // Ignore storage failures (private mode, blocked storage, quota exceeded).
    }
  }

  function setThemeButtonState(themeButton) {
    var isDark = html.getAttribute('data-bs-theme') === 'dark';
    themeButton.textContent = isDark ? '라이트' : '다크';
    themeButton.setAttribute('aria-pressed', isDark ? 'true' : 'false');
    themeButton.setAttribute('aria-label', isDark ? '라이트 테마로 전환' : '다크 테마로 전환');
  }

  function setNavToggleState(expanded) {
    if (!navToggleBtn) {
      return;
    }
    navToggleBtn.setAttribute('aria-expanded', expanded ? 'true' : 'false');
    navToggleBtn.setAttribute('aria-label', expanded ? '메뉴 닫기' : '메뉴 열기');
  }

  function setFeedbackState(input, feedbackEl, invalid, message) {
    if (invalid) {
      input.setAttribute('aria-invalid', 'true');
      input.classList.add('is-invalid');
      feedbackEl.textContent = message;
      feedbackEl.classList.remove('visually-hidden');
      return;
    }
    input.removeAttribute('aria-invalid');
    input.classList.remove('is-invalid');
    feedbackEl.classList.add('visually-hidden');
  }

  function bindNumberInputValidation(inputId, min, max, label) {
    var input = document.getElementById(inputId);
    var feedback = document.getElementById(inputId + 'Feedback');
    if (!input || !feedback || input.dataset.validationBound === 'true') {
      return;
    }
    input.dataset.validationBound = 'true';

    function validate() {
      var raw = input.value.trim();
      if (!raw) {
        setFeedbackState(input, feedback, true, label + ' 입력은 필수입니다.');
        return false;
      }
      var parsed = Number(raw);
      if (!Number.isInteger(parsed) || parsed < min || parsed > max) {
        setFeedbackState(input, feedback, true, label + '는 ' + min + '에서 ' + max + ' 사이의 정수여야 합니다.');
        return false;
      }
      setFeedbackState(input, feedback, false, '');
      return true;
    }

    input.addEventListener('input', validate);
    input.addEventListener('blur', validate);

    var form = input.form;
    if (form && form.dataset.validationBound !== 'true') {
      form.dataset.validationBound = 'true';
      form.addEventListener('submit', function (event) {
        var isValid = validate();
        if (!isValid) {
          event.preventDefault();
          event.stopPropagation();
        }
      });
    }
  }

  function liveRegion() {
    return document.getElementById('fragment-live-region');
  }

  function announce(message) {
    var region = liveRegion();
    if (!region) {
      return;
    }
    region.textContent = '';
    window.setTimeout(function () {
      region.textContent = message;
    }, 0);
  }

  function applyBusyState(target, busy) {
    if (!target) {
      return;
    }
    target.setAttribute('aria-busy', busy ? 'true' : 'false');
  }

  function setStateVisible(container, stateName) {
    if (!container) {
      return;
    }
    var states = container.querySelectorAll('[data-state]');
    Array.prototype.forEach.call(states, function (el) {
      el.hidden = el.getAttribute('data-state') !== stateName;
    });
  }

  function statusMessageById(id) {
    if (id === 'frequency') {
      return '번호 출현 빈도를 불러왔습니다.';
    }
    if (id === 'rounds') {
      return '회차 목록을 불러왔습니다.';
    }
    if (id === 'recommend') {
      return '추천 결과를 업데이트했습니다.';
    }
    return '';
  }

  function loadFragmentFallback(target) {
    if (!target) {
      return Promise.resolve();
    }
    var url = target.getAttribute('hx-get');
    if (!url) {
      return Promise.resolve();
    }
    applyBusyState(target, true);
    setStateVisible(target, 'loading');
    return fetch(url, { headers: { 'HX-Request': 'true' } })
      .then(function (response) {
        if (!response.ok) {
          throw new Error('fragment load failed: ' + response.status);
        }
        return response.text();
      })
      .then(function (fragmentHtml) {
        announce('콘텐츠를 불러왔습니다.');
        target.outerHTML = fragmentHtml;
        bindDynamicValidation();
      })
      .catch(function () {
        applyBusyState(target, false);
        setStateVisible(target, 'error');
        announce('콘텐츠를 불러오지 못했습니다.');
      });
  }

  function bindDynamicValidation() {
    bindNumberInputValidation('countInput', 1, 10, '추천 개수');
    bindNumberInputValidation('roundInput', 1, 3000, '회차');
  }

  if (btn) {
    setThemeButtonState(btn);
    btn.addEventListener('click', function () {
      var next = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
      html.setAttribute('data-bs-theme', next);
      safeSetLocalStorage('theme', next);
      setThemeButtonState(btn);
    });
  }

  if (navMain) {
    function setMenuOpen(open) {
      navMain.classList.toggle('show', open);
      setNavToggleState(open);
    }

    setMenuOpen(navMain.classList.contains('show'));

    if (navToggleBtn) {
      navToggleBtn.addEventListener('click', function () {
        setMenuOpen(!navMain.classList.contains('show'));
      });
    }

    var navLinks = navMain.querySelectorAll('.nav-link[href*="#"]');
    Array.prototype.forEach.call(navLinks, function (link) {
      link.addEventListener('click', function () {
        if (!navMain.classList.contains('show')) {
          return;
        }
        setMenuOpen(false);
      });
    });
  }

  function loadHtmxFallbackFragments() {
    if (window.htmx) {
      return;
    }
    var loadTargets = document.querySelectorAll('[hx-trigger="load"][hx-get]');
    Array.prototype.forEach.call(loadTargets, function (target) {
      loadFragmentFallback(target);
    });
  }

  if (window.htmx) {
    document.body.addEventListener('htmx:beforeRequest', function (event) {
      applyBusyState(event.target, true);
    });

    document.body.addEventListener('htmx:afterSwap', function (event) {
      applyBusyState(event.target, false);
      var targetId = event.target && event.target.id ? event.target.id : '';
      var message = statusMessageById(targetId);
      if (message) {
        announce(message);
      }
      bindDynamicValidation();
    });

    document.body.addEventListener('htmx:responseError', function (event) {
      var target = event.detail && event.detail.elt ? event.detail.elt : null;
      if (!target) {
        return;
      }
      applyBusyState(target, false);
      setStateVisible(target, 'error');
    });

    document.body.addEventListener('htmx:sendError', function (event) {
      var target = event.detail && event.detail.elt ? event.detail.elt : null;
      if (!target) {
        return;
      }
      applyBusyState(target, false);
      setStateVisible(target, 'error');
    });
  }

  document.body.addEventListener('click', function (event) {
    var retryBtn = event.target.closest('.kraft-retry-btn');
    if (!retryBtn) {
      return;
    }
    var targetId = retryBtn.getAttribute('data-target');
    var section = targetId ? document.getElementById(targetId) : null;
    if (!section) {
      return;
    }

    if (window.htmx) {
      setStateVisible(section, 'loading');
      applyBusyState(section, true);
      window.htmx.trigger(section, 'load');
      return;
    }

    loadFragmentFallback(section);
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      bindDynamicValidation();
      loadHtmxFallbackFragments();
    });
  } else {
    bindDynamicValidation();
    loadHtmxFallbackFragments();
  }

  function initBottomNav() {
    var navItems = document.querySelectorAll('.kraft-bottom-nav-item');
    if (!navItems.length || !window.IntersectionObserver) {
      return;
    }

    var sectionIds = ['latest', 'recommend', 'round-search', 'frequency', 'rounds'];
    var visibleSections = {};
    var observedIds = {};

    function updateActiveItem() {
      var activeId = null;
      for (var i = 0; i < sectionIds.length; i++) {
        if (visibleSections[sectionIds[i]]) {
          activeId = sectionIds[i];
          break;
        }
      }
      Array.prototype.forEach.call(navItems, function (item) {
        var isActive = item.getAttribute('href') === '#' + activeId;
        item.classList.toggle('active', isActive);
        if (isActive) {
          item.setAttribute('aria-current', 'true');
        } else {
          item.removeAttribute('aria-current');
        }
      });
    }

    Array.prototype.forEach.call(navItems, function (item) {
      item.addEventListener('click', function () {
        var target = item.getAttribute('href');
        Array.prototype.forEach.call(navItems, function (navItem) {
          var isActive = navItem.getAttribute('href') === target;
          navItem.classList.toggle('active', isActive);
          if (isActive) {
            navItem.setAttribute('aria-current', 'true');
          } else {
            navItem.removeAttribute('aria-current');
          }
        });
      });
    });

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        visibleSections[entry.target.id] = entry.isIntersecting;
      });
      updateActiveItem();
    }, { threshold: 0.2 });

    function observeSection(el) {
      if (!el || observedIds[el.id]) {
        return;
      }
      observer.observe(el);
      observedIds[el.id] = true;
    }

    sectionIds.forEach(function (id) {
      observeSection(document.getElementById(id));
    });

    document.body.addEventListener('htmx:afterSwap', function (event) {
      if (event.target && sectionIds.indexOf(event.target.id) !== -1) {
        observeSection(event.target);
      }
    });
  }

  initBottomNav();

  function ballColorClass(n) {
    if (n <= 10) return 'b1';
    if (n <= 20) return 'b2';
    if (n <= 30) return 'b3';
    if (n <= 40) return 'b4';
    return 'b5';
  }

  window.kraftUi = {
    createBallElement: function (number, isBonus) {
      var span = document.createElement('span');
      var cls = 'kraft-ball ' + ballColorClass(number);
      if (isBonus) cls += ' bonus';
      span.className = cls;
      span.textContent = number;
      return span;
    }
  };
}());

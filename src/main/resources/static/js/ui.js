(function () {
  'use strict';

  var html = document.documentElement;
  var btn = document.getElementById('theme-btn');
  var navMain = document.getElementById('navMain');
  var navToggleBtn = document.getElementById('navToggleBtn');
  var bottomNavInited = false;

  function setThemeButtonState(themeButton) {
    var isDark = html.getAttribute('data-bs-theme') === 'dark';
    themeButton.textContent = isDark ? '라이트' : '다크';
    themeButton.setAttribute('aria-pressed', isDark ? 'true' : 'false');
    themeButton.setAttribute('aria-label', isDark ? '라이트 테마로 전환' : '다크 테마로 전환');
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

  function validateBoundInput(input, feedback, min, max, label) {
    var raw = input.value.trim();
    if (!raw) {
      setFeedbackState(input, feedback, true, label + ' 입력은 필수입니다.');
      return false;
    }
    var parsed = Number(raw);
    if (!Number.isInteger(parsed) || parsed < min || parsed > max) {
      setFeedbackState(input, feedback, true, label + '은 ' + min + '에서 ' + max + ' 사이의 정수여야 합니다.');
      return false;
    }
    setFeedbackState(input, feedback, false, '');
    return true;
  }

  function bindNumberInputValidation(inputId, min, max, label) {
    var input = document.getElementById(inputId);
    var feedback = document.getElementById(inputId + 'Feedback');
    if (!input || !feedback || input.dataset.validationBound === 'true') return;
    input.dataset.validationBound = 'true';

    function validate() {
      return validateBoundInput(input, feedback, min, max, label);
    }

    input.addEventListener('input', validate);
    input.addEventListener('blur', validate);
    input.addEventListener('wheel', function (e) { input.blur(); e.preventDefault(); }, { passive: false });
    input.addEventListener('paste', function (e) {
      var text = (e.clipboardData || window.clipboardData).getData('text');
      if (!/^\d+$/.test(text.trim())) {
        e.preventDefault();
      }
    });

    var form = input.form;
    if (form && form.dataset.validationBound !== 'true') {
      form.dataset.validationBound = 'true';
      form.addEventListener('submit', function (event) {
        var fields = Array.prototype.slice.call(form.querySelectorAll('input[type="number"]'));
        var firstInvalid = null;
        fields.forEach(function (field) {
          var fieldFeedback = document.getElementById(field.id + 'Feedback');
          if (!fieldFeedback) return;
          var fMin = Number(field.getAttribute('min') || 1);
          var fMax = Number(field.getAttribute('max') || 3000);
          var fLabel = (form.querySelector('label[for="' + field.id + '"]') || {}).textContent || '입력값';
          if (!validateBoundInput(field, fieldFeedback, fMin, fMax, fLabel.trim()) && !firstInvalid) firstInvalid = field;
        });
        if (firstInvalid) {
          event.preventDefault();
          event.stopPropagation();
          firstInvalid.focus();
          firstInvalid.select();
        }
      });
    }
  }

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

  function bindDynamicValidation() {
    bindNumberInputValidation('countInput', 1, 10, '추천 세트 수');
    bindNumberInputValidation('roundInput', 1, 3000, '회차');
  }

  function stateMessage(id) {
    if (id === 'frequency') return '번호 출현 빈도를 불러왔습니다.';
    if (id === 'rounds') return '회차 목록을 불러왔습니다.';
    if (id === 'recommend') return '추천 결과를 업데이트했습니다.';
    return '콘텐츠를 불러왔습니다.';
  }

  function setBottomNavActive(navItems, activeId) {
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

  function initBottomNav() {
    var nav = document.querySelector('.kraft-bottom-nav');
    if (!nav || !window.IntersectionObserver) return;

    var navItems = nav.querySelectorAll('.kraft-bottom-nav-item');
    if (!navItems.length) return;

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
      if (activeId) {
        setBottomNavActive(navItems, activeId);
      }
    }

    var header = document.querySelector('.navbar.sticky-top');
    var headerHeight = header ? header.offsetHeight : 0;
    var bottomNavHeight = nav.offsetHeight || 0;
    var topMargin = -(headerHeight + 12);
    var bottomMargin = -(bottomNavHeight + 12);

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        visibleSections[entry.target.id] = entry.isIntersecting;
      });
      updateActiveItem();
    }, {
      threshold: 0.25,
      rootMargin: topMargin + 'px 0px ' + bottomMargin + 'px 0px'
    });

    function observeSection(el) {
      if (!el || observedIds[el.id]) return;
      observer.observe(el);
      observedIds[el.id] = true;
    }

    sectionIds.forEach(function (id) {
      observeSection(document.getElementById(id));
    });

    if (!bottomNavInited) {
      Array.prototype.forEach.call(navItems, function (item) {
        item.addEventListener('click', function () {
          var target = item.getAttribute('href').replace('#', '');
          setBottomNavActive(navItems, target);
        });
      });
      bottomNavInited = true;
    }
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
        bindDynamicValidation();
        initBottomNav();
        announce('콘텐츠를 불러왔습니다.');
      })
      .catch(function () {
        setUiState(target, 'error', '콘텐츠를 불러오지 못했습니다.');
      });
  }

  if (btn) {
    setThemeButtonState(btn);
    btn.addEventListener('click', function () {
      var next = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
      html.setAttribute('data-bs-theme', next);
      try { localStorage.setItem('theme', next); } catch (e) {}
      setThemeButtonState(btn);
    });
  }

  if (navMain && navToggleBtn) {
    navToggleBtn.addEventListener('click', function () {
      var open = !navMain.classList.contains('show');
      navMain.classList.toggle('show', open);
      navToggleBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
      navToggleBtn.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    });
  }

  if (window.htmx) {
    document.body.addEventListener('htmx:beforeRequest', function (event) {
      setUiState(event.target, 'loading');
    });
    document.body.addEventListener('htmx:afterSwap', function (event) {
      setUiState(event.target, 'success', stateMessage(event.target.id));
      bindDynamicValidation();
      initBottomNav();
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

  bindDynamicValidation();
  initBottomNav();
}());

(function () {
  'use strict';

  var RECOMMEND_HISTORY_KEY = 'kraft-recommend-history';
  var RECOMMEND_HISTORY_MAX = 10;
  var GENERATED_RULE_INPUT = 'data-generated-disabled-rule';

  function recommendForm() {
    return document.getElementById('recommendForm');
  }

  function syncDisabledRuleInputs(form) {
    if (!form) return;
    Array.prototype.forEach.call(form.querySelectorAll('input[' + GENERATED_RULE_INPUT + ']'), function (input) {
      input.remove();
    });

    Array.prototype.forEach.call(document.querySelectorAll('.recommend-rule-checkbox'), function (checkbox) {
      if (checkbox.checked) return;
      var input = document.createElement('input');
      input.type = 'hidden';
      input.name = 'disabledRules';
      input.value = checkbox.dataset.ruleName || '';
      input.setAttribute(GENERATED_RULE_INPUT, 'true');
      form.appendChild(input);
    });
  }

  function submitRecommendForm(form) {
    if (!form) return;
    syncDisabledRuleInputs(form);
    if (window.htmx && typeof window.htmx.trigger === 'function') {
      window.htmx.trigger(form, 'submit');
      return;
    }
    if (typeof form.requestSubmit === 'function') {
      form.requestSubmit();
      return;
    }
    form.submit();
  }

  function activateFilterButton(btn) {
    var row = btn.closest('.filter-btn-row');
    if (row) {
      Array.prototype.forEach.call(row.querySelectorAll('.filter-btn'), function (item) {
        item.classList.remove('active');
      });
    }
    btn.classList.add('active');
  }

  function updateFilterInputs(btn) {
    if (btn.dataset.target) {
      var input = document.getElementById(btn.dataset.target);
      if (input) {
        input.value = btn.dataset.value || '';
      }
      return;
    }

    var minInput = document.getElementById('filterSumMin');
    var maxInput = document.getElementById('filterSumMax');
    if (minInput) {
      minInput.value = btn.dataset.sumMin || '';
    }
    if (maxInput) {
      maxInput.value = btn.dataset.sumMax || '';
    }
  }

  document.addEventListener('submit', function (event) {
    if (event.target && event.target.id === 'recommendForm') {
      syncDisabledRuleInputs(event.target);
    }
  }, true);

  document.addEventListener('click', function (event) {
    var btn = event.target.closest('.filter-btn');
    if (!btn) return;

    activateFilterButton(btn);
    updateFilterInputs(btn);
    submitRecommendForm(recommendForm());
  });

  document.addEventListener('change', function (event) {
    if (!event.target.classList.contains('recommend-rule-checkbox')) return;
    submitRecommendForm(recommendForm());
  });

  document.addEventListener('click', function (event) {
    var copyBtn = event.target.closest('.set-card-copy-btn');
    if (!copyBtn) return;
    var card = copyBtn.closest('.set-card');
    if (!card) return;
    var numbers = Array.from(card.querySelectorAll('.ball-row .kraft-ball'))
      .map(function (el) { return el.textContent.trim(); })
      .filter(Boolean)
      .join(', ');
    if (!numbers) return;

    function remember() {
      try {
        var history = JSON.parse(localStorage.getItem(RECOMMEND_HISTORY_KEY) || '[]');
        history = history.filter(function (item) { return item !== numbers; });
        history.unshift(numbers);
        if (history.length > RECOMMEND_HISTORY_MAX) {
          history.length = RECOMMEND_HISTORY_MAX;
        }
        localStorage.setItem(RECOMMEND_HISTORY_KEY, JSON.stringify(history));
      } catch (ignored) {}
    }

    function feedback(ok) {
      copyBtn.textContent = ok ? '복사됨' : '실패';
      copyBtn.disabled = true;
      setTimeout(function () {
        copyBtn.textContent = '복사';
        copyBtn.disabled = false;
      }, 1200);
      if (ok) {
        remember();
      }
    }

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(numbers).then(function () { feedback(true); }, function () { feedback(false); });
      return;
    }

    var textarea = document.createElement('textarea');
    textarea.value = numbers;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
      document.execCommand('copy');
      feedback(true);
    } catch (err) {
      feedback(false);
    }
    document.body.removeChild(textarea);
  });
}());

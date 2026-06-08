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
}());

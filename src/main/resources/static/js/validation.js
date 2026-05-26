(function () {
  'use strict';

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
      setFeedbackState(input, feedback, true, label + ' 값을 입력해 주세요.');
      return false;
    }
    var parsed = Number(raw);
    if (!Number.isInteger(parsed) || parsed < min || parsed > max) {
      setFeedbackState(input, feedback, true, label + '은(는) ' + min + '부터 ' + max + ' 사이의 정수여야 합니다.');
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
    input.setAttribute('aria-describedby', inputId + 'Feedback');

    function validate() {
      return validateBoundInput(input, feedback, min, max, label);
    }

    input.addEventListener('input', validate);
    input.addEventListener('blur', validate);
    input.addEventListener('wheel', function (e) { input.blur(); e.preventDefault(); }, { passive: false });
    input.addEventListener('paste', function (e) {
      var text = (e.clipboardData || window.clipboardData).getData('text');
      if (!/^\d+$/.test(text.trim())) { e.preventDefault(); }
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
          var labelEl = form.querySelector('label[for="' + field.id + '"]');
          var fLabel = labelEl ? labelEl.textContent : 'Input';
          if (!validateBoundInput(field, fieldFeedback, fMin, fMax, fLabel.trim()) && !firstInvalid) {
            firstInvalid = field;
          }
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

  function bindFromElement(id, label) {
    var el = document.getElementById(id);
    if (!el) return;
    var min = Number(el.getAttribute('min') || 1);
    var max = Number(el.getAttribute('max') || 9999);
    bindNumberInputValidation(id, min, max, label);
  }

  function bindDynamicValidation() {
    bindFromElement('countInput', '세트 수');
    bindFromElement('roundInput', '회차');
  }

  bindDynamicValidation();
  document.addEventListener('kraft:fragmentLoaded', bindDynamicValidation);
}());

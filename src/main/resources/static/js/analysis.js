(function () {
  var selected = [];
  var MAX = 6;
  var picker = document.getElementById('analysis-picker');
  var display = document.getElementById('analysis-selected-display');
  var hint = document.getElementById('analysis-hint');

  if (!picker) return;

  picker.querySelectorAll('.ball-picker-item').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var num = +this.dataset.number;
      var idx = selected.indexOf(num);
      if (idx >= 0) {
        selected.splice(idx, 1);
        this.classList.remove('selected');
      } else if (selected.length < MAX) {
        selected.push(num);
        this.classList.add('selected');
      }
      updateDisplay();
      if (selected.length === MAX) triggerAnalysis();
    });
  });

  function updateDisplay() {
    if (selected.length === 0) {
      display.innerHTML = '<span class="analysis-selected-placeholder">\uBC88\uD638\uB97C 6\uAC1C \uC120\uD0DD\uD558\uC138\uC694</span>';
      return;
    }
    var sorted = selected.slice().sort(function (a, b) { return a - b; });
    display.innerHTML = sorted.map(function (n) {
      return '<span class="kraft-ball b' + ballGroup(n) + '">' + n + '</span>';
    }).join('');
  }

  function triggerAnalysis() {
    var sorted = selected.slice().sort(function (a, b) { return a - b; });
    var params = sorted.map(function (n, i) {
      return 'n' + (i + 1) + '=' + encodeURIComponent(n);
    }).join('&');
    hint.textContent = '\uBD84\uC11D \uC911...';
    requestFragment('/fragments/analysis?' + params, '#analysis-result')
      .then(function () {
        hint.textContent = '6\uAC1C \uC120\uD0DD \uC2DC \uC790\uB3D9\uC73C\uB85C \uBD84\uC11D\uD569\uB2C8\uB2E4';
      })
      .catch(function () {
        var target = document.getElementById('analysis-result');
        if (target) {
          target.innerHTML = '<p class="text-danger small mt-2">\uBD84\uC11D\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.</p>';
        }
        hint.textContent = '6\uAC1C \uC120\uD0DD \uC2DC \uC790\uB3D9\uC73C\uB85C \uBD84\uC11D\uD569\uB2C8\uB2E4';
      });
  }

  function requestFragment(url, targetSelector) {
    if (window.htmx && typeof window.htmx.ajax === 'function') {
      var htmxRequest = window.htmx.ajax('GET', url, { target: targetSelector, swap: 'innerHTML' });
      return htmxRequest && typeof htmxRequest.then === 'function' ? htmxRequest : Promise.resolve();
    }

    var target = document.querySelector(targetSelector);
    if (!target) return Promise.reject(new Error('Target element not found'));
    return fetch(url, { headers: { 'HX-Request': 'true' } })
      .then(function (response) {
        if (!response.ok) throw new Error('Fragment request failed');
        return response.text();
      })
      .then(function (html) {
        target.innerHTML = html;
      });
  }

  function ballGroup(n) {
    return Math.floor((n - 1) / 10) + 1;
  }
})();

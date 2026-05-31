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
      display.innerHTML = '<span class="analysis-selected-placeholder">번호를 6개 선택하세요</span>';
      return;
    }
    var sorted = selected.slice().sort(function (a, b) { return a - b; });
    display.innerHTML = sorted.map(function (n) {
      return '<span class="kraft-ball b' + ballGroup(n) + '">' + n + '</span>';
    }).join('');
  }

  function triggerAnalysis() {
    var sorted = selected.slice().sort(function (a, b) { return a - b; });
    var params = sorted.map(function (n, i) { return 'n' + (i + 1) + '=' + n; }).join('&');
    hint.textContent = '분석 중…';
    htmx.ajax('GET', '/fragments/analysis?' + params, { target: '#analysis-result', swap: 'innerHTML' })
      .then(function () { hint.textContent = '6개 선택 시 자동으로 분석됩니다'; });
  }

  function ballGroup(n) { return Math.floor((n - 1) / 10) + 1; }
})();

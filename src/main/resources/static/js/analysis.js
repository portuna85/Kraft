(function () {
  var selected = [];
  var picker = document.getElementById('analysis-picker');
  var display = document.getElementById('analysis-selected-display');
  var hint = document.getElementById('analysis-hint');

  if (!picker) return;

  BallPicker.init({
    picker: picker,
    maxSelection: 6,
    onSelect: function (numbers) {
      selected = numbers.slice().sort(function (a, b) { return a - b; });
      updateDisplay();
    },
    onComplete: function (numbers) {
      triggerAnalysis(numbers.slice().sort(function (a, b) { return a - b; }));
    }
  });

  function updateDisplay() {
    if (selected.length === 0) {
      display.innerHTML = '<span class="analysis-selected-placeholder">번호를 6개 선택하세요</span>';
      return;
    }
    display.innerHTML = selected.map(function (n) {
      return '<span class="kraft-ball b' + ballGroup(n) + '">' + n + '</span>';
    }).join('');
  }

  function triggerAnalysis(sorted) {
    var params = sorted.map(function (n, i) {
      return 'n' + (i + 1) + '=' + encodeURIComponent(n);
    }).join('&');
    hint.textContent = '분석 중...';
    BallPicker.requestFragment('/fragments/analysis?' + params, '#analysis-result')
      .then(function () {
        hint.textContent = '6개 선택 시 자동으로 분석합니다';
      })
      .catch(function () {
        var target = document.getElementById('analysis-result');
        if (target) {
          target.innerHTML = '<p class="text-danger small mt-2">분석을 불러오지 못했습니다.</p>';
        }
        hint.textContent = '6개 선택 시 자동으로 분석합니다';
      });
  }

  function ballGroup(n) {
    return Math.floor((n - 1) / 10) + 1;
  }
})();

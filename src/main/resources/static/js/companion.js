(function () {
  var picker = document.getElementById('companion-picker');

  if (!picker) return;

  BallPicker.init({
    picker: picker,
    maxSelection: 1,
    onSelect: function (numbers) {
      var resultEl = document.getElementById('companion-result');
      if (resultEl && !(window.htmx && typeof window.htmx.ajax === 'function')) {
        resultEl.innerHTML = '<p class="text-secondary small mt-2">결과를 불러오는 중입니다.</p>';
      }
      BallPicker.requestFragment(
        '/fragments/companion?target=' + encodeURIComponent(numbers[0]),
        '#companion-result'
      ).catch(function () {
        var target = document.getElementById('companion-result');
        if (target) {
          target.innerHTML = '<p class="text-danger small mt-2">동반 번호를 불러오지 못했습니다.</p>';
        }
      });
    }
  });
})();

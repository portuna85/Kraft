(function () {
  var current = null;
  var picker = document.getElementById('companion-picker');

  if (!picker) return;

  picker.querySelectorAll('.ball-picker-item').forEach(function (btn) {
    btn.addEventListener('click', function () {
      if (current !== null) {
        var prev = picker.querySelector('[data-number="' + current + '"]');
        if (prev) prev.classList.remove('selected');
      }
      current = +this.dataset.number;
      this.classList.add('selected');
      htmx.ajax('GET', '/fragments/companion?target=' + current, { target: '#companion-result', swap: 'innerHTML' });
    });
  });
})();

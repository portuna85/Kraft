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
      requestFragment('/fragments/companion?target=' + encodeURIComponent(current), '#companion-result')
        .catch(function () {
          var target = document.getElementById('companion-result');
          if (target) {
            target.innerHTML = '<p class="text-danger small mt-2">\uB3D9\uBC18 \uBC88\uD638\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.</p>';
          }
        });
    });
  });

  function requestFragment(url, targetSelector) {
    if (window.htmx && typeof window.htmx.ajax === 'function') {
      var htmxRequest = window.htmx.ajax('GET', url, { target: targetSelector, swap: 'innerHTML' });
      return htmxRequest && typeof htmxRequest.then === 'function' ? htmxRequest : Promise.resolve();
    }

    var target = document.querySelector(targetSelector);
    if (!target) return Promise.reject(new Error('Target element not found'));
    target.innerHTML = '<p class="text-secondary small mt-2">\uACB0\uACFC\uB97C \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4.</p>';
    return fetch(url, { headers: { 'HX-Request': 'true' } })
      .then(function (response) {
        if (!response.ok) throw new Error('Fragment request failed');
        return response.text();
      })
      .then(function (html) {
        target.innerHTML = html;
      });
  }
})();

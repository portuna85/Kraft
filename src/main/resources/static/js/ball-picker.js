// Shared ball picker selection logic and HTMX fragment fallback.
// options for BallPicker.init:
//   picker      {string|Element} - picker element id or the element itself
//   maxSelection {number}         - max selectable balls (default 1 = single-select)
//   onSelect    {function(nums)} - called on every selection change
//   onComplete  {function(nums)} - called when maxSelection is reached (multi-select only)
(function (global) {
  'use strict';

  function requestFragment(url, targetSelector) {
    if (window.htmx && typeof window.htmx.ajax === 'function') {
      var req = window.htmx.ajax('GET', url, { target: targetSelector, swap: 'innerHTML' });
      return req && typeof req.then === 'function' ? req : Promise.resolve();
    }
    var target = document.querySelector(targetSelector);
    if (!target) return Promise.reject(new Error('Target element not found'));
    return fetch(url, { headers: { 'HX-Request': 'true' } })
      .then(function (r) {
        if (!r.ok) throw new Error('Fragment request failed');
        return r.text();
      })
      .then(function (html) { target.innerHTML = html; });
  }

  function initBallPicker(options) {
    var pickerEl = typeof options.picker === 'string'
      ? document.getElementById(options.picker) : options.picker;
    if (!pickerEl) return;

    var maxSelection = options.maxSelection || 1;
    var onComplete = options.onComplete || null;
    var onSelect = options.onSelect || null;
    var selected = [];
    var currentSingle = null;

    pickerEl.querySelectorAll('.ball-picker-item').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var num = +this.dataset.number;
        if (maxSelection === 1) {
          if (currentSingle !== null) {
            var prev = pickerEl.querySelector('[data-number="' + currentSingle + '"]');
            if (prev) prev.classList.remove('selected');
          }
          currentSingle = num;
          this.classList.add('selected');
          if (onSelect) onSelect([currentSingle]);
        } else {
          var idx = selected.indexOf(num);
          if (idx >= 0) {
            selected.splice(idx, 1);
            this.classList.remove('selected');
          } else if (selected.length < maxSelection) {
            selected.push(num);
            this.classList.add('selected');
          }
          if (onSelect) onSelect(selected.slice());
          if (selected.length === maxSelection && onComplete) {
            onComplete(selected.slice());
          }
        }
      });
    });
  }

  global.BallPicker = { init: initBallPicker, requestFragment: requestFragment };
}(window));

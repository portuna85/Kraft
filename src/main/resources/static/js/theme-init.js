(function () {
  'use strict';

  function readStoredTheme() {
    try {
      return localStorage.getItem('theme');
    } catch (e) {
      return null;
    }
  }

  var stored = readStoredTheme();
  var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme:dark)').matches;
  var theme = stored || (prefersDark ? 'dark' : 'light');
  document.documentElement.setAttribute('data-bs-theme', theme);
}());

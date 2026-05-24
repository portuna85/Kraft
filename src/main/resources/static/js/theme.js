(function () {
  'use strict';

  var html = document.documentElement;
  var btn = document.getElementById('theme-btn');
  if (!btn) return;

  function setState() {
    var dark = html.getAttribute('data-bs-theme') === 'dark';
    btn.textContent = dark ? 'Light' : 'Dark';
    btn.setAttribute('aria-pressed', dark ? 'true' : 'false');
    btn.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
  }

  setState();
  btn.addEventListener('click', function () {
    var next = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-bs-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) {}
    setState();
  });
}());
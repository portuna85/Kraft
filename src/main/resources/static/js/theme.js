(function () {
  'use strict';

  var html = document.documentElement;
  var btn  = document.getElementById('theme-btn');
  var moon = document.getElementById('theme-icon-moon');
  var sun  = document.getElementById('theme-icon-sun');
  if (!btn) return;

  function setState() {
    var dark = html.getAttribute('data-bs-theme') === 'dark';
    if (moon) moon.classList.toggle('theme-icon--hidden', dark);
    if (sun)  sun.classList.toggle('theme-icon--hidden', !dark);
    btn.setAttribute('aria-pressed', dark ? 'true' : 'false');
    btn.setAttribute('aria-label',   dark ? '라이트 테마로 전환' : '다크 테마로 전환');
  }

  setState();
  btn.addEventListener('click', function () {
    var next = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-bs-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) {}
    setState();
  });
}());

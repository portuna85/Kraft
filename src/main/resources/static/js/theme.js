(function () {
  'use strict';

  var html = document.documentElement;
  var btn = document.getElementById('theme-btn');
  if (!btn) return;

  function setState() {
    var dark = html.getAttribute('data-bs-theme') === 'dark';
    btn.textContent = dark ? '라이트' : '다크';
    btn.setAttribute('aria-pressed', dark ? 'true' : 'false');
    btn.setAttribute('aria-label', dark ? '라이트 테마로 전환' : '다크 테마로 전환');
  }

  setState();
  btn.addEventListener('click', function () {
    var next = html.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-bs-theme', next);
    try { localStorage.setItem('theme', next); } catch (e) {}
    setState();
  });
}());
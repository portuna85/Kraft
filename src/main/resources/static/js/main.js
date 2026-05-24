(function () {
  'use strict';

  var navMain = document.getElementById('navMain');
  var navToggleBtn = document.getElementById('navToggleBtn');

  if (navMain && navToggleBtn) {
    navToggleBtn.addEventListener('click', function () {
      var open = !navMain.classList.contains('show');
      navMain.classList.toggle('show', open);
      navToggleBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
      navToggleBtn.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    });
  }
}());
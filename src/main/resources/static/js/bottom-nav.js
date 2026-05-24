(function () {
  'use strict';

  var bottomNavInited = false;

  function setBottomNavActive(navItems, activeId) {
    Array.prototype.forEach.call(navItems, function (item) {
      var isActive = item.getAttribute('href') === '#' + activeId;
      item.classList.toggle('active', isActive);
      if (isActive) {
        item.setAttribute('aria-current', 'true');
      } else {
        item.removeAttribute('aria-current');
      }
    });
  }

  function initBottomNav() {
    var nav = document.querySelector('.kraft-bottom-nav');
    if (!nav || !window.IntersectionObserver) return;

    var navItems = nav.querySelectorAll('.kraft-bottom-nav-item');
    if (!navItems.length) return;

    var sectionIds = ['latest', 'recommend', 'round-search', 'frequency', 'rounds'];
    var visibleSections = {};
    var observedIds = {};

    function updateActiveItem() {
      for (var i = 0; i < sectionIds.length; i++) {
        if (visibleSections[sectionIds[i]]) {
          setBottomNavActive(navItems, sectionIds[i]);
          return;
        }
      }
    }

    var header = document.querySelector('.navbar.sticky-top');
    var headerHeight = header ? header.offsetHeight : 0;
    var bottomNavHeight = nav.offsetHeight || 0;

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        visibleSections[entry.target.id] = entry.isIntersecting;
      });
      updateActiveItem();
    }, {
      threshold: 0.25,
      rootMargin: -(headerHeight + 12) + 'px 0px ' + -(bottomNavHeight + 12) + 'px 0px'
    });

    function observeSection(el) {
      if (!el || observedIds[el.id]) return;
      observer.observe(el);
      observedIds[el.id] = true;
    }

    sectionIds.forEach(function (id) { observeSection(document.getElementById(id)); });

    if (!bottomNavInited) {
      Array.prototype.forEach.call(navItems, function (item) {
        item.addEventListener('click', function () {
          setBottomNavActive(navItems, item.getAttribute('href').replace('#', ''));
        });
      });
      bottomNavInited = true;
    }
  }

  initBottomNav();
  document.addEventListener('kraft:fragmentLoaded', initBottomNav);
}());

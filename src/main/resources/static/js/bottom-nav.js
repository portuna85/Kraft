(function () {
  'use strict';

  var bottomNavInited = false;
  var refreshBottomNavActive = function () {};

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
    if (bottomNavInited) {
      refreshBottomNavActive();
      return;
    }
    var nav = document.querySelector('.kraft-bottom-nav');
    if (!nav || !window.IntersectionObserver) return;

    var navItems = nav.querySelectorAll('.kraft-bottom-nav-item');
    if (!navItems.length) return;

    var sectionIds = ['latest', 'recommend', 'round-search', 'frequency', 'rounds'];
    var sectionElements = [];
    var observedIds = {};
    var ticking = false;

    function viewportCenterY() {
      var header = document.querySelector('.navbar.sticky-top');
      var headerHeight = header ? header.offsetHeight : 0;
      var bottomHeight = nav.offsetHeight || 0;
      var top = headerHeight;
      var bottom = window.innerHeight - bottomHeight;
      return top + Math.max((bottom - top) / 2, 0);
    }

    function updateActiveItem() {
      if (!sectionElements.length) {
        return;
      }
      var centerY = viewportCenterY();
      var closest = null;
      var closestDistance = Number.POSITIVE_INFINITY;
      for (var i = 0; i < sectionElements.length; i++) {
        var section = sectionElements[i];
        var rect = section.getBoundingClientRect();
        if (rect.height <= 0) {
          continue;
        }
        var sectionCenter = rect.top + rect.height / 2;
        var distance = Math.abs(sectionCenter - centerY);
        if (distance < closestDistance) {
          closestDistance = distance;
          closest = section;
        }
      }
      if (closest && closest.id) {
        setBottomNavActive(navItems, closest.id);
      }
    }

    var observer = new IntersectionObserver(function () {
      updateActiveItem();
    }, {
      threshold: [0, 0.25, 0.5, 0.75, 1]
    });

    function observeSection(el) {
      if (!el || observedIds[el.id]) return;
      sectionElements.push(el);
      observer.observe(el);
      observedIds[el.id] = true;
    }

    sectionIds.forEach(function (id) { observeSection(document.getElementById(id)); });

    Array.prototype.forEach.call(navItems, function (item) {
      item.addEventListener('click', function () {
        setBottomNavActive(navItems, item.getAttribute('href').replace('#', ''));
      });
    });
    bottomNavInited = true;

    refreshBottomNavActive = function () {
      if (ticking) return;
      ticking = true;
      window.requestAnimationFrame(function () {
        updateActiveItem();
        ticking = false;
      });
    };

    window.addEventListener('scroll', function () {
      refreshBottomNavActive();
    }, { passive: true });

    window.addEventListener('resize', function () {
      refreshBottomNavActive();
    });
    refreshBottomNavActive();
  }

  initBottomNav();
  document.addEventListener('kraft:fragmentLoaded', function () {
    if (!bottomNavInited) {
      initBottomNav();
      return;
    }
    refreshBottomNavActive();
  });
}());

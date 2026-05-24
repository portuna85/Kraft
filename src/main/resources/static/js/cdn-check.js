(function () {
  'use strict';

  // htmx: 이 스크립트는 htmx <script> 이후에 실행되므로 window.htmx 유무로 로드 성공 판단
  window.kraftCdnStatus.htmxLoaded = !!window.htmx;
  window.kraftCdnStatus.htmxFailed = !window.htmx;

  // Bootstrap CSS: d-none 클래스 적용 여부로 로드 성공 판단
  var testEl = document.createElement('div');
  testEl.className = 'd-none';
  testEl.setAttribute('aria-hidden', 'true');
  document.body.appendChild(testEl);
  var needsFallback = window.getComputedStyle(testEl).display !== 'none';
  document.body.removeChild(testEl);

  if (needsFallback) {
    var link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = '/webjars/bootstrap/5.3.3/css/bootstrap.min.css';
    document.head.appendChild(link);
    window.kraftCdnStatus.bootstrapFailed = true;
  }
}());

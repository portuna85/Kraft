// jsdom에는 requestSubmit이 없으므로 stub 처리
if (typeof HTMLFormElement !== 'undefined' && !HTMLFormElement.prototype.requestSubmit) {
  HTMLFormElement.prototype.requestSubmit = function () {
    this.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  };
}

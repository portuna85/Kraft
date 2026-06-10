document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('form[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (e) => {
      if (!window.confirm(form.dataset.confirm)) {
        e.preventDefault();
        return;
      }
      const submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) setLoading(submitBtn);
    });
  });

  document.querySelectorAll('button[data-confirm]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      if (!window.confirm(btn.dataset.confirm)) {
        e.preventDefault();
        return;
      }
      setLoading(btn);
    });
  });
});

function setLoading(btn) {
  btn.disabled = true;
  btn.dataset.originalText = btn.innerHTML;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>처리 중...';
}

const PAGE_SIZE = 5;

function initPagination(listId, paginationId) {
  const list = document.getElementById(listId);
  const pager = document.getElementById(paginationId);
  if (!list || !pager) return null;

  const items = Array.from(list.querySelectorAll('.store-card'));
  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  let current = 1;

  const currentEl = pager.querySelector('.store-page-current');
  const totalEl = pager.querySelector('.store-page-total');
  const prevBtn = pager.querySelector('[data-dir="prev"]');
  const nextBtn = pager.querySelector('[data-dir="next"]');

  if (totalEl) totalEl.textContent = totalPages;
  if (totalPages <= 1) pager.classList.add('d-none');

  function render() {
    const from = (current - 1) * PAGE_SIZE;
    const to = from + PAGE_SIZE;
    items.forEach((item, i) => {
      item.hidden = i < from || i >= to;
    });
    if (currentEl) currentEl.textContent = current;
    if (prevBtn) prevBtn.disabled = current <= 1;
    if (nextBtn) nextBtn.disabled = current >= totalPages;
  }

  pager.addEventListener('click', e => {
    const btn = e.target.closest('[data-dir]');
    if (!btn || btn.disabled) return;
    if (btn.dataset.dir === 'prev' && current > 1) current--;
    else if (btn.dataset.dir === 'next' && current < totalPages) current++;
    render();
  });

  render();
  return () => { current = 1; render(); };
}

document.addEventListener('DOMContentLoaded', () => {
  const reset1 = initPagination('store-list-1', 'store-pagination-1');
  const reset2 = initPagination('store-list-2', 'store-pagination-2');

  const tabRow = document.getElementById('storeTabs');
  if (!tabRow) return;

  tabRow.addEventListener('click', e => {
    const btn = e.target.closest('.store-tab');
    if (!btn) return;

    tabRow.querySelectorAll('.store-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');

    const grade = btn.dataset.grade;
    ['1', '2'].forEach(g => {
      const list = document.getElementById('store-list-' + g);
      const pager = document.getElementById('store-pagination-' + g);
      if (g === grade) {
        list?.classList.remove('d-none');
        pager?.classList.remove('d-none');
      } else {
        list?.classList.add('d-none');
        pager?.classList.add('d-none');
      }
    });

    if (grade === '1' && reset1) reset1();
    if (grade === '2' && reset2) reset2();
  });
});

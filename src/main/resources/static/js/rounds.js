document.addEventListener('DOMContentLoaded', () => {
  const tabRow = document.getElementById('searchStoreTabs');
  if (!tabRow) return;
  tabRow.addEventListener('click', e => {
    const btn = e.target.closest('.store-tab');
    if (!btn) return;
    tabRow.querySelectorAll('.store-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    const targetId = btn.dataset.target;
    document.querySelectorAll('#search-store-list-1, #search-store-list-2')
            .forEach(l => l.classList.add('d-none'));
    const list = document.getElementById(targetId);
    if (list) list.classList.remove('d-none');
  });
});

document.addEventListener('DOMContentLoaded', () => {
  const tabRow = document.getElementById('storeTabs');
  if (!tabRow) return;
  tabRow.addEventListener('click', e => {
    const btn = e.target.closest('.store-tab');
    if (!btn) return;
    tabRow.querySelectorAll('.store-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');
    document.querySelectorAll('.store-list').forEach(l => l.classList.add('d-none'));
    const list = document.getElementById('store-list-' + btn.dataset.grade);
    if (list) list.classList.remove('d-none');
  });
});

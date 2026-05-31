document.addEventListener('click', function (e) {
  var btn = e.target.closest('.btn-sort');
  if (!btn) return;
  var section = btn.closest('.kraft-section-frequency');
  if (!section) return;
  var list = section.querySelector('#freq-list');
  if (!list) return;

  section.querySelectorAll('.btn-sort').forEach(function (b) { b.classList.remove('active'); });
  btn.classList.add('active');

  var items = Array.from(list.querySelectorAll('.freq-item'));
  var sort = btn.dataset.sort;
  items.sort(function (a, b) {
    if (sort === 'number')     return +a.dataset.number - +b.dataset.number;
    if (sort === 'count-desc') return +b.dataset.count  - +a.dataset.count;
    if (sort === 'count-asc')  return +a.dataset.count  - +b.dataset.count;
    return 0;
  });
  items.forEach(function (item) { list.appendChild(item); });
});

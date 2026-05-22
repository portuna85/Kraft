(function () {
  'use strict';

  function toComparable(value) {
    var text = (value || '').trim();
    var num = Number(text.replace(/,/g, ''));
    if (!Number.isNaN(num) && text !== '') {
      return { type: 'number', value: num };
    }
    var time = Date.parse(text);
    if (!Number.isNaN(time)) {
      return { type: 'number', value: time };
    }
    return { type: 'text', value: text.toLowerCase() };
  }

  function sortTableByColumn(table, columnIndex, ascending) {
    var tbody = table.tBodies[0];
    if (!tbody) return;

    var rows = Array.prototype.slice.call(tbody.rows);
    rows.sort(function (a, b) {
      var aCell = a.cells[columnIndex] ? a.cells[columnIndex].innerText : '';
      var bCell = b.cells[columnIndex] ? b.cells[columnIndex].innerText : '';
      var av = toComparable(aCell);
      var bv = toComparable(bCell);
      if (av.type === 'number' && bv.type === 'number') {
        return ascending ? av.value - bv.value : bv.value - av.value;
      }
      if (av.value < bv.value) return ascending ? -1 : 1;
      if (av.value > bv.value) return ascending ? 1 : -1;
      return 0;
    });

    rows.forEach(function (row) {
      tbody.appendChild(row);
    });
  }

  function bindTableSort() {
    var tables = document.querySelectorAll('.ops-sortable-table');
    tables.forEach(function (table) {
      var headers = table.querySelectorAll('thead th');
      headers.forEach(function (header, index) {
        var btn = header.querySelector('.ops-sort-btn');
        if (!btn) return;
        btn.addEventListener('click', function () {
          var ascending = header.getAttribute('aria-sort') !== 'ascending';
          headers.forEach(function (h) {
            h.setAttribute('aria-sort', 'none');
          });
          header.setAttribute('aria-sort', ascending ? 'ascending' : 'descending');
          sortTableByColumn(table, index, ascending);
        });
      });
    });
  }

  function bindMessageToggle() {
    document.querySelectorAll('.ops-msg-toggle').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var cell = btn.closest('td');
        if (!cell) return;
        var shortEl = cell.querySelector('.ops-msg-short');
        var fullEl = cell.querySelector('.ops-msg-full');
        if (!shortEl || !fullEl) return;

        var expanded = btn.getAttribute('aria-expanded') === 'true';
        if (expanded) {
          fullEl.classList.add('d-none');
          shortEl.classList.remove('d-none');
          btn.textContent = '더보기';
          btn.setAttribute('aria-expanded', 'false');
        } else {
          fullEl.classList.remove('d-none');
          shortEl.classList.add('d-none');
          btn.textContent = '접기';
          btn.setAttribute('aria-expanded', 'true');
        }
      });
    });
  }

  function init() {
    bindTableSort();
    bindMessageToggle();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
}());

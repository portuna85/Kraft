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
    var tables = document.querySelectorAll('table.table');
    tables.forEach(function (table) {
      var headers = table.querySelectorAll('thead th');
      headers.forEach(function (header, index) {
        header.style.cursor = 'pointer';
        header.addEventListener('click', function () {
          var asc = header.getAttribute('data-sort-dir') !== 'asc';
          headers.forEach(function (h) { h.removeAttribute('data-sort-dir'); });
          header.setAttribute('data-sort-dir', asc ? 'asc' : 'desc');
          sortTableByColumn(table, index, asc);
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

        var expanded = !fullEl.classList.contains('d-none');
        if (expanded) {
          fullEl.classList.add('d-none');
          shortEl.classList.remove('d-none');
          btn.textContent = 'more';
        } else {
          fullEl.classList.remove('d-none');
          shortEl.classList.add('d-none');
          btn.textContent = 'less';
        }
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      bindTableSort();
      bindMessageToggle();
    });
  } else {
    bindTableSort();
    bindMessageToggle();
  }
}());

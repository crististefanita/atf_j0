(function () {
  if (window.__atfTableViewportBound) return;
  window.__atfTableViewportBound = true;
  var surfaces = '${SCROLL_SURFACES}';
  function prepare(viewport) {
    if (!viewport.hasAttribute('tabindex')) viewport.tabIndex = 0;
    var table = Array.from(viewport.children).find(e => e.tagName === 'TABLE');
    if (!table || !table.tHead || !table.tHead.rows.length) return;
    var labels = Array.from(table.tHead.rows[0].cells, c => c.textContent.trim());
    Array.from(table.tBodies).forEach(body => Array.from(body.rows).forEach(row => {
      if (row.cells.length !== labels.length) return;
      Array.from(row.cells).forEach((cell, index) => {
        if (cell.getAttribute('data-atf-column-label') !== labels[index])
          cell.setAttribute('data-atf-column-label', labels[index]);
      });
    }));
  }
  function makeFocusable() {
    document.querySelectorAll(surfaces).forEach(prepare);
    new MutationObserver(records => {
      var changed = new Set();
      records.forEach(record => record.addedNodes.forEach(node => {
        if (node.nodeType !== 1) return;
        var owner = node.closest(surfaces);
        if (owner) changed.add(owner);
        node.querySelectorAll(surfaces).forEach(v => changed.add(v));
      }));
      changed.forEach(prepare);
    }).observe(document.body, { childList: true, subtree: true });
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', makeFocusable, { once: true });
  } else makeFocusable();
  document.addEventListener('keydown', function (event) {
    var viewport = event.target;
    if (!viewport || !viewport.matches || !viewport.matches(surfaces)
        || event.altKey || event.ctrlKey || event.metaKey || event.isComposing) return;
    var next = viewport.scrollTop;
    switch (event.key) {
      case 'Home': next = 0; break;
      case 'End': next = viewport.scrollHeight; break;
      case 'PageDown': next += viewport.clientHeight; break;
      case 'PageUp': next -= viewport.clientHeight; break;
      case 'ArrowDown': next += 40; break;
      case 'ArrowUp': next -= 40; break;
      case ' ': next += (event.shiftKey ? -1 : 1) * viewport.clientHeight; break;
      default: return;
    }
    event.preventDefault();
    viewport.scrollTop = next;
  }, true);
  window.addEventListener('wheel', function (event) {
    var target = event.target;
    if (!target || !target.closest || event.ctrlKey || event.metaKey || event.altKey || event.shiftKey
        || !event.deltaY || Math.abs(event.deltaX) > Math.abs(event.deltaY)
        || target.closest('pre,input,textarea,select,[contenteditable="true"]')) return;
    var viewport = target.closest(surfaces);
    if (!viewport || viewport.scrollHeight <= viewport.clientHeight) return;
    var unit = event.deltaMode === 2 ? viewport.clientHeight : event.deltaMode === 1 ? 16 : 1;
    event.preventDefault();
    event.stopPropagation();
    viewport.scrollTop += event.deltaY * unit;
  }, { capture: true, passive: false });
}());

var evidenceTableMeasureContext = document.createElement('canvas').getContext('2d'),
  EVIDENCE_STATUS_COLUMN = /(^|\b)(result|status|outcome)(\b|$)/,
  EVIDENCE_TIME_COLUMN = /(^|\b)(time|timestamp|duration)(\b|$)/,
  EVIDENCE_ATTEMPT_COLUMN = /(^|\b)(attempt|attempts)(\b|$)/;
function evidenceTableIntrinsicWidth(cell) {
  var value = (cell && cell.textContent ? cell.textContent : '').replace(/\s+/g, ' ').trim(),
    style = cell && window.getComputedStyle ? window.getComputedStyle(cell) : null,
    context = evidenceTableMeasureContext,
    padding = style ? parseFloat(style.paddingLeft) + parseFloat(style.paddingRight)
      + parseFloat(style.borderLeftWidth) + parseFloat(style.borderRightWidth) : 16;
  padding = Number.isFinite(padding) ? padding : 16;
  if (style && style.textTransform === 'uppercase') value = value.toUpperCase();
  if (style && style.textTransform === 'lowercase') value = value.toLowerCase();
  if (context) context.font = style && style.font ? style.font : '12px sans-serif';
  var measured = context ? context.measureText(value).width : value.length * 7.2;
  return Math.max(measured + padding + 2, 24);
}
function evidenceTableHeader(table, index) {
  var header = table.tHead && table.tHead.rows && table.tHead.rows[0];
  return (header && header.cells[index] ? header.cells[index].textContent : '')
    .replace(/\s+/g, ' ').trim().toLowerCase();
}
function isKeyValue(headers) { return headers.length === 2 && headers[0] === 'field' && headers[1] === 'value'; }
function evidenceTableColumnKind(header) {
  return EVIDENCE_STATUS_COLUMN.test(header) ? 1 : EVIDENCE_TIME_COLUMN.test(header) ? 2
    : EVIDENCE_ATTEMPT_COLUMN.test(header) ? 3 : 0;
}
function evidenceTableNumericColumn(table, index) {
  var values = Array.from(table.tBodies).flatMap(body => Array.from(body.rows))
    .filter(row => row.cells[index]).map(row => row.cells[index].textContent.trim()).filter(Boolean);
  return values.length > 0 && values.every(value => /^[+-]?\d+(?:[.,]\d+)?\s*%?$/.test(value));
}
/* Fill the host: meet measured demand first, then share surplus among flexible columns. */
function evidenceTableAllocateWidths(intrinsic, table, headerWidths, columnKinds) {
  var preferred = intrinsic.map((value, index) => Math.max(value || 24, headerWidths[index] || 24)),
    minimums = preferred.map((value, index) => Math.ceil(Math.max(headerWidths[index] || 24,
      columnKinds[index] ? value : Math.min(value, 96)))),
    minimumTotal = minimums.reduce((sum, value) => sum + value, 0);
  table.style.setProperty('--atf-content-min-width', minimumTotal + 'px');
  var tableWidth = Math.max(minimumTotal, table.clientWidth),
    remaining = Math.max(0, tableWidth - minimumTotal),
    demand = preferred.map((value, index) => Math.max(0, value - minimums[index])),
    demandTotal = demand.reduce((sum, value) => sum + value, 0),
    fulfilled = Math.min(remaining, demandTotal),
    widths = minimums.map((value, index) => value + (demandTotal ? fulfilled * demand[index] / demandTotal : 0)),
    surplus = remaining - fulfilled;
  if (surplus > 0) {
    var flexible = preferred.map((_, index) => index).filter(index => !columnKinds[index]);
    if (!flexible.length) flexible = preferred.map((_, index) => index);
    var weightTotal = flexible.reduce((sum, index) => sum + preferred[index], 0);
    flexible.forEach(index => { widths[index] += surplus * preferred[index] / weightTotal; });
  }
  var total = widths.reduce((sum, value) => sum + value, 0);
  return widths.map(value => value * 100 / total);
}
var ATF_EVIDENCE_TABLE_SELECTOR =
  '.atf-category-detail > table,' +
  'table.atf-ui-action-table,' +
  'table.atf-ui-observation-table,' +
  'table.assertions,' +
  'table.atf-setup-evidence-table,' +
  'table.helm-evidence-table,' +
  'table.helm-validation-table,' +
  ':is(${TABLE_VIEWPORTS}) > table';
var ATF_EVIDENCE_TABLE_LAYOUT_CACHE = '__atfEvidenceTableLayout';

function evidenceTableCandidates(root) {
  if (!root) return Array.from(document.querySelectorAll(ATF_EVIDENCE_TABLE_SELECTOR));
  if (Array.isArray(root)) return root;
  if (root.matches && root.matches(ATF_EVIDENCE_TABLE_SELECTOR)) return [root];
  return root.querySelectorAll
    ? Array.from(root.querySelectorAll(ATF_EVIDENCE_TABLE_SELECTOR)) : [];
}
function evidenceTableFingerprint(table, count) {
  var style = window.getComputedStyle ? window.getComputedStyle(table) : null,
    font = style
      ? [style.fontFamily, style.fontSize, style.fontWeight, style.lineHeight].join('/')
      : '';
  var parts = [count, table.clientWidth || 0, font,
    table.querySelectorAll(':scope>colgroup>col').length];
  parts.push(evidenceTableCache(table).contentVersion || 0);
  return parts.join('|');
}
function evidenceTableSetAttribute(table, name, value) {
  if (table.getAttribute(name) !== value) table.setAttribute(name, value);
}
function evidenceTableSetWidth(cell, width) {
  if (!width) return;
  cell.style.setProperty('width', width.toFixed(2) + '%', 'important');
  cell.style.setProperty('min-width', '0', 'important');
  cell.style.setProperty('max-width', 'none', 'important');
}
function evidenceTableCache(table) {
  return table[ATF_EVIDENCE_TABLE_LAYOUT_CACHE]
    || (table[ATF_EVIDENCE_TABLE_LAYOUT_CACHE] = {});
}
function evidenceTablesFromMutation(record) {
  var tables = new Set();
  var add = function (node) {
    if (!node || node.nodeType !== 1) return;
    if (node.matches && node.matches(ATF_EVIDENCE_TABLE_SELECTOR)) tables.add(node);
    if (node.closest) {
      var table = node.closest(ATF_EVIDENCE_TABLE_SELECTOR);
      if (table) tables.add(table);
    }
    if (node.querySelectorAll) {
      node.querySelectorAll(ATF_EVIDENCE_TABLE_SELECTOR).forEach(table => tables.add(table));
    }
  };
  add(record.target);
  Array.from(record.addedNodes || []).forEach(add);
  return Array.from(tables);
}

function invalidateEvidenceTableContent(node) {
  var table = node && node.closest && node.closest('table');
  while (table) {
    if (table.matches(ATF_EVIDENCE_TABLE_SELECTOR)) {
      var cache = evidenceTableCache(table);
      cache.contentVersion = (cache.contentVersion || 0) + 1;
    }
    table = table.parentElement && table.parentElement.closest('table');
  }
}
function evidenceTableVisible(table) {
  return !table.closest('details:not([open]),.d-none,.hide,[hidden]') && table.getClientRects().length > 0;
}
function observeVisibleEvidenceTables(tables) {
  if (!window.__atfValidationTableResizeObserver) return;
  tables.filter(evidenceTableVisible).forEach(table => window.__atfValidationTableResizeObserver.observe(table));
}
function normalizeEvidenceTableColumns(root) {
  evidenceTableCandidates(root).forEach(table => {
    if (!evidenceTableVisible(table)) return;
    var parent = table.parentElement;
    if (parent && !parent.matches('${TABLE_VIEWPORTS}')) {
      var viewport = document.createElement('div');
      viewport.className = '${DATA_TABLE_VIEWPORT}';
      viewport.tabIndex = 0;
      parent.insertBefore(viewport, table);
      viewport.appendChild(table);
    } else if (parent && !parent.hasAttribute('tabindex')) {
      parent.tabIndex = 0;
    }
    var rows = Array.from(table.rows || []),
      count = rows.length ? rows[0].cells.length : 0;
    if (count < 2) return;
    var cache = evidenceTableCache(table), fingerprint = evidenceTableFingerprint(table, count);
    if (cache.fingerprint === fingerprint) return;
    var headers = Array.from({ length: count }, (_, index) => evidenceTableHeader(table, index)),
      columnKinds = headers.map((header, index) => evidenceTableColumnKind(header)
        || (evidenceTableNumericColumn(table, index) ? 4 : 0)),
      headerRow = table.tHead && table.tHead.rows[0],
      headerWidths = headers.map((_, index) => evidenceTableIntrinsicWidth(
        headerRow && headerRow.cells[index]));

    var intrinsic = [];
    rows.forEach(row => {
      Array.from(row.cells || []).forEach((cell, index) => {
        intrinsic[index] = Math.max(intrinsic[index] || 0, evidenceTableIntrinsicWidth(cell));
      });
    });
    var widths = evidenceTableAllocateWidths(intrinsic, table, headerWidths, columnKinds);
    var widthSignature = widths
        .map(value => value.toFixed(2) + '%')
        .join('|'),
      maxSignature = intrinsic
        .map(value => Math.round(value || 0) + 'px')
        .join('|');
    if (cache.widthSignature !== widthSignature) rows.forEach(row => {
      Array.from(row.cells || []).forEach((cell, index) => evidenceTableSetWidth(cell, widths[index]));
    });
    var columns = Array.from(table.querySelectorAll(':scope>colgroup>col'));
    if (columns.length === count) columns.forEach((column, index) => {
      evidenceTableSetWidth(column, widths[index]);
    });
    evidenceTableSetAttribute(table, 'data-atf-content-layout', 'dynamic-longest-value');
    evidenceTableSetAttribute(table, 'data-atf-content-column-count', String(count));
    if (isKeyValue(headers)) {
      evidenceTableSetAttribute(table, 'data-atf-key-value-layout', 'field-value');
      evidenceTableSetAttribute(table, 'data-atf-key-column-width', widths[0].toFixed(2) + '%');
    }
    evidenceTableSetAttribute(table, 'data-atf-content-column-max-px', maxSignature);
    evidenceTableSetAttribute(table, 'data-atf-content-column-widths', widthSignature);
    if (table.classList.contains('helm-validation-table')) {
      evidenceTableSetAttribute(table, 'data-atf-validation-layout', 'dynamic-longest-value');
      evidenceTableSetAttribute(table, 'data-atf-validation-column-widths', widthSignature);
    }
    cache.fingerprint = evidenceTableFingerprint(table, count);
    cache.widthSignature = widthSignature;
  });
}
function normalizeValidationTableColumns(root) {
  normalizeEvidenceTableColumns(root);
}
function bindValidationTableLayout() {
  normalizeValidationTableColumns();
  if (window.__atfValidationTableLayoutBound) return;
  window.__atfValidationTableLayoutBound = true;
  var pendingTables = new Set(), pendingRoots = new Set(),
    schedule = function (tables) {
      (tables || Array.from(document.querySelectorAll(ATF_EVIDENCE_TABLE_SELECTOR))).forEach(table => {
        pendingTables.add(table);
      });
      if (window.__atfValidationTableLayoutFrame != null) return;
      var flush = function () {
        window.__atfValidationTableLayoutFrame = null;
        pendingRoots.forEach(root => {
          for (var parent = root.parentElement; parent; parent = parent.parentElement)
            if (pendingRoots.has(parent)) return;
          evidenceTablesFromMutation({target: root}).forEach(table => pendingTables.add(table));
        });
        pendingRoots.clear();
        var current = Array.from(pendingTables);
        pendingTables.clear();
        observeVisibleEvidenceTables(current);
        normalizeValidationTableColumns(current);
      };
      window.__atfValidationTableLayoutFrame = window.requestAnimationFrame
        ? window.requestAnimationFrame(flush)
        : setTimeout(flush, 0);
  };
  var initialTables = Array.from(document.querySelectorAll(ATF_EVIDENCE_TABLE_SELECTOR));
  if (window.ResizeObserver) {
    window.__atfValidationTableResizeObserver = new ResizeObserver(function (entries) {
      var tables = Array.from(entries, entry => entry.target);
      schedule(tables);
    });
    observeVisibleEvidenceTables(initialTables);
  } else {
    window.addEventListener('resize', function () { schedule(); });
  }
  if (window.MutationObserver && document.body) {
    new MutationObserver(function (records) {
      var contentTargets = new Set();
      records.forEach(record => {
        var target = record.target.nodeType === 1 ? record.target : record.target.parentElement;
        if (record.type === 'attributes' && record.oldValue === target.getAttribute(record.attributeName)) return;
        if (target) contentTargets.add(target);
        Array.from(record.removedNodes || []).forEach(node => {
          if (node.nodeType !== 1 || node.isConnected || !window.__atfValidationTableResizeObserver) return;
          evidenceTableCandidates(node).forEach(table => window.__atfValidationTableResizeObserver.unobserve(table));
        });
        if (target) {
          var root = record.type === 'attributes' ? target : target.closest(ATF_EVIDENCE_TABLE_SELECTOR);
          if (root) pendingRoots.add(root);
        }
        Array.from(record.addedNodes || []).forEach(node => {
          if (node.nodeType === 1) pendingRoots.add(node);
        });
      });
      contentTargets.forEach(invalidateEvidenceTableContent);
      if (pendingRoots.size) schedule([]);
    }).observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      characterData: true,
      attributeOldValue: true,
      attributeFilter: ['open', 'class']
    });
  }
}
setTimeout(bindValidationTableLayout, 0);

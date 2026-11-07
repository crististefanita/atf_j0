function statusDisplay(value) {
  var key = String(value || '').trim(),
    map = {
      200: '200 OK',
      201: '201 Created',
      202: '202 Accepted',
      204: '204 No Content',
      400: '400 Bad Request',
      401: '401 Unauthorized',
      403: '403 Forbidden',
      404: '404 Not Found',
      409: '409 Conflict',
      500: '500 Internal Server Error'
    };
  return map[key] || key;
}
function failureCells(row) {
  return Array.from(row.cells || [], cell => text(cell.querySelector('.atf-ui-value pre') || cell));
}
function failedAssertionRow(row) {
  var status = row.dataset.atfAssertionStatus;
  if (status === 'pass' || status === 'fail') return status === 'fail';
  var result = row.cells && row.cells.length >= 4 && row.cells[row.cells.length - 1];
  return !!result && (text(result).indexOf('❌') >= 0 ||
    !!result.querySelector('.badge-danger,.atf-ui-action-result-fail'));
}
function failedSetupEvidence(card) {
  var zone = card.querySelector('.atf-setup-evidence[data-atf-evidence-status=fail]');
  if (!zone) return null;
  var row = q('tbody tr', zone).find(failedAssertionRow);
  if (!row) return null;
  var cells = failureCells(row);
  return {
    ui: true, step: 'Setup', validation: 'Setup evidence',
    assertion: 'Setup check · ' + (cells[0] || 'unknown') + ' → ' + (cells[1] || '') + ' → ' + (cells[2] || ''),
    message: 'Setup check failed: ' + (cells[0] || 'unknown') + ' expected ' + (cells[1] || '') + ', actual ' + (cells[2] || '')
  };
}
function failedUiEvidence(card) {
  var zones = q(
    'details[data-atf-evidence-status=fail], .atf-ui-assertions[data-atf-evidence-status=fail], details.step-detail.assertion-evidence',
    card
  );
  for (var zone of zones) {
    var owner = zone.closest('[data-atf-evidence-owner]');
    if (zone.closest('.card') !== card || (owner && owner.dataset.atfEvidenceOwner === 'api')) continue;
    var row = q('tbody tr', zone).find(failedAssertionRow);
    if (!row) continue;
    return uiFailureRowContext(row, zone);
  }
  return null;
}
function uiFailureRowContext(row, zone) {
  var cells = failureCells(row), assertion = zone.classList.contains('atf-ui-assertions') || zone.classList.contains('assertion-evidence'),
    label = zone.classList.contains('atf-ui-observations') ? 'Audit observation' : assertion ? 'UI assertion' : 'UI action';
  var title = nodeTitle(row.closest('.card')), observed = cells[assertion ? 2 : 3],
    terminal = !assertion && title.toLowerCase() === 'failure evidence';
  return {
    ui: true, step: label, validation: title,
    assertion: label + ' · ' + (cells[0] || 'unknown') + ' → ' +
      (cells[1] || '') + ' → ' + (cells[2] || '') + ' → ' + (cells[3] || ''),
    message: terminal && observed ? observed : label + ' failed: ' + (observed || 'see the concrete evidence row')
  };
}
function failedUiRows(item) {
  return q('.atf-ui-action-table tbody tr', item).filter(failedAssertionRow);
}
function failedAssertionRows(item) {
  // Derived summaries never participate in the original evidence ordinal.
  return q('.helm-validation-table tbody tr:not(.atf-ui-failure-evidence *)', item).filter(failedAssertionRow);
}
function failedDiagnosticRows(item) {
  return q('.phase-diagnostics [data-atf-diagnostic-status=fail] tbody tr', item);
}
function failedExecutionEvidence(item) {
  return q('[data-atf-terminal-failure=true]', item).filter(function (evidence) {
    var request = ownerCard(evidence, 'request');
    return request && statusOf(request) === 'fail';
  });
}

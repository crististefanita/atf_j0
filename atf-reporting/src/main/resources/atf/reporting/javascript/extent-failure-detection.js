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
  return Array.from(row.cells || [], cell => text(cell, ''));
}
function failedAssertionRow(row) {
  var status = row.getAttribute('data-atf-assertion-status');
  if (status === 'pass' || status === 'fail') return status === 'fail';
  var result = row.cells && row.cells.length >= 4 && row.cells[row.cells.length - 1];
  return !!result && (text(result, '').indexOf('❌') >= 0 ||
    !!result.querySelector('.badge-danger,.atf-ui-action-result-fail'));
}
function failedSetupEvidence(card) {
  var zone = card.querySelector('.atf-setup-evidence[data-atf-evidence-status="fail"]');
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
    'details[data-atf-evidence-status="fail"], .atf-ui-assertions[data-atf-evidence-status="fail"], details.step-detail.assertion-evidence',
    card
  );
  for (var zone of zones) {
    var owner = zone.closest('[data-atf-evidence-owner]');
    if (zone.closest('.card') !== card || (owner && owner.getAttribute('data-atf-evidence-owner') === 'api')) continue;
    var row = q('tbody tr', zone).find(failedAssertionRow);
    if (!row) continue;
    var cells = failureCells(row);
    var assertion = zone.classList.contains('atf-ui-assertions') || zone.classList.contains('assertion-evidence');
    var label = zone.classList.contains('atf-ui-observations') ? 'Audit observation' :
      assertion ? 'UI assertion' : 'Playwright action';
    return {
      ui: true, step: label, validation: text(card, '.card-title a.node'),
      assertion: label + ' · ' + (cells[0] || 'unknown') + ' → ' +
        (cells[1] || '') + ' → ' + (cells[2] || '') + ' → ' + (cells[3] || ''),
      message: label + ' failed: ' + (cells[3] || 'see the concrete evidence row')
    };
  }
  return null;
}
function failureEvidenceCandidate(card) {
  var title = text(card, '.card-title a.node');
  if (title.toLowerCase() !== 'failure evidence') return null;
  var actions = card.querySelector('.atf-ui-action-sequence[data-atf-evidence-status="fail"]');
  var assertions = card.querySelector('.atf-ui-assertions[data-atf-evidence-status="fail"]');
  var action = actions && q('tbody tr', actions).find(failedAssertionRow);
  var row = action || (assertions && q('tbody tr', assertions).find(failedAssertionRow));
  if (!row) return null;
  var cells = failureCells(row);
  var observed = cells[3] || 'see the concrete action evidence';
  return {
    ui: true, step: 'Failure evidence', validation: title,
    assertion: action ?
      'Playwright action · ' + (cells[0] || 'unknown') + ' → ' + (cells[1] || '') + ' → ' + observed :
      'Failure cause · ' + (cells[0] || 'unknown') + ' → ' + (cells[1] || '') + ' → ' + (cells[2] || '') + ' → ' + (cells[3] || ''),
    message: 'Failure evidence: ' + (action ? observed : cells[2] || 'see the concrete failure evidence')
  };
}
function failedAssertionRows(item) {
  return q('.helm-validation-table tbody tr', item).filter(failedAssertionRow);
}

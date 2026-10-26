function failureCauseChain(item) {
  var causes = [];
  q('details.phase-diagnostics [data-atf-diagnostic-status="fail"] tbody tr', item).forEach(function (row) {
    var cells = Array.prototype.slice.call(row.cells || []);
    if (cells.length > 1) causes.push(text(cells[0], '') + ': ' + text(cells[1], ''));
  });
  q('.card[data-atf-node-role="phase"]', item).forEach(function (card) {
    if (statusOf(card) !== 'fail') return;
    q('.helm-trace-table tbody tr', card).forEach(function (row) {
      var cells = Array.prototype.slice.call(row.cells || []),
        label = String(text(cells[0], '')).trim().toUpperCase();
      if (label === 'FAILURE CAUSE') causes.push(text(cells[1], ''));
    });
  });
  return causes
    .map(function (cause, index) {
      return (index === 0 ? 'PRIMARY: ' : 'SECONDARY: ') + cause;
    })
    .join(' | ');
}
function scopeFailureContextToPhase(item, context) {
  var phase = q('.card[data-atf-node-role="phase"]', item).find(
    function (card) {
      return statusOf(card) === 'fail';
    }
  );
  if (!phase) return context;
  var requests = q('[data-atf-node-role="request"]', phase),
    validations = q('[data-atf-node-role="validation"]', phase);
  if (!context.ui && requests.length) {
    var request = requests[requests.length - 1];
    context.request = text(request, '.card-title a.node');
    var measured = { message: '', duration: '' };
    failureTransportEvidence(request, measured);
    context.duration = measured.duration || context.duration;
  }
  if (validations.length)
    context.validation = text(
      validations[validations.length - 1],
      '.card-title a.node'
    );
  return context;
}

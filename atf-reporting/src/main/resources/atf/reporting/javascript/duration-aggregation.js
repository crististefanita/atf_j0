function directActionDuration(card) {
  var result = { value: 0, present: false };
  q('.atf-ui-action-table tbody tr', card).forEach(row => {
    if (row.closest('.card[data-atf-node-role],.card[data-atf-scenario-id]') !== card) return;
    var value = atfDurationValue(text(row.querySelector('.atf-ui-action-time'), ''));
    if (value !== null) {
      result.value += value;
      result.present = true;
    }
  });
  return result;
}
function directTransportEvidence(card) {
  var panel = Array.from(card.children).find(child => child.classList.contains('collapse'));
  return !!(panel && panel.querySelector(
    '.execution-table,.helm-trace-table,.helm-request-details-content,.helm-response-details-content'));
}
function durationAttribute(node, name, value) {
  if (node && node.getAttribute(name) !== String(value)) node.setAttribute(name, String(value));
}
function setNodeDuration(card, value, source) {
  if (value === null) return;
  var badge = durationBadge(card), formatted = atfFormatDuration(value);
  if (badge && text(badge, '') !== formatted) badge.textContent = formatted;
  [card, badge].forEach(node => {
    durationAttribute(node, 'data-duration-ms', value);
    durationAttribute(node, 'data-duration-source', source);
  });
  if (source === 'measured') durationAttribute(badge, 'data-atf-measured-duration-ms', value);
}
function aggregateChildDurations(card) {
  var summary = { total: 0, count: 0 };
  directStructuralChildren(card).forEach(child => {
    var value = aggregateNodeDuration(child);
    if (value !== null) { summary.total += value; summary.count++; }
  });
  return summary;
}
function executionTiming(card) {
  var marker = q('[data-atf-execution-duration-ms]', card).find(item => item.closest('.card') === card);
  var row = marker && marker.closest('tr.event-row');
  if (row) {
    var table = row.closest('table');
    card.appendChild(marker);
    row.remove();
    if (table && !table.querySelector('tbody tr')) table.remove();
  }
  return marker;
}
function timingValue(marker, name) { return Number(marker.getAttribute('data-atf-' + name)); }
function durationDecision(card, children) {
  var timing = executionTiming(card), badge = durationBadge(card);
  if (timing) {
    var value = timingValue(timing, 'execution-duration-ms');
    if (Number.isFinite(value) && value >= 0) {
      durationAttribute(badge, 'title', 'Execution of ' + timingValue(timing, 'execution-intervals')
        + ' callbacks; snapshot publication: ' + atfFormatDuration(timingValue(timing, 'publication-duration-ms')));
      return { value: value, source: 'execution' };
    }
  }
  var parent = card.parentElement.closest('.card');
  if (card.getAttribute('data-atf-node-role') === 'assertion-step' && parent && executionTiming(parent)) {
    durationAttribute(badge, 'title', 'Assertion snapshot publication; excluded from phase execution total');
    return { value: measuredNodeDuration(card), source: 'publication' };
  }
  var actions = directActionDuration(card), measured = measuredNodeDuration(card),
    ownAction = actions.present ? actions.value : directTransportEvidence(card) && measured !== null ? measured : 0;
  return children.count ? { value: Math.max(children.total, ownAction), source: 'aggregated' }
    : actions.present ? { value: actions.value, source: 'aggregated' }
    : { value: measured, source: 'measured' };
}
function aggregateNodeDuration(card) {
  var decision = durationDecision(card, aggregateChildDurations(card));
  setNodeDuration(card, decision.value, decision.source);
  return decision.value;
}
function allScenarioCards() {
  return Array.from(new Set(classItems().flatMap(root => directScenarioCards(root))
    .concat(q('#test-view .test-content-detail .test-contents > .mt-4 > .accordion > .card'))));
}
function aggregateVisibleNodeDurations() {
  q('#test-view .test-content-detail .card[data-atf-node-role="phase"]').forEach(aggregateNodeDuration);
}
function aggregateNodeDurations() {
  testItems();
  rememberMeasuredDurations();
  allScenarioCards().forEach(aggregateNodeDuration);
  aggregateVisibleNodeDurations();
}

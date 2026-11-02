function directActionDuration(card) {
  var result = null;
  q('.atf-ui-action-table tbody tr', card).forEach(row => {
    if (row.closest('.card[data-atf-node-role],.card[data-atf-scenario-id]') !== card) return;
    var value = atfDurationValue(text(row.querySelector('.atf-ui-action-time'), ''));
    if (value !== null) result = (result || 0) + value;
  });
  return result;
}
function directTransportEvidence(card) {
  var panel = Array.from(card.children).find(child => child.matches('.collapse'));
  return !!(panel && panel.querySelector(
    '.execution-table,.helm-trace-table,.helm-request-details-content,.helm-response-details-content'));
}
function durationAttr(node, name, value) {
  if (node && node.getAttribute(name) !== String(value)) node.setAttribute(name, String(value));
}
function setNodeDuration(card, value, source) {
  if (value === null) return;
  var badge = durationBadge(card);
  publishDuration(badge, value, source);
  markDuration(card, value, source);
  durationAttr(badge, "aria-label", source + " " + atfFormatDuration(value));
  if (source === 'measured') durationAttr(badge, 'data-atf-measured-duration-ms', value);
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
function durationDecision(card, children) {
  var timing = executionTiming(card), badge = durationBadge(card);
  if (timing) {
    var value = Number(timing.getAttribute('data-atf-execution-duration-ms'));
    if (Number.isFinite(value) && value >= 0) {
      durationAttr(badge, 'title', timing.getAttribute('title'));
      return { value: value, source: 'execution' };
    }
  }
  var phase = card.parentElement.closest('[data-atf-node-role=phase]');
  if (phase && executionTiming(phase)) {
    var transport = q("[data-atf-transport-duration-ms]", card)
      .filter(e => e.closest(".card") === card).map(e => Number(e.getAttribute("data-atf-transport-duration-ms")))
      .filter(v => Number.isFinite(v) && v >= 0), actions = directActionDuration(card),
      source = actions !== null ? "actions" : transport.length ? "transport" : "publication";
    durationAttr(badge, "title", source === "publication" ? "Publication; excluded from execution" : "Captured " + source + "; phase execution unchanged");
    return { value: actions !== null ? actions : transport.length ? transport.reduce((sum, value) => sum + value, 0) : measuredNodeDuration(card), source: source };
  }
  var actions = directActionDuration(card), measured = measuredNodeDuration(card),
    ownAction = actions !== null ? actions : directTransportEvidence(card) && measured !== null ? measured : 0;
  return children.count ? { value: Math.max(children.total, ownAction), source: 'aggregated' }
    : actions !== null ? { value: actions, source: 'aggregated' }
    : { value: measured, source: 'measured' };
}
function aggregateNodeDuration(card) {
  var decision = durationDecision(card, aggregateChildDurations(card));
  setNodeDuration(card, decision.value, decision.source);
  return decision.value;
}
function allScenarioCards() {
  return Array.from(new Set(classItems().flatMap(directScenarioCards)
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

function normalizeCodeBlocks() {
  q('textarea.code-block').forEach(function (area) {
    var pre = document.createElement('pre');
    pre.className = 'code-block';
    pre.textContent = area.value;
    area.parentNode.replaceChild(pre, area);
  });
}
function disclosurePanel(card) {
  if (card.getAttribute('data-atf-node-role') === 'phase' ||
      card.hasAttribute('data-atf-scenario-id'))
    return card.querySelector(':scope>.accordion');
  var header = card.querySelector(':scope>.card-header');
  for (var i = 0; i < card.children.length; i++) {
    if (card.children[i] !== header) return card.children[i];
  }
  return null;
}
function disclosureHasContent(panel) {
  return !!panel && (panel.children.length > 0 ||
    String(panel.textContent || '').trim().length > 0);
}
function setDisclosureAvailability(card, header, expandable) {
  card.setAttribute('data-atf-node-expandable', String(expandable));
  if (expandable || !header) return;
  header.removeAttribute('role');
  header.removeAttribute('tabindex');
  header.removeAttribute('aria-expanded');
}
function normalizeDisclosureState() {
  q(
    '.test-view .test-content-detail .card[data-atf-scenario-id],.test-view .test-content-detail .card[data-atf-node-role]'
  ).forEach(function (card) {
    var header = card.querySelector(':scope>.card-header'),
      panel = disclosurePanel(card),
      expandable = disclosureHasContent(panel);
    if (!header) return;
    setDisclosureAvailability(card, header, expandable);
    if (!expandable) return;
    var open =
      card.getAttribute('data-atf-scenario-open') === 'true' ||
      card.getAttribute('data-atf-phase-open') === 'true' ||
      card.getAttribute('data-atf-node-open') === 'true' ||
      !!(panel && panel.classList.contains('show'));
    header.setAttribute('role', 'button');
    header.setAttribute('tabindex', '0');
    header.setAttribute('aria-expanded', String(open));
  });
  if (window.__atfDisclosureStateBound) return;
  window.__atfDisclosureStateBound = true;
  ['click', 'keydown'].forEach(function (event) {
    document.addEventListener(event, function () {
      setTimeout(normalizeDisclosureState, 0);
    }, true);
  });
}
setTimeout(normalizeDisclosureState, 0);
function captureDomContract() {
  assignNodeRoles();
  var requests = q('[data-atf-node-role="request"]'),
    validations = q('[data-atf-node-role="validation"]');
  var violations = validations.filter(function (v) {
    return !v.closest('[data-atf-node-role="request"]');
  }).length;
  window.__atfDomContract = {
    tests: testItems().length,
    phases: q('[data-atf-node-role="phase"]').length,
    requests: requests.length,
    validations: validations.length,
    traceTables: q('[data-atf-evidence-kind="trace"] .helm-trace-table').length,
    jsonBlocks: q('[data-atf-evidence-kind="json"] pre.code-block').length,
    assertionTables: q('.helm-validation-table').length,
    categoryLinks: q('.category-view a[data-atf-test-id]').length,
    violations: violations,
    openEvidence: q('.collapse.show').length
  };
}

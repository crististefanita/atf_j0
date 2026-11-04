function failureIndexField(label, value, extra, limit) {
  value = String(value || '');
  if (limit && value.length > limit) value = value.slice(0, limit - 1) + '…';
  return '<div class="atf-failure-index-row' + (extra ? ' ' + extra : '') +
    '"><span class="atf-failure-label">' + esc(label) + '</span><span class="atf-failure-value">' +
    esc(value) + '</span></div>';
}
function failureLink(id, scenario, ordinal, kind) {
  return '#' + [['test-id', id], ['scenario-index', scenario], ['failure-ordinal', ordinal], ['failure-kind', kind]]
    .map(([key, value]) => key + '=' + encodeURIComponent(value)).join('&');
}
function failureRecordId(id, scenario, ordinal, kind) {
  return 'failure-' + id + '-' + scenario + '-' + (kind === 'assertion' ? '' : kind + '-') + ordinal;
}
function failureIndexMarkup(name, ctx, id, scenario, ordinal, kind) {
  var step = ctx.ui || ctx.evidenceOwner === 'component' || !ctx.request,
    fields = [['TEST', ctx.owner || 'API tests'], ['PHASE', ctx.phase]];
  if (ctx.checkpoint) fields.push(['CHECKPOINT', ctx.checkpoint]);
  fields.push([step ? 'STEP' : 'REQUEST', step ? ctx.step : ctx.request]);
  if (!step) fields.push(['DURATION', ctx.duration || 'No response duration recorded']);
  fields.push([ctx.diagnostic ? 'EVIDENCE' : 'VALIDATION', ctx.validation],
    [ctx.diagnostic ? 'DIAGNOSTIC' : 'ASSERTION', ctx.diagnostic || ctx.assertion],
    ['CAUSE', ctx.message, 'atf-failure-index-cause', ctx.previewLimit || 240]);
  return '<div class="atf-failure-index atf-failure-evidence" data-atf-evidence-owner="' +
    esc(ctx.evidenceOwner || (ctx.ui ? 'ui' : step ? 'execution' : 'api')) +
    '" data-atf-failure-id="' + esc(failureRecordId(id, scenario, ordinal, kind)) +
    '"><h4 class="atf-failure-index-title">' + esc(name) + '</h4>' +
    fields.map(field => failureIndexField(...field)).join('') +
    '<a class="atf-failure-open" href="' + esc(failureLink(id, scenario, ordinal, kind)) +
    '" title="Open exact evidence in Tests" data-atf-test-id="' + esc(id) +
    '" data-atf-scenario-index="' + esc(scenario) + '" data-atf-failure-ordinal="' + esc(ordinal) +
    '" data-atf-failure-kind="' + esc(kind) + '">Open in Tests</a></div>';
}
function selectFailure(index) {
  var rows = q('#atf-failure-list>.test-item'), row = rows[index],
    detail = document.getElementById('atf-failure-detail');
  if (!row || !detail) return;
  rows.forEach(function (item, i) { item.classList.toggle('active', i === index); });
  detail.innerHTML = row.getAttribute('data-atf-index-html') || '';
}
function failureRecords() {
  return testItems().filter(item => statusOf(item) === 'fail').flatMap(item => {
    var records = [
      ['ui', failedUiRows(item)], ['assertion', failedAssertionRows(item)], ['diagnostic', failedDiagnosticRows(item)],
      ['execution', failedExecutionEvidence(item)]
    ].flatMap(([kind, rows]) => rows.map((assertionRow, ordinal) => ({ item, assertionRow, ordinal: ordinal + 1, kind })));
    return records.length ? records : [{ item, assertionRow: null, ordinal: 1, kind: 'scenario' }];
  });
}
function failurePhaseRank(phase) {
  return ({ SETUP: 0, 'TEST BODY': 1, TEARDOWN: 2 })[phase] ?? 99;
}
function sortFailureRecords(records) {
  return records.sort(function (a, b) {
    return (
      failurePhaseRank(failureContextFor(a.item, a.assertionRow).phase) -
      failurePhaseRank(failureContextFor(b.item, b.assertionRow).phase)
    );
  });
}
function resetFailureProjection() {
  var host = document.getElementById('atf-failure-list'),
    detail = document.getElementById('atf-failure-detail');
  if (host) host.innerHTML = '';
  if (detail) detail.innerHTML = '';
  return host;
}
function failureRow(record, index) {
  var item = record.item,
    id =
      item.getAttribute('data-atf-class-root-id') ||
      item.getAttribute('test-id'),
    scenarioIndex = item.getAttribute('data-atf-scenario-index') || '0',
    name = testName(item),
    owner = testClass(item),
    ctx = failureContextFor(item, record.assertionRow);
  ctx.owner = owner;
  var row = document.createElement('div');
  row.className = 'test-item atf-failure fail-row';
  row.setAttribute('status', 'fail');
  row.setAttribute('data-atf-test-id', id);
  row.setAttribute('data-atf-scenario-index', scenarioIndex);
  row.setAttribute('data-atf-failure-ordinal', String(record.ordinal));
  row.setAttribute('data-atf-failure-kind', record.kind);
  row.setAttribute(
    'data-atf-failure-id',
    failureRecordId(id, scenarioIndex, record.ordinal, record.kind)
  );
  row.setAttribute('data-atf-failure-phase', ctx.phase);
  row.setAttribute(
    'data-atf-index-html',
    failureIndexMarkup(name, ctx, id, scenarioIndex, record.ordinal, record.kind)
  );
  row.innerHTML =
    '<div class="test-detail"><p class="name">' +
    esc(name) +
    '</p><p class="text-sm">' +
    esc(owner) +
    '</p></div><div class="test-status"><span class="badge log badge-danger status status-fail">Fail</span></div>';
  row.addEventListener('click', () => selectFailure(index));
  return row;
}
function renderFails() {
  var host = document.getElementById('atf-failure-list');
  if (!host) return;
  var records = sortFailureRecords(failureRecords());
  resetFailureProjection();
  if (!records.length) {
    host.innerHTML = '<div class="atf-fails-empty empty atf-empty-state">No failed tests.</div>';
    return;
  }
  records.forEach(function (record, index) {
    host.appendChild(failureRow(record, index));
  });
  selectFailure(0);
}

function failureIndexField(label, value, extra, limit) {
  value = String(value || '');
  if (limit && value.length > limit) value = value.slice(0, limit - 1) + '…';
  return '<div class="atf-failure-index-row' + (extra ? ' ' + extra : '') +
    '"><span class="atf-failure-label">' + esc(label) + '</span><span class="atf-failure-value">' +
    esc(value) + '</span></div>';
}
function failureIndexMarkup(name, ctx, id, scenarioIndex, ordinal) {
  var safeId = esc(id),
    safeScenario = esc(scenarioIndex),
    stepEvidence = ctx.ui || ctx.evidenceOwner === 'component' || !ctx.request;
  return (
    '<div class="atf-failure-index atf-failure-evidence" data-atf-evidence-owner="' +
    esc(ctx.evidenceOwner || (ctx.ui ? 'ui' : stepEvidence ? 'execution' : 'api')) +
    '" data-atf-failure-id="failure-' +
    safeId +
    '-' +
    safeScenario +
    '-' +
    esc(ordinal || 1) +
    '"><h4 class="atf-failure-index-title">' +
    esc(name) +
    '</h4>' +
    failureIndexField('TEST', ctx.owner || 'API tests') +
    failureIndexField('PHASE', ctx.phase) +
    (ctx.checkpoint ? failureIndexField('CHECKPOINT', ctx.checkpoint) : '') +
    failureIndexField(stepEvidence ? 'STEP' : 'REQUEST', stepEvidence ? ctx.step : ctx.request) +
    (stepEvidence ? '' : failureIndexField('DURATION', ctx.duration || 'No response duration recorded')) +
    failureIndexField('VALIDATION', ctx.validation) +
    failureIndexField('ASSERTION', ctx.assertion) +
    failureIndexField('CAUSE', ctx.message, 'atf-failure-index-cause', ctx.previewLimit || 240) +
    '<a class="atf-failure-open" href="#test-id=' +
    safeId +
    '&amp;scenario-index=' +
    safeScenario +
    '&amp;failure-ordinal=' + esc(ordinal || 1) +
    '" title="Open the exact failing test in the Tests tab" data-atf-test-id="' +
    safeId +
    '" data-atf-scenario-index="' +
    safeScenario +
    '" data-atf-failure-ordinal="' +
    esc(ordinal || 1) +
    "\" onclick=\"if(typeof atfOpenTest==='function'){try{atfOpenTest('" +
    safeId +
    "','" +
    safeScenario +
    "',undefined,'" + esc(ordinal || 1) + "');return false;}catch(error){}}window.location.hash='test-id='+encodeURIComponent('" +
    safeId +
    "')+'&scenario-index='+encodeURIComponent('" +
    safeScenario +
    "')+'&failure-ordinal=" + esc(ordinal || 1) + "';return false;\">Open in Tests</a></div>"
  );
}
function selectFailure(index) {
  var rows = q('#atf-failure-list>.test-item'),
    row = rows[index],
    detail = document.getElementById('atf-failure-detail');
  if (!row || !detail) return;
  for (var i = 0; i < rows.length; i++)
    rows[i].classList.toggle('active', i === index);
  var id = row.getAttribute('data-atf-test-id'),
    scenarioIndex = row.getAttribute('data-atf-scenario-index'),
    failureOrdinal = row.getAttribute('data-atf-failure-ordinal');
  detail.innerHTML = '';
  var indexMarkup = row.getAttribute('data-atf-index-html') || '';
  if (indexMarkup) detail.insertAdjacentHTML('beforeend', indexMarkup);
  var open = detail.querySelector('.atf-failure-open');
  if (open)
    open.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      if (typeof atfOpenTest === 'function') {
        try {
          atfOpenTest(id, scenarioIndex, undefined, failureOrdinal);
          return false;
        } catch (error) {}
      }
      window.location.hash =
        'test-id=' +
        encodeURIComponent(id) +
        '&scenario-index=' +
        encodeURIComponent(scenarioIndex) + '&failure-ordinal=' + encodeURIComponent(failureOrdinal);
      return false;
    });
}
function failureRecords() {
  var records = [];
  testItems()
    .filter(function (item) {
      return statusOf(item) === 'fail';
    })
    .forEach(function (item) {
      var failedRows = failedAssertionRows(item);
      if (!failedRows.length) failedRows = [null];
      failedRows.forEach(function (assertionRow, ordinal) {
        records.push({
          item: item,
          assertionRow: assertionRow,
          ordinal: ordinal + 1
        });
      });
    });
  return records;
}
function failurePhaseRank(phase) {
  var order = { SETUP: 0, 'TEST BODY': 1, TEARDOWN: 2 },
    rank = order[phase];
  return rank === undefined ? 99 : rank;
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
  row.setAttribute(
    'data-atf-failure-id',
    'failure-' + id + '-' + scenarioIndex + '-' + record.ordinal
  );
  row.setAttribute('data-atf-failure-phase', ctx.phase);
  row.setAttribute(
    'data-atf-index-html',
    failureIndexMarkup(name, ctx, id, scenarioIndex, record.ordinal)
  );
  row.innerHTML =
    '<div class="test-detail"><p class="name">' +
    esc(name) +
    '</p><p class="text-sm">' +
    esc(owner) +
    '</p></div><div class="test-status"><span class="badge log badge-danger status status-fail">Fail</span></div>';
  row.addEventListener('click', function () {
    selectFailure(index);
  });
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

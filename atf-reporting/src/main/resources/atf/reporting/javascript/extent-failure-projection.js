function failureIndexMarkup(name, ctx, id, scenarioIndex, ordinal) {
  var safeId = esc(id),
    safeScenario = esc(scenarioIndex),
    stepEvidence = ctx.ui || ctx.evidenceOwner === 'component';
  return (
    '<div class="atf-failure-index atf-failure-evidence" data-atf-evidence-owner="' +
    esc(ctx.evidenceOwner || (ctx.ui ? 'ui' : 'api')) +
    '" data-atf-failure-id="failure-' +
    safeId +
    '-' +
    safeScenario +
    '-' +
    esc(ordinal || 1) +
    '"><h4 class="atf-failure-index-title">' +
    esc(name) +
    '</h4>' +
    '<div class="atf-failure-index-row"><span class="atf-failure-label">TEST</span><span class="atf-failure-value">' +
    esc(ctx.owner || 'API tests') +
    '</span></div>' +
    '<div class="atf-failure-index-row"><span class="atf-failure-label">PHASE</span><span class="atf-failure-value">' +
    esc(ctx.phase) +
    '</span></div>' +
    (ctx.checkpoint
      ? '<div class="atf-failure-index-row"><span class="atf-failure-label">CHECKPOINT</span><span class="atf-failure-value">' +
        esc(ctx.checkpoint) + '</span></div>'
      : '') +
    '<div class="atf-failure-index-row"><span class="atf-failure-label">' +
    (stepEvidence ? 'STEP' : 'REQUEST') +
    '</span><span class="atf-failure-value">' +
    esc(stepEvidence ? ctx.step : ctx.request) +
    '</span></div>' +
    (stepEvidence
      ? ''
      : '<div class="atf-failure-index-row"><span class="atf-failure-label">DURATION</span><span class="atf-failure-value">' +
        esc(ctx.duration || 'No response duration recorded') +
        '</span></div>') +
    '<div class="atf-failure-index-row"><span class="atf-failure-label">VALIDATION</span><span class="atf-failure-value">' +
    esc(ctx.validation) +
    '</span></div>' +
    '<div class="atf-failure-index-row"><span class="atf-failure-label">ASSERTION</span><span class="atf-failure-value">' +
    esc(ctx.assertion) +
    '</span></div>' +
    '<div class="atf-failure-index-row atf-failure-index-cause"><span class="atf-failure-label">CAUSE</span><span class="atf-failure-value">' +
    esc(ctx.message) +
    '</span></div><a class="atf-failure-open" href="#test-id=' +
    safeId +
    '&amp;scenario-index=' +
    safeScenario +
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
    "');return false;}catch(error){}}window.location.hash='test-id='+encodeURIComponent('" +
    safeId +
    "')+'&scenario-index='+encodeURIComponent('" +
    safeScenario +
    '\');return false;">Open in Tests</a></div>'
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
    scenarioIndex = row.getAttribute('data-atf-scenario-index');
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
          atfOpenTest(id, scenarioIndex);
          return false;
        } catch (error) {}
      }
      window.location.hash =
        'test-id=' +
        encodeURIComponent(id) +
        '&scenario-index=' +
        encodeURIComponent(scenarioIndex);
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

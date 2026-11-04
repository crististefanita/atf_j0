function uiFailureSource(card) {
  var action = card.querySelector('details.atf-ui-action-sequence[data-atf-evidence-status="fail"]'),
    observation = card.querySelector('details.atf-ui-observations[data-atf-evidence-status="fail"]'),
    assertion = card.querySelector('details.step-detail.assertion-evidence .helm-validation-table, .atf-ui-assertions[data-atf-evidence-status="fail"] .helm-validation-table'),
    source = action || observation || assertion;
  if (!source) return null;
  var failed = source.querySelector('.atf-ui-action-result-fail'),
    row = failed ? failed.closest('tr') : source.tBodies?.[0]?.rows[0],
    cells = row?.cells || [],
    assertionOnly = !action && !observation;
  if (assertionOnly && (!row || !failedAssertionRow(row))) return null;
  return {
    kind: action ? 'UI action' : observation ? 'Audit observation' : 'Assertion',
    target: failed ? text(cells[1]) : assertionOnly ? text(cells[0]) : '',
    expected: text(cells[failed ? 2 : 1]),
    observed: text(cells[failed ? 3 : 2]),
    result: failed ? '' : text(cells[3])
  };
}
function uiFailureTable(headers, rows, kind) {
  return '<table class="helm-evidence-table ' + kind + '"><thead><tr>' + headers + '</tr></thead><tbody>' +
    rows.map(row => '<tr>' + row.map(value => '<td>' + esc(value) + '</td>').join('') + '</tr>').join('') + '</tbody></table>';
}
function assertionFailureTable(failure) {
  return uiFailureTable('<th>ASSERTION</th><th>EXPECTED</th><th>ACTUAL</th><th>RESULT</th>',
    [[failure.target || 'Assertion', failure.expected, failure.observed, failure.result || '❌']], 'helm-validation-table');
}
function appendUiFailureEvidence(card) {
  var table = directEvidenceTable(card), failure = uiFailureSource(card);
  if (!table || !failure || !table.tBodies.length ||
      table.querySelector(':scope>tbody>tr.atf-ui-failure-evidence-row')) return;
  var assertion = failure.kind === 'Assertion',
    label = '❌ ' + failure.kind + ' failed' + (assertion || !failure.target ? '' : ' · ' + failure.target),
    fields = [['TYPE', failure.kind], ['TARGET', failure.target], ['EXPECTED', failure.expected],
      ['OBSERVED', failure.observed || 'failure was reported without an observed value']],
    markup = assertion ? assertionFailureTable(failure) :
      uiFailureTable('<th>FIELD</th><th>VALUE</th>', fields.filter(field => field[1]), 'atf-ui-failure-table'),
    row = document.createElement('tr');
  row.className = 'event-row atf-ui-failure-evidence-row';
  row.innerHTML = '<td><span class="badge log fail-bg">Fail</span></td><td></td><td>' +
    '<details class="step-detail atf-ui-failure-evidence' + (assertion ? ' assertion-evidence' : '') + '" data-atf-evidence-status="fail">' +
    '<summary aria-expanded="false" aria-label="' + esc(label) + '">' + disclosureSummary(label) + '</summary><div class="body">' + markup + '</div></details></td>';
  table.tBodies[0].appendChild(row);
}
function markLastSemanticEvidence() {
  q('[data-atf-node-role="request"],[data-atf-node-role="phase"]').forEach(
    function (card) {
      var table = directEvidenceTable(card);
      if (!table) return;
      var details = q(
        ':scope>tbody>tr.event-row>td:last-child>details.step-detail',
        table
      );
      details.forEach(function (item) {
        item.removeAttribute('data-atf-evidence-last');
      });
      if (details.length)
        details[details.length - 1].setAttribute(
          'data-atf-evidence-last',
          'true'
        );
    }
  );
}

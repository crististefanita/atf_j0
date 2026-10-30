function failureTransportEvidence(card, state) {
  ['.helm-trace-table', '.execution-table'].forEach(function (selector) {
    var table = card.querySelector(selector);
    if (!table) return;
    q('tbody tr', table).forEach(function (row) {
      var label = text(row.cells[0], '').toUpperCase(),
        rawDuration = text(row.cells[1], '');
      if (label === 'FAILURE CAUSE') state.message = rawDuration;
      if (['RESPONSE DURATION', 'RESPONSE DURATION (MS)', 'RESPONSE.DURATION.MS'].includes(label) && !state.duration) {
        var durationMs = atfDurationValue(rawDuration);
        state.duration = durationMs === null ? rawDuration : atfFormatDuration(durationMs);
      }
    });
  });
}
function failureValidationCandidate(card) {
  var table = card.querySelector('.helm-validation-table');
  if (!table) return null;
  var rows = table.tBodies[0] ? Array.from(table.tBodies[0].rows) : [],
    row = rows.find(failedAssertionRow);
  if (!row) return null;
  var cells = row.cells;
  return {
    card: card,
    label: text(cells[0], ''),
    expected: text(cells[1], ''),
    actual: text(cells[2], ''),
    result: text(cells[3], '')
  };
}
function failureRequestIdentity(card) {
  if (!card) return '';
  var node = q('[data-atf-request-method]', card).find(candidate =>
    candidate.closest('.card[data-atf-node-role="request"]') === card);
  return node ? node.getAttribute('data-atf-request-method') + ' ' +
    node.getAttribute('data-atf-request-path') : '';
}
function collectFailureState(item) {
  var state = {
    phase: '',
    phaseCard: null,
    component: !!item.querySelector('[data-atf-node-role="assertion-step"]'),
    requests: [],
    validations: [],
    uiFailures: [],
    runtimeFailures: [],
    message: '',
    duration: ''
  };
  q('.card', item).forEach(function (card) {
    var title = text(card, '.card-title a.node'),
      upper = title.toUpperCase();
    if (
      !state.phase &&
      /^(SETUP|TEST BODY|TEARDOWN)$/.test(upper) &&
      statusOf(card) === 'fail'
    ) {
      state.phase = title;
      state.phaseCard = card;
    }
    var identity = failureRequestIdentity(card);
    if (identity || /^(GET|POST|PUT|PATCH|DELETE|HEAD) /.test(title))
      state.requests.push({ card: card, title: identity || title });
    var setupFailure = failedSetupEvidence(card),
      uiFailure = failedUiEvidence(card);
    if (setupFailure) state.uiFailures.push(setupFailure);
    if (uiFailure) state.uiFailures.push(uiFailure);
    var runtimeFailure = failureEvidenceCandidate(card);
    if (runtimeFailure) state.runtimeFailures.push(runtimeFailure);
    failureTransportEvidence(card, state);
    var candidate = failureValidationCandidate(card);
    if (candidate) state.validations.push(candidate);
  });
  return state;
}
function failureEntryBelongsToPhase(entry, phaseCard) {
  return !phaseCard || Boolean(
    phaseCard.compareDocumentPosition(entry.card) & Node.DOCUMENT_POSITION_PRECEDING
  );
}
function latestFailureEntry(entries, phaseCard) {
  var scoped = entries.filter(entry => failureEntryBelongsToPhase(entry, phaseCard));
  return scoped.length ? scoped[scoped.length - 1] : entries[0] || null;
}
function failureContextFromState(state) {
  var missing = 'Failure recorded; see exact test for full evidence',
    context = {
      phase: state.phase || 'Unknown phase',
      ui: false,
      request: '',
      duration: state.duration,
      step: 'Execution',
      validation: 'No contract validation recorded',
      assertion: missing,
      message: state.message || missing
    },
    concrete = state.runtimeFailures[0] || state.uiFailures[0];
  if (concrete) {
    return Object.assign(context, concrete, {
      ui: true,
      request: '',
      duration: '',
      step: concrete.step || 'UI step'
    });
  }
  if (state.component && !state.requests.length) context.evidenceOwner = 'component';
  if (state.phase !== 'TEARDOWN') {
    var requestEntry = latestFailureEntry(state.requests, state.phaseCard),
      fallback = latestFailureEntry(state.validations, state.phaseCard);
    context.request = requestEntry ? requestEntry.title : '';
    if (fallback) {
      context.validation = /status/i.test(fallback.label)
        ? 'Validate: status=' + fallback.expected
        : fallback.label;
      var expected = statusDisplay(fallback.expected),
        actual = statusDisplay(fallback.actual);
      context.assertion = fallback.label + ' → ' + expected + ' → ' + actual;
      if (!state.message && ((fallback.result || '').indexOf('❌') >= 0 || state.phase === 'TEST BODY'))
        context.message = fallback.label + ' expected ' + expected + ', actual ' + actual;
    }
  }
  return context;
}
function failureContext(item) {
  var context = failureContextFromState(collectFailureState(item));
  if (typeof scopeFailureContextToPhase === 'function')
    context = scopeFailureContextToPhase(item, context);
  if (typeof failureCauseChain === 'function') {
    var chain = failureCauseChain(item);
    if (chain) context.message = chain;
  }
  return context;
}
function failureContextFor(item, anchor) {
  var ctx = failureContext(item);
  if (!anchor) return ctx;
  ctx.previewLimit = +(anchor.closest('[data-atf-preview-limit]') || anchor).getAttribute('data-atf-preview-limit') || 240;
  var diagnostic = anchor.closest('.phase-diagnostics [data-atf-phase-name]');
  if (diagnostic) return Object.assign(ctx, {
    evidenceOwner: 'execution', ui: false, request: '', duration: '', checkpoint: '',
    phase: diagnostic.getAttribute('data-atf-phase-name'), step: 'Execution',
    validation: 'Execution diagnostics', diagnostic: text(anchor.cells[0], ''),
    assertion: 'No contract comparison recorded', message: text(anchor.cells[1], '')
  });
  if (anchor.matches('[data-atf-terminal-failure="true"]')) {
    var terminalPhase = anchor.closest('.card[data-atf-node-role="phase"]');
    return Object.assign(ctx, {
      evidenceOwner: 'execution', ui: false, checkpoint: '', step: 'Execution',
      phase: terminalPhase ? text(terminalPhase, '.card-title a.node') : ctx.phase,
      request: anchor.getAttribute('data-atf-request-method') + ' ' + anchor.getAttribute('data-atf-request-path'),
      duration: anchor.getAttribute('data-atf-terminal-duration'), validation: 'Execution evidence',
      diagnostic: 'Transport failure', assertion: 'No contract comparison recorded',
      message: anchor.getAttribute('data-atf-terminal-message')
    });
  }
  var cells = Array.from(anchor.cells || [], cell => text(cell, '')),
    label = cells[0] || 'Assertion',
    expected = cells[1] || '',
    actual = cells[2] || '';
  var validation = anchor.closest('.card'),
    component = anchor.closest('.card[data-atf-node-role="assertion-step"]'),
    phase = anchor.closest('.card[data-atf-node-role="phase"]');
  if (component) {
    var title = text(component, '.card-title a.node');
    return Object.assign(ctx, {
      evidenceOwner: 'component', ui: false, request: '', duration: '',
      step: title, validation: title,
      phase: phase ? text(phase, '.card-title a.node') : ctx.phase,
      assertion: label + ' → ' + expected + ' → ' + actual,
      message: label + ' expected ' + expected + ', actual ' + actual
    });
  }
  var evidence = anchor.closest('[data-atf-evidence-owner]'),
    channel = evidence && evidence.getAttribute('data-atf-evidence-owner'),
    requests = [];
  for (var parent = validation && validation.parentElement; parent; parent = parent.parentElement) {
    if (parent.matches('.card[data-atf-node-role="request"]')) requests.push(parent);
  }
  var nearest = requests[0],
    requestTitle = nearest ? text(nearest, '.card-title a.node') : '',
    identity = failureRequestIdentity(nearest),
    httpRequest = !!identity || (channel !== 'api' && /^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s/.test(requestTitle));
  // Explicit typed ownership wins. Method-shaped titles are only a legacy fallback.
  if (channel === 'api' || channel === 'ui' || httpRequest) {
    ctx.ui = channel === 'ui' || (channel !== 'api' && !httpRequest);
    ctx.evidenceOwner = ctx.ui ? 'ui' : 'api';
    ctx.request = ctx.ui ? '' : identity || (httpRequest ? requestTitle : 'API correlation');
    ctx.step = ctx.ui ? 'UI assertion' : '';
    ctx.duration = '';
    var checkpoint = requests.length ? text(requests[requests.length - 1], '.card-title a.node') : '';
    ctx.checkpoint = checkpoint !== requestTitle || !httpRequest ? checkpoint : '';
    if (phase) ctx.phase = text(phase, '.card-title a.node');
    if (!ctx.ui && nearest && httpRequest) {
      var measured = { message: '', duration: '' };
      failureTransportEvidence(nearest, measured);
      ctx.duration = measured.duration;
    }
  }
  if (validation) {
    var title = text(validation, '.card-title a.node');
    if (title) {
      ctx.validation = title;
      if (title.toLowerCase() === 'failure evidence') ctx.step = 'Failure evidence';
    }
  }
  ctx.assertion =
    label + ' → ' + statusDisplay(expected) + ' → ' + statusDisplay(actual);
  var message = label + ' expected ' + statusDisplay(expected) + ', actual ' + statusDisplay(actual),
    primary = ctx.message && ctx.message.indexOf('PRIMARY:') >= 0;
  if (ctx.evidenceOwner === 'api' && primary)
    ctx.message = message + ' | Scenario diagnostics: ' + ctx.message;
  if (ctx.ui || !primary)
    ctx.message = ctx.ui && label === 'Failure cause' ? statusDisplay(actual) : message;
  return ctx;
}

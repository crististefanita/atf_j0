function nodeTitle(card) {
  return text(card, '.card-title a.node');
}
function ownerCard(node, role) {
  return node.closest('.card[data-atf-node-role="' + role + '"]');
}
function failureTransportEvidence(card, state) {
  ['.helm-trace-table', '.execution-table'].forEach(function (selector) {
    var table = card.querySelector(selector);
    if (!table) return;
    q('tbody tr', table).forEach(function (row) {
      var label = text(row.cells[0]).toUpperCase(),
        rawDuration = text(row.cells[1]);
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
  var row = Array.from(table.tBodies[0]?.rows || []).find(failedAssertionRow);
  if (!row) return null;
  var cells = row.cells;
  return {
    card,
    label: text(cells[0]),
    expected: text(cells[1]),
    actual: text(cells[2]),
    result: text(cells[3])
  };
}
function failureRequestIdentity(card) {
  if (!card) return '';
  var node = q('[data-atf-request-method]', card).find(candidate =>
    ownerCard(candidate, 'request') === card);
  return node ? node.dataset.atfRequestMethod + ' ' + node.dataset.atfRequestPath : '';
}
function collectFailureState(item) {
  var state = {
    phase: '',
    phaseCard: null,
    component: !!item.querySelector('[data-atf-node-role=assertion-step]'),
    requests: [],
    validations: [],
    uiFailures: [],
    runtimeFailures: [],
    message: '',
    duration: ''
  };
  q('.card', item).forEach(function (card) {
    var title = nodeTitle(card),
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
    if (uiFailure) {
      state.uiFailures.push(uiFailure);
      if (title.toLowerCase() === 'failure evidence')
        state.runtimeFailures.push(Object.assign({}, uiFailure, {step: 'Failure evidence'}));
    }
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
  return scoped.at(-1) || entries[0] || null;
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
      duration: ''
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
  ctx.previewLimit = +(anchor.closest('[data-atf-preview-limit]') || anchor).dataset.atfPreviewLimit || 240;
  var uiTable = anchor.closest('.atf-ui-action-table'),
    ui = uiTable && uiFailureRowContext(anchor, uiTable.closest('details'));
  var diagnostic = anchor.closest('.phase-diagnostics [data-atf-phase-name]'),
    terminal = anchor.matches('[data-atf-terminal-failure=true]');
  if (diagnostic || terminal)
    Object.assign(ctx, { evidenceOwner: 'execution', ui: false, request: '', duration: '',
      checkpoint: '', step: 'Execution', assertion: 'No contract comparison recorded' });
  if (diagnostic) return Object.assign(ctx, {
    phase: diagnostic.getAttribute('data-atf-phase-name'),
    validation: 'Execution diagnostics', diagnostic: text(anchor.cells[0]),
    message: text(anchor.cells[1])
  });
  if (terminal) {
    var terminalPhase = ownerCard(anchor, 'phase');
    return Object.assign(ctx, {
      phase: terminalPhase ? nodeTitle(terminalPhase) : ctx.phase,
      request: anchor.getAttribute('data-atf-request-method') + ' ' + anchor.getAttribute('data-atf-request-path'),
      duration: anchor.getAttribute('data-atf-terminal-duration'), validation: 'Execution evidence',
      diagnostic: 'Transport failure',
      message: anchor.getAttribute('data-atf-terminal-message')
    });
  }
  var cells = failureCells(anchor),
    label = cells[0] || 'Assertion',
    expected = cells[1] || '',
    actual = cells[2] || '';
  var validation = anchor.closest('.card'),
    component = ownerCard(anchor, 'assertion-step'),
    phase = ownerCard(anchor, 'phase');
  if (component) {
    var title = nodeTitle(component);
    return Object.assign(ctx, {
      evidenceOwner: 'component', ui: false, request: '', duration: '',
      step: title, validation: title,
      phase: phase ? nodeTitle(phase) : ctx.phase,
      assertion: label + ' → ' + expected + ' → ' + actual,
      message: label + ' expected ' + expected + ', actual ' + actual
    });
  }
  var evidence = anchor.closest('[data-atf-evidence-owner]'),
    channel = evidence && evidence.getAttribute('data-atf-evidence-owner'),
    requests = [];
  for (var parent = validation && validation.parentElement; parent; parent = parent.parentElement) {
    if (parent.matches('.card[data-atf-node-role=request]')) requests.push(parent);
  }
  var nearest = requests[0],
    requestTitle = nearest ? nodeTitle(nearest) : '',
    identity = failureRequestIdentity(nearest),
    httpRequest = !!identity || (channel !== 'api' && /^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s/.test(requestTitle));
  // Explicit typed ownership wins. Method-shaped titles are only a legacy fallback.
  if (channel === 'api' || channel === 'ui' || httpRequest) {
    ctx.ui = channel === 'ui' || (channel !== 'api' && !httpRequest);
    ctx.evidenceOwner = ctx.ui ? 'ui' : 'api';
    ctx.request = ctx.ui ? '' : identity || (httpRequest ? requestTitle : 'API correlation');
    ctx.step = ctx.ui ? 'UI assertion' : '';
    ctx.duration = '';
    var checkpoint = requests.length ? nodeTitle(requests[requests.length - 1]) : '';
    ctx.checkpoint = checkpoint !== requestTitle || !httpRequest ? checkpoint : '';
    if (phase) ctx.phase = nodeTitle(phase);
    if (!ctx.ui && nearest && httpRequest) {
      var measured = { message: '', duration: '' };
      failureTransportEvidence(nearest, measured);
      ctx.duration = measured.duration;
    }
  }
  if (validation) {
    var title = nodeTitle(validation);
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
  if (ui) Object.assign(ctx, ui, {
    evidenceOwner: 'ui', request: '', duration: '', diagnostic: ui.assertion,
    phase: phase ? nodeTitle(phase) : ctx.phase
  });
  return ctx;
}

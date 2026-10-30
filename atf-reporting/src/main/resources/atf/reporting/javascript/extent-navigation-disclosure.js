var atfScenarioCards = '.test-content-detail .test-contents > .mt-4 > .accordion > .card';
function openScenarioCard(card) {
  if (!card) return;
  var panel = card.querySelector(':scope>.accordion');
  if (!panel) return;
  collapseDirectScenarioCards();
  card.setAttribute('data-atf-selected', 'true');
  setDisclosureState(card, panel, true, 'data-atf-scenario-open');
  normalizeCodeBlocks();
  assignNodeRoles();
  aggregateClassDurations();
  normalizeEvidenceDisclosures();
  bindPhaseCollapse();
  bindNodeCollapse();
  if (card.scrollIntoView) card.scrollIntoView({ block: 'center' });
}
function setDisclosureState(card, panel, open, attribute) {
  if (!panel) return;
  var state = String(!!open),
    header = card.firstElementChild,
    node = card.querySelector(':scope>.card-header .card-title .node');
  card.setAttribute(attribute, state);
  if (header) header.setAttribute('aria-expanded', state);
  if (node) node.classList.toggle('collapsed', !open);
  var panels = attribute === 'data-atf-node-open' ? nodePanels(card) : [panel];
  panels.forEach(function (child) {
    child.classList.toggle('show', open);
    child.style.setProperty('display', open ? 'block' : 'none', 'important');
  });
}
function bindDisclosureKeyboard(header, toggle) {
  if (!header.hasAttribute('tabindex')) header.setAttribute('tabindex', '0');
  if (!header.hasAttribute('role')) header.setAttribute('role', 'button');
  header.addEventListener('keydown', function (e) {
    if ((e.key === 'Enter' || e.key === ' ') && e.target === header) {
      atfNavEpoch++;
      toggle(e);
    }
  }, true);
}
function openScenarioAuditPath(card) {
  var phase = card && card.querySelector(':scope>.accordion>[data-atf-phase-name="test-body"]'),
    request = phase && phase.querySelector(':scope>.accordion>[data-atf-node-role="request"]'),
    validation = request && request.querySelector(':scope>.accordion>[data-atf-node-role="validation"]');
  if (!validation) return;
  revealScenarioEvidence(card, validation);
  q('details', validation).forEach(function (detail) { detail.open = true; });
}
/** Resolve the same scenario-local ordinal used by Fails, without reconstructing identity from preview text. */
function openScenarioFailurePath(card, ordinal, kind) {
  var index = Number(ordinal) - 1;
  if (!Number.isInteger(index) || index < 0) return;
  var targets = kind === 'execution' ? failedExecutionEvidence(card) :
    kind === 'diagnostic' ? failedDiagnosticRows(card) : failedAssertionRows(card);
  var target = targets[index];
  if (!target) return;
  revealScenarioEvidence(card, target);
}
function revealScenarioEvidence(card, target) {
  var ancestors = [];
  for (var parent = target; parent && parent !== card; parent = parent.parentElement)
    ancestors.push(parent);
  ancestors.reverse().forEach(function (node) {
    if (node.tagName === 'DETAILS') node.open = true;
    if (!node.matches('.card[data-atf-node-role]')) return;
    var phase = node.getAttribute('data-atf-node-role') === 'phase',
      panel = phase ? node.querySelector(':scope>.accordion') : nodePanel(node);
    setDisclosureState(node, panel, true, phase ? 'data-atf-phase-open' : 'data-atf-node-open');
    if (phase) applyPhaseBodyState(node, true);
  });
  normalizeValidationTableColumns();
  if (target.scrollIntoView) target.scrollIntoView({ block: 'center' });
}
function collapseDirectScenarioCards() {
  q(atfScenarioCards).forEach(
    function (card) {
      var panel = card.querySelector(':scope > .accordion');
      if (!panel || card.getAttribute('data-atf-scenario-open') === 'true')
        return;
      setDisclosureState(card, panel, false, 'data-atf-scenario-open');
    }
  );
}
function collapseScenarioCards(force, root, open) {
  var bulk = typeof open === 'boolean', next = !!open;
  q(atfScenarioCards, root).forEach(
    function (scenario) {
      if (bulk) setDisclosureState(scenario, scenario.querySelector(':scope>.accordion'), next, 'data-atf-scenario-open');
      scenario.querySelectorAll(':scope [data-atf-node-role]').forEach(function (card) {
        var role = card.getAttribute('data-atf-node-role'),
          panel = role === 'phase'
            ? card.querySelector(':scope>.accordion')
            : nodePanel(card),
          attribute = role === 'phase'
            ? 'data-atf-phase-open' : 'data-atf-node-open';
        if (!panel) return;
        if (!force && card.getAttribute(attribute) === 'true') return;
        setDisclosureState(card, panel, next, attribute);
        if (role === 'phase') applyPhaseBodyState(card, next);
      });
    }
  );
}
function toggleScenario(card, panel, e) {
  if (e) {
    e.preventDefault();
    e.stopPropagation();
    if (e.stopImmediatePropagation) e.stopImmediatePropagation();
  }
  var startedAt = atfPerformanceStart();
  normalizeScenarioAnnotations();
  var open = card.getAttribute('data-atf-scenario-open') === 'true',
    next = !open;
  atfPerformanceCount('scenarioToggles');
  setDisclosureState(card, panel, next, 'data-atf-scenario-open');
  if (next) {
    assignNodeRoles();
    aggregateClassDurations();
    normalizeEvidenceDisclosures();
    bindPhaseCollapse();
    bindNodeCollapse();
  }
  collapseScenarioCards(true);
  atfPerformanceRecordDisclosureToggle(startedAt);
}
function initializeScenarioCard(card, panel) {
  if (card.getAttribute('data-atf-scenario-open') !== null) return;
  setDisclosureState(card, panel, false, 'data-atf-scenario-open');
}
function bindScenarioCollapse() {
  var controls = '#test-view .test-content-detail :is(.et,.ct)';
  q(controls).forEach(function (control) {
    control.setAttribute('role', 'button');
    control.setAttribute('tabindex', '0');
    control.setAttribute('aria-label', control.getAttribute('title'));
  });
  if (!document.documentElement.hasAttribute('data-atf-bulk-disclosure-bound')) {
    document.documentElement.setAttribute('data-atf-bulk-disclosure-bound', 'true');
    var toggleOwner = function (event) {
      var control = event.target.closest && event.target.closest(controls);
      if (!control || (event.type === 'keydown' &&
          (event.target !== control || (event.key !== 'Enter' && event.key !== ' ')))) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      var owner = control.closest('.test-content-detail'),
        open = control.classList.contains('et');
      collapseScenarioCards(true, owner, open);
    };
    document.addEventListener('click', toggleOwner, true);
    document.addEventListener('keydown', toggleOwner, true);
  }
  q('#test-view .test-list .test-item').forEach(function (item) {
    if (item.getAttribute('data-atf-collapse-bound')) return;
    item.setAttribute('data-atf-collapse-bound', 'true');
    item.addEventListener('click', function () {
      setTimeout(function () {
        normalizeScenarioAnnotations();
        q(atfScenarioCards).forEach(function (card) {
          var panel = card.querySelector(':scope > .accordion');
          if (panel) initializeScenarioCard(card, panel);
        });
        collapseScenarioCards();
      }, 0);
    });
  });
  q(
    '#test-view .test-content-detail .test-contents > .mt-4 > .accordion > .card'
  ).forEach(function (card) {
    var header = card.querySelector(':scope > .card-header'),
      panel = card.querySelector(':scope > .accordion'),
      node = header && header.querySelector(':scope > .card-title .node');
    var expandable = disclosureHasContent(panel);
    setDisclosureAvailability(card, header, expandable);
    if (!header || !expandable) return;
    initializeScenarioCard(card, panel);
    if (header.getAttribute('data-atf-scenario-collapse-bound')) return;
    header.setAttribute('data-atf-scenario-collapse-bound', 'true');
    var toggle = function (e) {
      toggleScenario(card, panel, e);
    };
    if (node && !node.getAttribute('data-atf-scenario-node-collapse-bound')) {
      node.setAttribute('data-atf-scenario-node-collapse-bound', 'true');
      node.addEventListener('click', toggle, true);
    }
    header.addEventListener('click', function (e) {
      if (node && e.target && node.contains(e.target)) return;
      toggle(e);
    });
    bindDisclosureKeyboard(header, toggle);
  });
}
function hasRelevantScenarioNodes(r) {
  var s = '.test-contents > .mt-4 > .accordion > .card,[data-atf-node-role]';
  return r.some(function (x) {
    return [].some.call(x.addedNodes || [], function (n) {
      return n && ((n.matches && n.matches(s)) ||
        (n.querySelector && n.querySelector(s)));
    });
  });
}
function watchScenarioCards() {
  var root = document.getElementById('test-view');
  if (!root || root.getAttribute('data-atf-scenario-watch')) return;
  if (window.MutationObserver) {
    root.setAttribute('data-atf-scenario-watch', 'true');
    var observer = new MutationObserver(function (records) {
      if (hasRelevantScenarioNodes(records)) {
        atfPerformanceCount('relevantMutationBatches');
        bindScenarioCollapse();
        scheduleTestViewNormalization();
      }
    });
    observer.observe(root, { childList: true, subtree: true });
  }
}
function phaseDirectBody(card) {
  return (
    card &&
    (card.querySelector(':scope>.card-body') ||
      card.querySelector(':scope>div:not(.card-header):not(.accordion)'))
  );
}
function applyPhaseBodyState(card, open) {
  var body = phaseDirectBody(card);
  if (body)
    body.style.setProperty('display', open ? 'block' : 'none', 'important');
}
function bindPhaseCollapse() {
  bindNodeCollapse('phase');
}
function nodePanel(card) {
  return nodePanels(card)[0] || null;
}
function nodePanels(card) {
  var header = card.querySelector(':scope>.card-header');
  return [].filter.call(card.children, function (child) {
    return child !== header;
  });
}
function bindNodeCollapse(role) {
  var phase = role === 'phase',
    attribute = phase ? 'data-atf-phase-open' : 'data-atf-node-open',
    bound = phase ? 'data-atf-phase-header-bound' : 'data-atf-node-collapse-bound';
  q(
    phase ? '#test-view .test-content-detail [data-atf-node-role="phase"]' :
      '#test-view .test-content-detail [data-atf-node-role="request"],#test-view .test-content-detail [data-atf-node-role="validation"],#test-view .test-content-detail [data-atf-node-role="assertion-step"]'
  ).forEach(function (card) {
    var header = card.querySelector(':scope>.card-header'),
      panel = phase ? card.querySelector(':scope>.accordion') : nodePanel(card);
    var expandable = disclosureHasContent(panel);
    setDisclosureAvailability(card, header, expandable);
    if (!header || !expandable) return;
    var open = card.getAttribute(attribute) === 'true';
    setDisclosureState(card, panel, open, attribute);
    if (phase) applyPhaseBodyState(card, open);
    if (header.getAttribute(bound)) return;
    header.setAttribute(bound, 'true');
    var toggle = function (e) {
      if (e) {
        e.preventDefault();
        e.stopImmediatePropagation();
        e.stopPropagation();
      }
      var open = card.getAttribute(attribute) === 'true',
        startedAt = atfPerformanceStart(),
        next = !open;
      atfPerformanceCount(phase ? 'phaseToggles' : 'nodeToggles');
      setDisclosureState(card, panel, next, attribute);
      if (phase) applyPhaseBodyState(card, next);
      atfPerformanceRecordDisclosureToggle(startedAt);
    };
    header.addEventListener('click', toggle, true);
    bindDisclosureKeyboard(header, toggle);
  });
}

var atfTestViewNormalizationPending = false;
var atfTestViewReadinessTimers = {};
var atfNavEpoch = 0;
var atfNavigationTargets = {
  'nav-test': 'test-view',
  'nav-category': 'category-view',
  'nav-fails': 'atf-fails-view',
  'nav-overview': 'dashboard-view'
};
document.addEventListener('pointerdown', function (e) {
  if (e.target.closest('#test-view')) atfNavEpoch++;
}, true);
function normalizeTestViewContent() {
  var startedAt = atfPerformanceStart();
  atfPerformanceCount('normalizationRuns');
  try {
  normalizeTestPackageMetadata();
  normalizeScenarioAnnotations();
  bindPhaseCollapse();
  bindNodeCollapse();
  collapseDirectScenarioCards();
  collapseScenarioCards();
  normalizeValidationTableColumns();
  } finally {
    atfPerformanceEnd(startedAt, 'normalizationDurationMs');
  }
  atfPerformanceMarkReady();
}
function scheduleTestViewNormalization() {
  if (atfTestViewNormalizationPending) return;
  atfPerformanceCount('normalizationSchedules');
  atfTestViewNormalizationPending = true;
  var flush = function () {
    atfTestViewNormalizationPending = false;
    normalizeTestViewContent();
  };
  // Cold readiness is structural and must also complete when visual frames are deferred.
  if (atfReportPerformanceState.ready && window.requestAnimationFrame) window.requestAnimationFrame(flush);
  else setTimeout(flush, 0);
}
function scheduleTestViewNormalizationAt(delay) {
  var key = String(delay);
  if (atfTestViewReadinessTimers[key] != null) return;
  atfTestViewReadinessTimers[key] = setTimeout(function () {
    atfTestViewReadinessTimers[key] = null;
    scheduleTestViewNormalization();
  }, delay);
}
window.atfOpenTest = function (id, scenarioIndex, expansion, failureOrdinal, failureKind) {
  var epoch = ++atfNavEpoch;
  var hash = 'test-id=' + id;
  if (scenarioIndex !== undefined) hash += '&scenario-index=' + scenarioIndex;
  if (expansion) hash += '&expand=' + expansion;
  if (failureOrdinal !== undefined) hash += '&failure-ordinal=' + encodeURIComponent(failureOrdinal);
  if (failureKind) hash += '&failure-kind=' + encodeURIComponent(failureKind);
  window.location.hash = hash;
  atfActivate('test-view');
  var item = document.querySelector('.test-item[test-id="' + id + '"]');
  if (item) {
    var detail = item.querySelector('.test-detail');
    if (detail && detail.click) detail.click();
    if (scenarioIndex !== undefined) {
      var index = Number(scenarioIndex);
      setTimeout(function () {
        if (epoch !== atfNavEpoch) return;
        var detailCards = directScenarioCards(
            document.querySelector('#test-view .test-content-detail')
          ),
          card = detailCards[index];
        if (card) {
          openScenarioCard(card);
          if (expansion === 'ui-validation') openScenarioAuditPath(card);
          if (failureOrdinal !== undefined) openScenarioFailurePath(card, failureOrdinal, failureKind);
        }
      }, 120);
    }
  }
};
function synchronizeNavigationSelection(id) {
  document.body.classList.remove('atf-overview-default');
  q('.view').forEach(function (v) {
    var active = v.id === id;
    v.classList.toggle('hide', !active);
    v.classList.toggle('d-none', !active);
  });
  q('.side-nav .nav-item').forEach(function (v) {
    v.classList.remove('active');
    q('a', v).forEach(function (link) {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
  });
  var nav = Object.keys(atfNavigationTargets).find(function (linkId) {
      return atfNavigationTargets[linkId] === id;
    }),
    navLink = nav && document.getElementById(nav);
  if (navLink) {
    navLink.classList.add('active');
    navLink.setAttribute('aria-current', 'page');
    if (navLink.parentElement) navLink.parentElement.classList.add('active');
  }
}
window.atfActivate = function (id) {
  synchronizeNavigationSelection(id);
  normalizeDurations();
  assignNodeRoles();
  normalizeEvidenceDisclosures();
  normalizeValidationTableColumns();
  if (id === 'test-view') {
    atfPerformanceBeginReadiness();
    normalizeTestPackageMetadata();
    normalizeScenarioAnnotations();
    bindScenarioCollapse();
    bindPhaseCollapse();
    bindNodeCollapse();
    scheduleTestViewNormalization();
    scheduleTestViewNormalizationAt(100);
  }
  if (id === 'atf-fails-view') renderFails();
  if (id === 'dashboard-view') {
    removeUnsupportedOverviewCards();
    populateAudit();
    normalizeTagTableHeaders();
    normalizeNativeSummaryLabels();
    normalizeChartPalettes();
    normalizePercentages();
    setTimeout(removeUnsupportedOverviewCards, 0);
    setTimeout(normalizeChartPalettes, 150);
    setTimeout(removeUnsupportedOverviewCards, 150);
  }
  if (id === 'category-view') {
    ensureCategoryFilters();
    ensureCategoryDetail();
    normalizeCategorySummaryLabels();
    normalizeCategoryLinks();
  }
  captureDomContract();
};
function activateFromHash() {
  var hash = String(window.location.hash || '').replace(/^#/, '');
  var view = hash.split('&')[0];
  if (view === 'test-view') atfActivate('test-view');
  else if (view === 'category-view') atfActivate('category-view');
  else if (view === 'atf-fails-view') atfActivate('atf-fails-view');
  else if (view === 'dashboard-view' || !view) atfActivate('dashboard-view');
  else if (view.indexOf('test-id=') === 0) {
    var parts = hash.split('&'),
      id = decodeURIComponent(view.substring(8)),
      scenario,
      expansion,
      failureOrdinal,
      failureKind;
    parts.forEach(function (part) {
      if (part.indexOf('scenario-index=') === 0)
        scenario = decodeURIComponent(part.substring(15));
      if (part.indexOf('expand=') === 0)
        expansion = decodeURIComponent(part.substring(7));
      if (part.indexOf('failure-ordinal=') === 0)
        failureOrdinal = decodeURIComponent(part.substring(16));
      if (part.indexOf('failure-kind=') === 0)
        failureKind = decodeURIComponent(part.substring(13));
    });
    atfOpenTest(id, scenario, expansion, failureOrdinal, failureKind);
  }
}
function requestedNavigationView() {
  var view = String(window.location.hash || '').replace(/^#/, '').split('&')[0];
  if (view.indexOf('test-id=') === 0) return 'test-view';
  if (view === 'test-view' || view === 'category-view' || view === 'atf-fails-view') return view;
  return 'dashboard-view';
}
function bindNavigationSelectionLinks() {
  q('.side-nav a[id^="nav-"]').forEach(function (link) {
    if (link.__atfNavigationSelectionBound) return;
    link.__atfNavigationSelectionBound = true;
    link.addEventListener('click', function (event) {
      event.preventDefault();
      event.stopPropagation();
      var target = atfNavigationTargets[link.id];
      if (!target) return;
      // Own the tab transition; Extent's delegated handler can leave a stale view.
      synchronizeNavigationSelection(target);
      setTimeout(function () {
        link.setAttribute('href', '#' + target);
        if (window.location.hash !== '#' + target) window.location.hash = target;
        synchronizeNavigationSelection(target);
      }, 0);
    }, true);
  });
}
window.addEventListener('hashchange', function () {
  activateFromHash();
  setTimeout(function () {
    synchronizeNavigationSelection(requestedNavigationView());
  }, 0);
});
function normalizeNavigationTargets() {
  Object.keys(atfNavigationTargets).forEach(function (id) {
    var link = document.getElementById(id);
    if (link) link.setAttribute('href', '#' + atfNavigationTargets[id]);
  });
}
function normalizeNavigationLinks() {
  normalizeNavigationTargets();
  bindNavigationSelectionLinks();
  normalizeTagFilter();
  bindTestFilters();
  ensureCategoryFilters();
  bindScenarioCollapse();
  bindPhaseCollapse();
  bindNodeCollapse();
  watchScenarioCards();
}
var atfReportNavigationInitialized = false;
function initializeReportNavigation() {
  if (atfReportNavigationInitialized) return;
  q('.collapse').forEach(function (v) {
    v.classList.remove('show');
  });
  q('.test-contents').forEach(function (v) {
    v.classList.add('d-none');
  });
  normalizeCodeBlocks();
  normalizeNavigationLinks();
  watchDurationNodes();
  normalizeTestPackageMetadata();
  setTimeout(function () {
    normalizeNavigationTargets();
    activateFromHash();
  }, 0);
  atfReportNavigationInitialized = true;
  setTimeout(function () {
    watchDurationNodes();
    scheduleTestViewNormalization();
    scheduleTestViewNormalizationAt(250);
    removeUnsupportedOverviewCards();
    applyTestFilters();
  }, 250);
}
document.addEventListener('DOMContentLoaded', initializeReportNavigation);
window.addEventListener('load', function () {
  initializeReportNavigation();
  normalizeNavigationTargets();
  // Extent may run its first-nav click from a queued ready callback after load.
  // Reconcile once that callback has had its turn; this only touches navigation state.
  setTimeout(function () {
    normalizeNavigationTargets();
    synchronizeNavigationSelection(requestedNavigationView());
  }, 0);
});

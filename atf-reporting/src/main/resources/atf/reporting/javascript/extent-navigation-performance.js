var atfReportPerformanceState = window.__atfReportPerformance || {
  normalizationSchedules: 0,
  normalizationRuns: 0,
  normalizationDurationMs: 0,
  relevantMutationBatches: 0,
  durationMutationBatches: 0,
  pageLoadMs: null,
  firstDisclosureToggleMs: null,
  disclosureToggleDurationMs: 0,
  disclosureToggles: 0,
  scenarioToggles: 0,
  phaseToggles: 0,
  nodeToggles: 0,
  readinessStartedAt: null,
  readinessCompletedAt: null,
  readinessMs: null,
  ready: false,
  longTaskObservationSupported: false,
  longTaskCount: 0,
  longTaskDurationMs: 0
};
window.__atfReportPerformance = atfReportPerformanceState;
function atfPerformanceNow() {
  return window.performance && typeof window.performance.now === 'function'
    ? window.performance.now() : Date.now();
}
function atfPerformanceCount(name) {
  atfReportPerformanceState[name] =
    Number(atfReportPerformanceState[name] || 0) + 1;
}
function atfPerformanceStart() {
  return atfPerformanceNow();
}
function atfPerformanceEnd(start, target) {
  if (start == null || !target) return;
  atfReportPerformanceState[target] =
    Number(atfReportPerformanceState[target] || 0) +
    Math.max(0, atfPerformanceNow() - start);
}
function atfPerformanceRecordDisclosureToggle(start) {
  if (start == null) return;
  var duration = Math.max(0, atfPerformanceNow() - start);
  atfReportPerformanceState.disclosureToggleDurationMs += duration;
  atfReportPerformanceState.disclosureToggles++;
  if (atfReportPerformanceState.firstDisclosureToggleMs == null)
    atfReportPerformanceState.firstDisclosureToggleMs = duration;
  atfPerformancePublish();
}
function atfPerformancePublish() {
  var root = document.documentElement;
  if (!root) return;
  root.setAttribute('data-atf-performance-ready',
    atfReportPerformanceState.ready ? 'true' : 'false');
  root.setAttribute('data-atf-performance-readiness-ms',
    String(atfReportPerformanceState.readinessMs == null
      ? '' : atfReportPerformanceState.readinessMs));
  root.setAttribute('data-atf-performance-page-load-ms',
    String(atfReportPerformanceState.pageLoadMs == null
      ? '' : atfReportPerformanceState.pageLoadMs));
  root.setAttribute('data-atf-performance-first-toggle-ms',
    String(atfReportPerformanceState.firstDisclosureToggleMs == null
      ? '' : atfReportPerformanceState.firstDisclosureToggleMs));
  root.setAttribute('data-atf-performance-toggle-duration-ms',
    String(atfReportPerformanceState.disclosureToggleDurationMs));
  root.setAttribute('data-atf-performance-toggle-count',
    String(atfReportPerformanceState.disclosureToggles));
  root.setAttribute('data-atf-performance-normalization-runs',
    String(atfReportPerformanceState.normalizationRuns));
}
function atfPerformanceBeginReadiness() {
  atfReportPerformanceState.readinessStartedAt = atfPerformanceNow();
  atfReportPerformanceState.readinessCompletedAt = null;
  atfReportPerformanceState.readinessMs = null;
  atfReportPerformanceState.ready = false;
  document.documentElement.removeAttribute('data-atf-test-view-ready');
  atfPerformancePublish();
}
function atfPerformanceMarkReady() {
  if (atfReportPerformanceState.readinessStartedAt == null ||
      atfReportPerformanceState.readinessCompletedAt != null) return;
  atfReportPerformanceState.readinessCompletedAt = atfPerformanceNow();
  atfReportPerformanceState.readinessMs =
    Math.max(0, atfReportPerformanceState.readinessCompletedAt -
      atfReportPerformanceState.readinessStartedAt);
  atfPerformanceRecordPageLoad();
  atfReportPerformanceState.ready = true;
  document.documentElement.setAttribute('data-atf-test-view-ready', 'true');
  atfPerformancePublish();
}
function atfPerformanceRecordPageLoad() {
  var entries = window.performance && window.performance.getEntriesByType
    ? window.performance.getEntriesByType('navigation') : [],
    duration = entries.length ? Number(entries[0].duration || 0) : 0;
  if (duration > 0) atfReportPerformanceState.pageLoadMs = duration;
}
window.addEventListener('load', function () {
  setTimeout(function () {
    atfPerformanceRecordPageLoad();
    atfPerformancePublish();
  }, 0);
});
function atfPerformanceSnapshot() {
  return Object.assign({}, atfReportPerformanceState);
}
(function observeLongTasks() {
  if (!window.PerformanceObserver ||
      !window.PerformanceObserver.supportedEntryTypes ||
      window.PerformanceObserver.supportedEntryTypes.indexOf('longtask') < 0) return;
  try {
    var observer = new PerformanceObserver(function (list) {
      list.getEntries().forEach(function (entry) {
        atfReportPerformanceState.longTaskCount++;
        atfReportPerformanceState.longTaskDurationMs += Number(entry.duration || 0);
      });
    });
    observer.observe({ entryTypes: ['longtask'] });
    atfReportPerformanceState.longTaskObservationSupported = true;
    window.__atfReportPerformanceObserver = observer;
  } catch (ignored) {}
})();

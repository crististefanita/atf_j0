function normalizeCategoryDurations(lookup) {
  if (typeof normalizeCategoryLinks === 'function') normalizeCategoryLinks();
  q('#category-view .test-list-item>.test-item').forEach(function (item) {
    var links = q('a.linked[test-id],a.linked[data-atf-test-id]', item),
      total = 0,
      count = 0;
    links.forEach(function (link) {
      var id =
        link.getAttribute('data-atf-test-id') || link.getAttribute('test-id');
      if (!id) return;
      var index = link.getAttribute('data-atf-scenario-index');
      if (index === null && typeof categoryScenarioIndex === 'function')
        index = String(categoryScenarioIndex(link, id));
      var card = scenarioCardFor(id, index, lookup);
      var value = card && card.getAttribute('data-duration-ms');
      if (value !== null && /^\d+(?:\.\d+)?$/.test(value)) {
        total += Number(value);
        count++;
      }
    });
    if (!count) return;
    var field =
      item.querySelector('.test-detail .duration') ||
      item.querySelector('.test-detail .text-sm');
    if (!field) return;
    var original = field.getAttribute('data-atf-original-label');
    if (original === null) {
      original = text(field, '');
      field.setAttribute('data-atf-original-label', original);
    }
    field.textContent = original + ' · ' + atfFormatDuration(total);
    field.setAttribute('data-duration-ms', String(total));
    field.setAttribute('data-duration-source', 'aggregated');
  });
}
function overviewTimelineEntries(lookup) {
  var roots = lookup ? lookup.roots : typeof classItems === 'function' ? classItems() : [],
    entries = [];
  if (!roots.length) return null;
  var complete = roots.every(function (root) {
    var value = root.getAttribute('data-duration-ms'),
      id = root.getAttribute('test-id');
    if (value === null || !/^\d+(?:\.\d+)?$/.test(value)) return false;
    entries.push({
      id: id,
      name: text(root, '.test-detail .name') || id,
      milliseconds: Number(value)
    });
    return true;
  });
  return complete ? entries : null;
}
function publishOverviewTimeline(entries, canvas) {
  var values = Object.create(null);
  entries.forEach(function (entry) {
    var label = entry.name;
    if (Object.prototype.hasOwnProperty.call(values, label))
      label += ' [' + entry.id + ']';
    entry.label = label;
    values[label] = entry.milliseconds / 1000;
  });
  window.timeline = values;
  if (typeof Chart === 'undefined' || !canvas) return;
  var registry = Chart.instances || {},
    charts = Array.isArray(registry)
      ? registry
      : Object.keys(registry).map(function (key) { return registry[key]; });
  charts.forEach(function (chart) {
    if (!chart || chart.canvas !== canvas || !chart.data) return;
    var previous = chart.data.datasets || [],
      datasets = entries.map(function (entry, index) {
        var existing = previous.find(function (dataset) {
          return dataset.label === entry.label;
        }) || previous[index] || {};
        return Object.assign({}, existing, {
          label: entry.label,
          data: [entry.milliseconds / 1000]
        });
      }),
      before = previous.map(function (dataset) { return [dataset.label, dataset.data]; }),
      after = datasets.map(function (dataset) { return [dataset.label, dataset.data]; });
    if (JSON.stringify(before) === JSON.stringify(after)) return;
    chart.data.datasets = datasets;
    if (typeof chart.update === 'function') chart.update(0);
  });
}
function normalizeOverviewDuration(lookup) {
  var timeline = document.getElementById('timeline'),
    host =
      timeline ||
      document.querySelector('#dashboard-view .card:has(#timeline)') ||
      document.getElementById('dashboard-view');
  if (!host) return;
  var total = 0,
    source = window.timeline || {},
    entries = overviewTimelineEntries(lookup);
  if (entries) {
    publishOverviewTimeline(entries, timeline);
    entries.forEach(function (entry) { total += entry.milliseconds; });
  } else {
    Object.keys(source).forEach(function (key) {
      var value = Number(source[key]);
      if (Number.isFinite(value) && value >= 0) total += Math.round(value * 1000);
    });
  }
  host.setAttribute('data-duration-ms', String(total));
  host.setAttribute('data-duration-source', 'aggregated');
  var label = 'Timeline duration ' + atfFormatDuration(total);
  host.setAttribute('aria-label', label);
  host.setAttribute('title', label);
}
function normalizeFailsDurations(lookup) {
  q('#atf-fails-view .atf-failure').forEach(function (row) {
    var card = scenarioCardFor(
      row.getAttribute('data-atf-test-id'),
      row.getAttribute('data-atf-scenario-index'), lookup
    );
    var value = card && card.getAttribute('data-duration-ms');
    if (value === null) return;
    row.setAttribute('data-duration-ms', value);
    row.setAttribute('data-duration-source', 'aggregated');
  });
}
function normalizeProjectedDurations() {
  var lookup = scenarioDurationLookup();
  normalizeCategoryDurations(lookup);
  normalizeOverviewDuration(lookup);
  normalizeFailsDurations(lookup);
}

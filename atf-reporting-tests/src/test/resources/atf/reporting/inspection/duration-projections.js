() => {
  const selectors = {
    Tests: '#test-view [data-duration-source],#test-view [data-duration-ms]',
    Overview: '#dashboard-view [data-duration-source],#dashboard-view [data-duration-ms]',
    Categories: '#category-view [data-duration-source],#category-view [data-duration-ms]',
    Fails: '#atf-fails-view [data-duration-source],#atf-fails-view [data-duration-ms]'
  };
  const numeric = /^\d+(?:\.\d+)?$/;
  const result = {};
  Object.entries(selectors).forEach(([name, selector]) => {
    const fields = [...document.querySelectorAll(selector)];
    const violations = [];
    let valid = 0;
    fields.forEach(field => {
      const source = field.getAttribute('data-duration-source') || '';
      const text = String(field.textContent || '').replace(/\s+/g, ' ').trim();
      const marker = field.getAttribute('data-duration-ms');
      const formatted = /^(?:\d+(?:\.\d+)? ms|\d+(?:\.\d+)? s)$/.test(text);
      const validMarker = source === 'absent'
        ? marker === null && (text === '—' || /^(?:pass|fail|warning|skip)$/i.test(text))
        : marker !== null && numeric.test(marker)
          && (source === 'aggregated' || source === 'measured' || formatted);
      if (validMarker) valid++;
      else violations.push(name + ': ' + text);
    });
    result[name] = { fieldCount: fields.length, validCount: valid, violations };
  });

  const timeline = document.getElementById('timeline');
  const roots = [...document.querySelectorAll('.test-list-item > .test-item[test-id]')];
  const complete = roots.length > 0 && roots.every(root => {
    const value = root.getAttribute('data-duration-ms');
    return value !== null && numeric.test(value);
  });
  if (!timeline || !complete) return result;
  const expected = roots.map(root => Number(root.getAttribute('data-duration-ms'))).sort((a, b) => a - b);
  const total = expected.reduce((sum, value) => sum + value, 0);
  const violations = result.Overview.violations;
  const tolerance = 0.001;
  if (Math.abs(Number(timeline.getAttribute('data-duration-ms')) - total) > tolerance) {
    violations.push('Timeline marker differs from canonical class durations: expected ' + total + ' ms');
  }
  function matches(values) {
    const sorted = values.map(value => Number(value) * 1000).sort((a, b) => a - b);
    return sorted.length === expected.length && sorted.every((value, index) =>
      Number.isFinite(value) && Math.abs(value - expected[index]) <= tolerance);
  }
  if (window.timeline && !matches(Object.values(window.timeline))) {
    violations.push('Timeline model differs from canonical class durations');
  }
  const registry = typeof Chart === 'undefined' ? [] : Chart.instances || {};
  const charts = Array.isArray(registry) ? registry : Object.values(registry);
  const chart = charts.find(candidate => candidate && candidate.canvas === timeline);
  if (chart && !matches(chart.data.datasets.flatMap(dataset => dataset.data))) {
    violations.push('Timeline chart differs from canonical class durations');
  }
  return result;
}

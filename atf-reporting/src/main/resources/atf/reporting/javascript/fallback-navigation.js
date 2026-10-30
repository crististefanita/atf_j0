history.scrollRestoration = 'manual';
const tabs = [...document.querySelectorAll('.tab')];
const panels = [...document.querySelectorAll('.tab-panel')];
function activate(id) {
  tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === id));
  panels.forEach(p => p.classList.toggle('active', p.id === id));
  history.replaceState(null, '', '#' + id);
}
function fallbackFailureTargets(scenario) {
  const diagnostics = [...scenario.querySelectorAll('.phase-diagnostics [data-atf-diagnostic-status="fail"]')];
  return [...scenario.querySelectorAll('section[data-atf-node-role="phase"]')].flatMap(phase => [
    ...diagnostics.filter(d => d.dataset.atfPhaseName.toLowerCase().replace(/ /g, '-') === phase.dataset.atfPhaseName)
      .flatMap(d => [...d.querySelectorAll('tbody tr')]),
    ...[...phase.querySelectorAll('table.assertions tbody tr')].filter(row => row.cells[3]?.querySelector('.status-fail'))
  ]);
}
function revealFallbackFailure(id, ordinal) {
  const scenario = document.getElementById(id);
  if (!scenario || !scenario.matches('article.scenario')) return false;
  activate('tests');
  const target = ordinal && fallbackFailureTargets(scenario)[Number(ordinal) - 1];
  for (let parent = target?.parentElement; parent && parent !== scenario; parent = parent.parentElement)
    if (parent.tagName === 'DETAILS') parent.open = true;
  normalizeValidationTableColumns(scenario);
  (target || scenario).scrollIntoView({ block: 'center', behavior: 'instant' });
  history.replaceState(null, '', '#' + encodeURIComponent(id) + (ordinal ? '&failure-ordinal=' + encodeURIComponent(ordinal) : ''));
  return true;
}
function navigateFallback(hash) {
  const [id, parameters] = hash.replace(/^#/, '').split('&');
  if (panels.some(panel => panel.id === id)) activate(id);
  else if (!revealFallbackFailure(decodeURIComponent(id), new URLSearchParams(parameters).get('failure-ordinal')))
    activate('overview');
}
tabs.forEach(tab => tab.addEventListener('click', () => activate(tab.dataset.tab)));
document.querySelectorAll('.fail-row').forEach(link => link.addEventListener('click', event => {
  event.preventDefault(); navigateFallback(link.getAttribute('href'));
}));
window.addEventListener('hashchange', () => navigateFallback(window.location.hash));
if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => navigateFallback(location.hash), { once: true });
else navigateFallback(location.hash);

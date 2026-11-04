history.scrollRestoration = 'manual';
const nodes = (selector, root = document) => [...root.querySelectorAll(selector)];
function synchronizeFallbackDisclosureAria(details) {
  const summary = details.querySelector(':scope > summary');
  if (summary) summary.setAttribute('aria-expanded', String(details.open));
}
document.querySelectorAll('details').forEach(synchronizeFallbackDisclosureAria);
document.addEventListener('toggle', event => {
  if (event.target.tagName === 'DETAILS') synchronizeFallbackDisclosureAria(event.target);
}, true);
const tabs = nodes('.tab');
const panels = nodes('.tab-panel');
function activate(id) {
  tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === id));
  panels.forEach(p => p.classList.toggle('active', p.id === id));
  history.replaceState(null, '', '#' + id);
}
function fallbackFailureTargets(scenario) {
  const diagnostics = nodes('.phase-diagnostics [data-atf-diagnostic-status=fail]', scenario);
  return nodes('section[data-atf-node-role=phase]', scenario).flatMap(phase => [
    ...diagnostics.filter(d => d.dataset.atfPhaseName.toLowerCase().replace(/ /g, '-') === phase.dataset.atfPhaseName)
      .flatMap(d => nodes('tbody tr', d)),
    ...nodes('[data-atf-terminal-failure=true],table.assertions tbody tr,.atf-ui-action-table tbody tr', phase)
      .filter(row => row.matches('[data-atf-terminal-failure=true]')
        ? row.closest('[data-atf-node-role=request]')?.querySelector(':scope>.node-title .status-fail')
        : row.lastElementChild?.querySelector('.status-fail,.atf-ui-action-result-fail'))
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

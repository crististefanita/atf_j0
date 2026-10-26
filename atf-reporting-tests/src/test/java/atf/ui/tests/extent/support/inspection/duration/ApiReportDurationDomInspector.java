package atf.ui.tests.extent.support.inspection.duration;
import atf.reporting.audit.ReportAuditChecks;

import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads duration semantics from the rendered DOM without navigation or policy decisions. */
public final class ApiReportDurationDomInspector {
    @SuppressWarnings("unchecked")
    ReportAuditSnapshot.Duration readDurationContract(Page page) {
        Map<String, Object> result = (Map<String, Object>) page.evaluate("""
                () => {
                  const violations = [];
                  const visible = element => {
                    if (!element) return false;
                    let current = element;
                    while (current) {
                      const style = getComputedStyle(current);
                      if (style.display === 'none' || style.visibility === 'hidden') return false;
                      current = current.parentElement;
                    }
                    return true;
                  };
                  const text = element => String(element?.textContent || '').replace(/\\s+/g, ' ').trim();
                  const parse = value => {
                    const raw = String(value || '').trim();
                    const seconds = raw.match(/^(\\d+(?:\\.\\d+)?)\\s*s$/i);
                    const millis = raw.match(/^(\\d+(?:\\.\\d+)?)\\s*ms$/i);
                    return seconds ? Number(seconds[1]) * 1000 : millis ? Number(millis[1]) : null;
                  };
                  const format = value => value <= 300
                    ? String(value) + ' ms'
                    : (value / 1000).toFixed(3).replace(/0+$/, '').replace(/\\.$/, '') + ' s';
                  const badge = card => card.querySelector(':scope > .card-header .node-info .badge-default')
                    || card.querySelector(':scope > .card-header .card-toolbar .badge-default')
                    || card.querySelector(':scope > .card-header .card-toolbar .badge');
                  const directChildren = card => Array.from(card.querySelectorAll(':scope > .accordion > .card'))
                    .filter(child => child.matches('[data-atf-node-role], [data-atf-scenario-id]'));
                  const directActions = card => {
                    let total = 0, count = 0;
                    card.querySelectorAll('.atf-ui-action-table tbody tr').forEach(row => {
                      if (row.closest('.card[data-atf-node-role], .card[data-atf-scenario-id]') !== card) return;
                      const value = parse(text(row.querySelector('.atf-ui-action-time')));
                      if (value !== null) { total += value; count++; }
                    });
                    return {total, count};
                  };
                  const transportEvidence = card => {
                    const collapse = Array.from(card.children || [])
                      .find(child => child.classList?.contains('collapse'));
                    return Boolean(collapse?.querySelector(
                      '.execution-table, .helm-trace-table, .helm-request-details-content, .helm-response-details-content'));
                  };
                  const nodes = Array.from(document.querySelectorAll(
                    '#test-view .test-content-detail:where(:not(.d-none)) .card[data-atf-node-role],'
                    + '#test-view .test-content-detail:where(:not(.d-none)) .card[data-atf-scenario-id]'
                  ));
                  let markerCount = 0, formattedCount = 0, aggregatedCount = 0;
                  const values = new Map();
                  const inspect = card => {
                    const rawDuration = card.getAttribute('data-duration-ms');
                    const duration = Number(rawDuration);
                    const durationBadge = badge(card);
                    const rawBadge = durationBadge && durationBadge.getAttribute('data-duration-ms');
                    const badgeValue = Number(rawBadge);
                    const markerValid = rawDuration !== null && rawDuration.trim() !== ''
                      && Number.isFinite(duration) && duration >= 0;
                    if (markerValid) markerCount++;
                    else violations.push('missing/non-numeric marker on ' + text(card));
                    if (!durationBadge || rawBadge === null || rawBadge.trim() === ''
                        || !Number.isFinite(badgeValue) || badgeValue !== duration) {
                      violations.push('badge marker mismatch on ' + text(card));
                    } else if (text(durationBadge) !== format(duration)) {
                      violations.push('formatter mismatch on ' + text(card) + ': ' + text(durationBadge));
                    } else {
                      formattedCount++;
                    }
                    const children = directChildren(card);
                    let childTotal = 0;
                    children.forEach(child => {
                      const childValue = inspect(child);
                      if (childValue !== null) childTotal += childValue;
                    });
                    const actions = directActions(card);
                    const measured = Number(card.getAttribute('data-atf-measured-duration-ms'));
                    const transport = transportEvidence(card) && Number.isFinite(measured) ? measured : 0;
                    const source = card.getAttribute('data-duration-source') || '';
                    if (source === 'aggregated') aggregatedCount++;
                    let expected = null;
                    if (children.length > 0 || actions.count > 0) {
                      expected = children.length > 0
                        ? Math.max(childTotal, actions.total, transport)
                        : actions.total;
                      if (markerValid && duration !== expected) {
                        violations.push('parent total mismatch on ' + text(card)
                          + ': expected ' + expected + ', actual ' + duration);
                      }
                    }
                    values.set(card, markerValid ? duration : null);
                    return markerValid ? duration : null;
                  };
                  nodes.forEach(node => { if (!values.has(node)) inspect(node); });
                  const roots = Array.from(document.querySelectorAll('#test-view .test-list-item > .test-item[test-id]'))
                    .filter(visible);
                  roots.forEach(root => {
                    const rawValue = root.getAttribute('data-duration-ms');
                    const value = Number(rawValue);
                    const scenarios = Array.from(root.querySelectorAll('.test-contents > .mt-4 > .accordion > .card'))
                      .filter(card => card.getAttribute('data-atf-filter-hidden') !== 'true');
                    const sum = scenarios.reduce((total, card) => total + (Number(card.getAttribute('data-duration-ms')) || 0), 0);
                    if (rawValue === null || rawValue.trim() === ''
                        || !Number.isFinite(value) || value < 0) violations.push('missing/non-numeric class total');
                    if (scenarios.length > 0 && value !== sum) {
                      violations.push('class total mismatch: expected ' + sum + ', actual ' + value);
                    }
                  });
                  return {nodes: nodes.length, markerCount, formattedCount, aggregatedCount,
                    ownerCount: roots.length, violations};
                }
                """);
        return new ReportAuditSnapshot.Duration(
                ReportAuditChecks.count(result.get("ownerCount")),
                ReportAuditChecks.count(result.get("nodes")),
                ReportAuditChecks.count(result.get("markerCount")),
                ReportAuditChecks.count(result.get("formattedCount")),
                ReportAuditChecks.count(result.get("aggregatedCount")),
                (List<String>) result.getOrDefault("violations", List.of()));
    }

    List<ReportAuditSnapshot.DurationProjection> readDurationProjections(Page page) {
        return ApiReportDurationProjectionAuditor.inspectCurrentDom(page);
    }
}

package atf.ui.tests.extent.support.inspection.duration;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Map;

/** Owns viewport changes and status-badge geometry only. */
public final class ApiReportDurationResponsiveInspector {
    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;

    public ApiReportDurationResponsiveInspector(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = auditFlow;
        this.locators = locators;
    }

    ReportAuditSnapshot.BadgeGeometry compare(Page page, int normalWidth, int normalHeight,
                                              int narrowWidth, int narrowHeight) {
        page.setViewportSize(normalWidth, normalHeight);
        page.reload();
        page.waitForLoadState();
        restoreTestsProjection(page, "normal viewport report state settles");
        ReportAuditSnapshot.Badge normal = statusBadgeGeometry(page);

        page.setViewportSize(narrowWidth, narrowHeight);
        page.reload();
        page.waitForLoadState();
        restoreTestsProjection(page, "narrow viewport report state settles");
        ReportAuditSnapshot.Badge narrow = statusBadgeGeometry(page);

        try {
            return new ReportAuditSnapshot.BadgeGeometry(normal, narrow);
        } finally {
            page.setViewportSize(normalWidth, normalHeight);
            page.reload();
            page.waitForLoadState();
            restoreTestsProjection(page, "configured viewport report state is restored");
        }
    }

    private void restoreTestsProjection(Page page, String description) {
        Locator tests = locators.testsView(page);
        if (!tests.isVisible()) {
            auditFlow.actions().click(locators.navTabAction(page, ReportUiContract.Tab.TESTS),
                    "restore Tests projection after viewport reload");
        }
        auditFlow.actions().waitForVisible(tests, ReportUiContract.Tab.TESTS.hash());
        auditFlow.actions().waitForAttribute(page,
                new AtfLocator(locators.runtimeRoot(page), "html"),
                "data-atf-runtime-ready", "true", description);
    }

    @SuppressWarnings("unchecked")
    private static ReportAuditSnapshot.Badge statusBadgeGeometry(Page page) {
        Map<String, Object> result = (Map<String, Object>) page.evaluate("""
                () => {
                  const labels = ['Pass', 'Fail', 'Skip', 'Warning'];
                  const visible = element => {
                    let current = element;
                    while (current) {
                      const style = getComputedStyle(current);
                      if (style.display === 'none' || style.visibility === 'hidden') return false;
                      current = current.parentElement;
                    }
                    return true;
                  };
                  const badges = [...document.querySelectorAll('.badge')]
                    .filter(element => labels.includes((element.textContent || '').trim()) && visible(element));
                  const styles = badges.map(element => {
                    const style = getComputedStyle(element);
                    return [style.display, style.boxSizing, style.height, style.minHeight,
                      style.paddingTop, style.paddingRight, style.paddingBottom, style.paddingLeft,
                      style.lineHeight, style.borderRadius].join('|');
                  });
                  return {
                    count: badges.length,
                    labels: badges.map(element => (element.textContent || '').trim()).sort().join('|'),
                    signature: [...new Set(styles)].sort().join('||')
                  };
                }
                """);
        return new ReportAuditSnapshot.Badge(
                number(result.get("count")),
                String.valueOf(result.getOrDefault("labels", "")),
                String.valueOf(result.getOrDefault("signature", "")));
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }
}

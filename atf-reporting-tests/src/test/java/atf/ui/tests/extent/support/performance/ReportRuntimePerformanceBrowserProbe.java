package atf.ui.tests.extent.support.performance;

import atf.reporting.runtime.performance.ReportRuntimePerformanceBudget;
import atf.reporting.runtime.performance.ReportRuntimePerformanceSnapshot;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Browser adapter for the renderer-neutral performance contract.
 *
 * <p>The probe deliberately reuses the generated-report flow and locator
 * facade. It does not create a business journey or decide a test verdict;
 * it only proves that a fresh report publishes the runtime counters after a
 * representative multi-signature disclosure sequence.</p>
 */
public final class ReportRuntimePerformanceBrowserProbe {
    private static final int MIN_REQUEST_SIGNATURES = 6;
    private static final ReportRuntimePerformanceBudget BUDGET =
            ReportRuntimePerformanceBudget.browserAuditDefaults();

    private final ReportUiLocators locators;

    public ReportRuntimePerformanceBrowserProbe() {
        this(new ReportUiLocators());
    }

    public ReportRuntimePerformanceBrowserProbe(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Result inspect(SectionAuditFixture fixture) {
        Objects.requireNonNull(fixture, "fixture");
        Page page = fixture.flow().openTests();
        int expandedRequests = 0;
        Set<String> signatures = new LinkedHashSet<>();
        for (String ownerName : fixture.expectedOwnerNames()) {
            fixture.flow().confirmTestsTabVisible(page, ownerName);
            var owner = fixture.flow().openOwner(page, ownerName);
            expandedRequests += fixture.flow().expandOwnerRequests(owner);
            collectSignatures(owner.detail(), signatures);
        }

        Object raw = page.evaluate("() => typeof window.atfPerformanceSnapshot === 'function'"
                + " ? window.atfPerformanceSnapshot() : window.__atfReportPerformance || null");
        Map<?, ?> values = raw instanceof Map<?, ?> map ? map : Map.of();
        ReportRuntimePerformanceSnapshot snapshot =
                ReportRuntimePerformanceSnapshot.from(values);
        List<String> violations = new ArrayList<>(BUDGET.violations(snapshot));
        if (signatures.size() < MIN_REQUEST_SIGNATURES) {
            violations.add("request signature floor is " + MIN_REQUEST_SIGNATURES
                    + "; observed " + signatures.size());
        }
        if (values.isEmpty()) {
            violations.add("browser performance snapshot is unavailable");
        }
        return new Result(page, snapshot, expandedRequests, signatures, violations);
    }

    private void collectSignatures(Locator detail, Set<String> signatures) {
        Locator requests = locators.requests(detail);
        for (int index = 0; index < requests.count(); index++) {
            Locator request = requests.nth(index);
            String title = safe(locators.requestTitle(request));
            String url = safe(locators.requestUrl(request));
            if (!title.isBlank() || !url.isBlank()) {
                signatures.add(title + " | " + url);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record Result(Page page, ReportRuntimePerformanceSnapshot snapshot,
                         int expandedRequestCount, Set<String> requestSignatures,
                         List<String> violations) {
        public Result {
            page = Objects.requireNonNull(page, "page");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            if (expandedRequestCount < 0) {
                throw new IllegalArgumentException("expandedRequestCount must not be negative");
            }
            requestSignatures = Set.copyOf(Objects.requireNonNull(requestSignatures,
                    "requestSignatures"));
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean valid() {
            return violations.isEmpty();
        }

        public String metricSummary() {
            return "ready=" + snapshot.readinessAvailable()
                    + ", readinessMs=" + snapshot.readinessMs()
                    + ", pageLoadMs=" + snapshot.pageLoadMs()
                    + ", firstDisclosureToggleMs=" + snapshot.firstDisclosureToggleMs()
                    + ", disclosureToggles=" + snapshot.disclosureToggles()
                    + ", normalizationRuns=" + snapshot.normalizationRuns()
                    + ", longTaskCount=" + snapshot.longTaskCount();
        }
    }
}

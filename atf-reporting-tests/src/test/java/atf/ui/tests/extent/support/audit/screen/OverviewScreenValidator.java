package atf.ui.tests.extent.support.audit.screen;
import atf.reporting.audit.ReportAuditChecks;
import atf.ui.tests.extent.support.data.UiTestData;
import atf.reporting.ui.contract.ReportUiContract;

import atf.reporting.contract.AtfAssertion;
import atf.mock.AtfMockConfiguration;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reusable semantic validator for the Extent Overview screen.
 *
 * <p>The validator owns the Overview DOM contract and the cross-screen
 * reconciliation rules. A test supplies the dynamic Screen Tests snapshot and
 * optionally the setup/YAML families plus the mock catalog. This keeps the
 * generated-report and existing-report tests on one implementation without
 * hard-coded totals.</p>
 */
public final class OverviewScreenValidator {
    private final ReportUiLocators locators;

    public OverviewScreenValidator() {
        this(new ReportUiLocators());
    }

    public OverviewScreenValidator(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    /** Reads only the rendered Overview state; it never mutates the report. */
    public Observation inspect(Page page) {
        return inspect(page, Profile.STANDARD);
    }

    /** Reads the same contract with the hierarchy profile required by navigation audits. */
    public Observation inspect(Page page, Profile profile) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(profile, "profile");
        Locator overview = locators.overviewView(page);
        List<String> sectionSelectors = ReportUiContract.overviewSelectors(profile.includeHierarchy());
        int sections = 0;
        for (String selector : sectionSelectors) {
            if (locators.overviewSection(overview, selector).count() > 0) sections++;
        }

        Locator metrics = locators.overviewMetrics(overview);
        int classes = metric(metrics, "Test classes in this run");
        int scenarios = metric(metrics, "Scenarios in this run");
        Locator auditRows = locators.overviewAuditRows(overview);
        int auditTotal = 0;
        for (int index = 0; index < auditRows.count(); index++) {
            Locator cells = locators.tableCells(auditRows.nth(index));
            if (cells.count() >= 2) auditTotal += ReportAuditChecks.count(cells.nth(1).textContent());
        }
        boolean complete = sections == sectionSelectors.size()
                && classes >= 0 && scenarios >= 0 && auditRows.count() > 0;
        return new Observation(sections, sectionSelectors.size(), classes, scenarios,
                auditTotal, auditRows.count(), complete);
    }

    /** Validates Overview against the supplied dynamic Screen Tests and source contract. */
    public Result validate(Page page, ScreenTestsSnapshot tests, Expected expected) {
        return validate(page, tests, expected, Profile.STANDARD);
    }

    /** Validates the selected Overview profile without duplicating cross-screen assertions. */
    public Result validate(Page page, ScreenTestsSnapshot tests, Expected expected, Profile profile) {
        Objects.requireNonNull(tests, "tests");
        Objects.requireNonNull(expected, "expected");
        Observation observed = inspect(page, profile);
        List<AtfAssertion> assertions = new ArrayList<>();
        assertions.add(ReportAuditChecks.check(
                "Overview sections", "all characteristic sections",
                observed.sectionCount() + "/" + observed.requiredSectionCount(), observed.complete()));
        assertions.add(ReportAuditChecks.check(
                "Test classes", String.valueOf(tests.ownerCount()),
                String.valueOf(observed.classCount()), observed.classCount() == tests.ownerCount()));
        assertions.add(ReportAuditChecks.check(
                "Scenarios", String.valueOf(tests.scenarioCount()),
                String.valueOf(observed.scenarioCount()), observed.scenarioCount() == tests.scenarioCount()));
        assertions.add(ReportAuditChecks.check(
                "Scenario audit total", String.valueOf(tests.scenarioCount()),
                String.valueOf(observed.auditScenarioTotal()),
                observed.auditScenarioTotal() == tests.scenarioCount()));

        if (!expected.configuredFamilies().isEmpty()) {
            assertions.add(ReportAuditChecks.check(
                    "Configured API families", String.valueOf(expected.configuredFamilies().size()),
                    String.valueOf(tests.ownerCount()),
                    tests.ownerCount() == expected.configuredFamilies().size()));
            assertions.add(ReportAuditChecks.check(
                    "Configured report owners", expected.expectedOwnerNames().toString(),
                    tests.ownerNames().toString(), tests.ownerNames().containsAll(expected.expectedOwnerNames())));
        }
        if (expected.mockRouteCount() > 0) {
            assertions.add(ReportAuditChecks.check(
                    "Mock-backed Screen Tests", "mock routes and rendered requests are both present",
                    expected.mockRouteCount() + " routes / " + tests.requestCount() + " requests",
                    tests.requestCount() > 0));
        }
        return new Result(observed, expected, assertions);
    }

    public static Expected fromScreenTests(ScreenTestsSnapshot tests) {
        Objects.requireNonNull(tests, "tests");
        return new Expected(List.of(), 0,
                Set.of(), "Screen Tests");
    }

    public enum Profile {
        STANDARD(false),
        NAVIGATION(true);

        private final boolean includeHierarchy;

        Profile(boolean includeHierarchy) { this.includeHierarchy = includeHierarchy; }

        public boolean includeHierarchy() { return includeHierarchy; }
    }

    public static Expected fromSetup(ScreenTestsSnapshot tests, Collection<String> families,
                                     AtfMockConfiguration mock) {
        Objects.requireNonNull(tests, "tests");
        Objects.requireNonNull(mock, "mock");
        List<String> normalizedFamilies = normalizeFamilies(families);
        Set<String> expectedOwners = normalizedFamilies.stream()
                .map(UiTestData.ApiFamily::fromKey)
                .flatMap(java.util.Optional::stream)
                .map(UiTestData.ApiFamily::ownerName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Expected(normalizedFamilies, mock.routes().size(),
                expectedOwners, "Screen Tests + setup/YAML + mock catalog");
    }

    private static List<String> normalizeFamilies(Collection<String> families) {
        if (families == null) return List.of();
        return families.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private int metric(Locator metrics, String label) {
        for (int index = 0; index < metrics.count(); index++) {
            Locator metric = metrics.nth(index);
            String actualLabel = locators.metricLabel(metric).textContent();
            if (label.equals(actualLabel == null ? "" : actualLabel.trim())) {
                return ReportAuditChecks.count(locators.metricValue(metric).textContent());
            }
        }
        return -1;
    }

    public record ScreenTestsSnapshot(int ownerCount, int scenarioCount, int requestCount,
                                      List<String> ownerNames) {
        public ScreenTestsSnapshot {
            if (ownerCount < 0 || scenarioCount < 0 || requestCount < 0) {
                throw new IllegalArgumentException("Screen Tests counts must be non-negative");
            }
            ownerNames = List.copyOf(Objects.requireNonNull(ownerNames, "ownerNames"));
        }
    }

    public record Observation(int sectionCount, int requiredSectionCount, int classCount,
                              int scenarioCount, int auditScenarioTotal, int auditRowCount,
                              boolean complete) { }

    public record Expected(List<String> configuredFamilies, int mockRouteCount,
                           Set<String> expectedOwnerNames,
                           String source) {
        public Expected {
            if (mockRouteCount < 0) {
                throw new IllegalArgumentException("Expected counts must be non-negative");
            }
            configuredFamilies = List.copyOf(Objects.requireNonNull(configuredFamilies, "configuredFamilies"));
            expectedOwnerNames = Set.copyOf(Objects.requireNonNull(expectedOwnerNames, "expectedOwnerNames"));
            source = Objects.requireNonNull(source, "source");
        }
    }

    public record Result(Observation observed, Expected expected, List<AtfAssertion> assertions) {
        public Result {
            observed = Objects.requireNonNull(observed, "observed");
            expected = Objects.requireNonNull(expected, "expected");
            assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions"));
        }

        public boolean valid() { return assertions.stream().allMatch(AtfAssertion::passed); }

        public String failureSummary() {
            return assertions.stream()
                    .filter(assertion -> !assertion.passed())
                    .map(assertion -> assertion.name() + " expected=" + assertion.expected()
                            + " actual=" + assertion.actual())
                    .toList().toString();
        }
    }
}

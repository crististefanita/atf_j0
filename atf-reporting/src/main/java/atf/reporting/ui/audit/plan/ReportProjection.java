package atf.reporting.ui.audit.plan;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.audit.ReportAuditSnapshot;
import atf.reporting.api.ReportScenarioSelection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Renderer-neutral expectation for one selected report.
 *
 * <p>The projection is created before DOM inspection. It belongs to reporting
 * because it describes the report contract, not a particular UI test fixture
 * or API scenario catalog.</p>
 */
public record ReportProjection(List<Owner> owners,
                               List<String> scenarioTitles,
                               int scenarioCount,
                               int phaseCount,
                               int requestCount,
                               List<String> requiredEvidence,
                               List<String> tabs,
                               List<AtfReportStatus> expectedScenarioStatuses) {
    public ReportProjection(List<Owner> owners, List<String> scenarioTitles,
                            int scenarioCount, int phaseCount, int requestCount,
                            List<String> requiredEvidence, List<String> tabs) {
        this(owners, scenarioTitles, scenarioCount, phaseCount, requestCount,
                requiredEvidence, tabs, List.of());
    }

    public static final List<String> DEFAULT_REQUIRED_EVIDENCE = List.of(
            "Request details", "Response details", "Execution evidence");
    public static final List<String> DEFAULT_TABS = List.of(
            "Tests", "Overview", "Categories", "Fails");

    public ReportProjection {
        owners = List.copyOf(Objects.requireNonNull(owners, "owners"));
        scenarioTitles = List.copyOf(Objects.requireNonNull(scenarioTitles, "scenarioTitles"));
        requiredEvidence = List.copyOf(Objects.requireNonNull(requiredEvidence, "requiredEvidence"));
        tabs = List.copyOf(Objects.requireNonNull(tabs, "tabs"));
        expectedScenarioStatuses = List.copyOf(Objects.requireNonNull(expectedScenarioStatuses,
                "expectedScenarioStatuses"));
        if (scenarioCount < 0 || phaseCount < 0 || requestCount < 0) {
            throw new IllegalArgumentException("projection counts must be non-negative");
        }
        if (scenarioCount != scenarioTitles.size() || scenarioCount != phaseCount
                || scenarioCount != requestCount) {
            throw new IllegalArgumentException("selected scenario projection counts must agree");
        }
        if (!expectedScenarioStatuses.isEmpty()
                && expectedScenarioStatuses.size() != scenarioCount) {
            throw new IllegalArgumentException(
                    "expectedScenarioStatuses must align one-to-one with scenarioTitles");
        }
    }

    /**
     * Builds the renderer-neutral expectation from exact API selections.
     *
     * <p>The selection contract is in core; the concrete catalog that resolves
     * an annotation remains in the consumer module. This keeps expected report
     * shape reusable by reporting, API acceptance and UI composition roots.</p>
     */
    public static ReportProjection from(Collection<ReportScenarioSelection> selections) {
        Objects.requireNonNull(selections, "selections");
        List<ReportScenarioSelection> selected = selections.stream()
                .filter(Objects::nonNull)
                .toList();
        List<Owner> owners = selected.stream()
                .map(selection -> new Owner(selection.reportOwner(), selection.family()))
                .distinct().toList();
        List<String> titles = selected.stream()
                .flatMap(selection -> selection.reportScenarioTitles().stream())
                .toList();
        List<AtfReportStatus> statuses = selected.stream()
                .flatMap(selection -> selection.reportScenarioStatuses().stream())
                .toList();
        return new ReportProjection(owners, titles, titles.size(), titles.size(), titles.size(),
                DEFAULT_REQUIRED_EVIDENCE, DEFAULT_TABS, statuses);
    }

    /** Empty projection used by static reports whose expected population is external. */
    public static ReportProjection empty() {
        return new ReportProjection(List.of(), List.of(), 0, 0, 0, List.of(), List.of(), List.of());
    }

    public boolean matches(ReportAuditSnapshot.Tests observed) {
        Objects.requireNonNull(observed, "observed");
        List<String> expectedOwners = owners.stream().map(Owner::name).toList();
        return observed.classCount() == expectedOwners.size()
                && sameMultiset(observed.classNames(), expectedOwners)
                && observed.scenarioCount() == scenarioCount
                && observed.scenarioTitles().size() == scenarioCount
                && sameMultiset(observed.scenarioTitles(), scenarioTitles)
                && observed.phaseCount() >= phaseCount
                && observed.requestCount() >= requestCount
                && observed.validationCount() >= requestCount
                && (expectedScenarioStatuses.isEmpty()
                || expectedScenarioStatuses.equals(observed.scenarioStatuses()));
    }

    public boolean hasStatusExpectation() {
        return !expectedScenarioStatuses.isEmpty();
    }

    public long expectedStatusCount(AtfReportStatus status) {
        Objects.requireNonNull(status, "status");
        return expectedScenarioStatuses.stream().filter(status::equals).count();
    }

    private static boolean sameMultiset(List<String> actual, List<String> expected) {
        return actual.size() == expected.size() && frequencies(actual).equals(frequencies(expected));
    }

    private static Map<String, Long> frequencies(List<String> values) {
        return values.stream().collect(Collectors.groupingBy(Function.identity(),
                LinkedHashMap::new, Collectors.counting()));
    }

    public record Owner(String name, String family) {
        public Owner {
            name = required(name, "name");
            family = required(family, "family");
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return value.trim();
        }
    }
}

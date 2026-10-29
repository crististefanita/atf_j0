package atf.reporting.model;
import atf.reporting.model.evidence.ReportRequestUiEvidence;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.projection.ReportExecutionProjections;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.ui.model.AtfUiActionRecord;

import java.util.List;
import java.util.Objects;

/**
 * Immutable renderer-neutral execution tree for one report.
 *
 * <p>The tree is the single source of truth for the fallback report views.
 * Section projections are derived from the same immutable population, so
 * Overview, Categories and Fails cannot silently disagree with Tests.</p>
 */
public record ReportExecutionModel(List<Scenario> scenarios) {
    public ReportExecutionModel {
        scenarios = List.copyOf(Objects.requireNonNull(scenarios, "scenarios"));
    }

    public Overview overview() {
        return projections().overview();
    }

    public Categories categories() {
        return projections().categories();
    }

    public Failures failures() {
        return projections().failures();
    }

    /**
     * Derives all renderer-neutral secondary views from this exact scenario
     * population. The existing accessors remain compatibility facades.
     */
    public ReportExecutionProjections projections() {
        return ReportExecutionProjections.project(scenarios);
    }

    public record Overview(long tests, long passed, long failed, long skipped) {
        public Overview {
            if (tests < 0 || passed < 0 || failed < 0 || skipped < 0) {
                throw new IllegalArgumentException("Overview counts must be non-negative");
            }
        }
    }

    public record Categories(List<String> tags) {
        public Categories {
            tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        }
    }

    public record Failures(List<Failure> items) {
        public Failures {
            items = List.copyOf(Objects.requireNonNull(items, "items"));
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }

    public record Failure(String scenarioId, String assertionName, String actual) {
        public Failure {
            scenarioId = required(scenarioId, "scenarioId");
            assertionName = assertionName == null ? "" : assertionName;
            actual = actual == null ? "" : actual;
        }
    }

    public record Scenario(String id, String testClass, String packageName, String scenario,
                           List<String> tags, List<Phase> phases, AtfReportStatus status) {
        public Scenario(String id, String testClass, String scenario,
                        List<String> tags, List<Phase> phases, AtfReportStatus status) {
            this(id, testClass, "", scenario, tags, phases, status);
        }

        public Scenario {
            id = required(id, "id");
            testClass = testClass == null ? "" : testClass;
            packageName = packageName == null ? "" : packageName;
            scenario = scenario == null ? "" : scenario;
            tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
            phases = List.copyOf(Objects.requireNonNull(phases, "phases"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record Phase(String name, List<ReportField> details,
                        List<Node> children, AtfReportStatus status,
                        java.util.Optional<atf.reporting.model.timing.ReportPhaseTiming> timing, long publicationMillis) {
        public Phase(String name, List<ReportField> details, List<Node> children, AtfReportStatus status) {
            this(name, details, children, status, java.util.Optional.empty(), 0L);
        }
        public Phase {
            timing = Objects.requireNonNull(timing, "timing");
            if (publicationMillis < 0) throw new IllegalArgumentException("Negative publication duration");
            name = name == null ? "" : name;
            details = List.copyOf(Objects.requireNonNull(details, "details"));
            children = List.copyOf(Objects.requireNonNull(children, "children"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public sealed interface Node permits Request, Validation, AssertionStep {
        AtfReportStatus status();
    }

    /** Assertion evidence for a component test, without a synthetic HTTP request. */
    public record AssertionStep(String title, List<AtfAssertion> assertions,
                                AtfReportStatus status) implements Node {
        public AssertionStep {
            title = required(title, "title");
            assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record Request(String title, String method, String path,
                          List<ReportField> details, List<ReportJsonBody> json,
                          List<ReportExecutionEvidence> executions, List<Node> children,
                          AtfReportStatus status, int uiActionCount, int screenshotCount,
                          ReportNetworkInspector.JourneyAggregate networkAggregate, ReportRequestUiEvidence uiEvidence) implements Node {
        public Request(String title, String method, String path, List<ReportField> details, List<ReportJsonBody> json,
                       List<ReportExecutionEvidence> executions, List<Node> children, AtfReportStatus status,
                       int uiActionCount, int screenshotCount, ReportNetworkInspector.JourneyAggregate networkAggregate) {
            this(title, method, path, details, json, executions, children, status, uiActionCount, screenshotCount,
                    networkAggregate, ReportRequestUiEvidence.empty());
        }
        public Request(String title, String method, String path,
                       List<ReportField> details, List<ReportJsonBody> json,
                       ReportExecutionEvidence execution, List<Node> children,
                       AtfReportStatus status) {
            this(title, method, path, details, json,
                    execution == null ? List.of() : List.of(execution), children, status, 0, 0);
        }

        public Request(String title, String method, String path,
                       List<ReportField> details, List<ReportJsonBody> json,
                       List<ReportExecutionEvidence> executions, List<Node> children,
                       AtfReportStatus status, int uiActionCount, int screenshotCount) {
            this(title, method, path, details, json, executions, children, status,
                    uiActionCount, screenshotCount, null);
        }

        public Request {
            uiEvidence = uiEvidence == null ? ReportRequestUiEvidence.empty() : uiEvidence;
            title = title == null ? "" : title;
            method = method == null ? "" : method;
            path = path == null ? "" : path;
            details = List.copyOf(Objects.requireNonNull(details, "details"));
            json = List.copyOf(Objects.requireNonNull(json, "json"));
            executions = List.copyOf(Objects.requireNonNull(executions, "executions"));
            children = List.copyOf(Objects.requireNonNull(children, "children"));
            status = Objects.requireNonNull(status, "status");
            networkAggregate = networkAggregate == null ? null : new ReportNetworkInspector.JourneyAggregate(
                    networkAggregate.authentication(), networkAggregate.correlation(),
                    networkAggregate.transport(), networkAggregate.signatures(), networkAggregate.diagnostics(),
                    networkAggregate.capture());
            if (uiActionCount < 0 || screenshotCount < 0) {
                throw new IllegalArgumentException("Evidence counts must be non-negative");
            }
        }

        /** Compatibility view for single-execution consumers. */
        public ReportExecutionEvidence execution() {
            return executions.isEmpty() ? null : executions.get(executions.size() - 1);
        }
    }

    public record Validation(String title, List<AtfAssertion> assertions,
                             AtfReportStatus status, int uiActionCount, int screenshotCount,
                             List<Node> children, List<AtfUiActionRecord> uiActions,
                             List<ReportScreenshot> screenshots, ReportEvidenceOwner evidenceOwner,
                             ReportUiActionDetailsMode actionDetailsMode) implements Node {
        public Validation(String title, List<AtfAssertion> assertions,
                          AtfReportStatus status, int uiActionCount, int screenshotCount,
                          List<Node> children, List<AtfUiActionRecord> uiActions,
                          List<ReportScreenshot> screenshots, ReportEvidenceOwner evidenceOwner) {
            this(title, assertions, status, uiActionCount, screenshotCount, children, uiActions, screenshots,
                    evidenceOwner, ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
        }
        public Validation(String title, List<AtfAssertion> assertions,
                          AtfReportStatus status, int uiActionCount, int screenshotCount,
                          List<Node> children, List<AtfUiActionRecord> uiActions, List<ReportScreenshot> screenshots) {
            this(title, assertions, status, uiActionCount, screenshotCount, children, uiActions, screenshots,
                    ReportEvidenceOwner.UNSPECIFIED);
        }
        public Validation(String title, List<AtfAssertion> assertions,
                          AtfReportStatus status, int uiActionCount, int screenshotCount,
                          List<Node> children) {
            this(title, assertions, status, uiActionCount, screenshotCount, children, List.of(), List.of());
        }

        public Validation(String title, List<AtfAssertion> assertions, AtfReportStatus status) {
            this(title, assertions, status, 0, 0, List.of());
        }

        public Validation(String title, List<AtfAssertion> assertions,
                          AtfReportStatus status, int uiActionCount, int screenshotCount) {
            this(title, assertions, status, uiActionCount, screenshotCount, List.of());
        }

        public Validation {
            actionDetailsMode = Objects.requireNonNull(actionDetailsMode, "actionDetailsMode");
            evidenceOwner = Objects.requireNonNull(evidenceOwner, "evidenceOwner");
            title = title == null ? "" : title;
            assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions"));
            status = Objects.requireNonNull(status, "status");
            children = List.copyOf(Objects.requireNonNull(children, "children"));
            uiActions = List.copyOf(Objects.requireNonNull(uiActions, "uiActions"));
            screenshots = List.copyOf(Objects.requireNonNull(screenshots, "screenshots"));
            if (uiActionCount < 0 || screenshotCount < 0) {
                throw new IllegalArgumentException("Evidence counts must be non-negative");
            }
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

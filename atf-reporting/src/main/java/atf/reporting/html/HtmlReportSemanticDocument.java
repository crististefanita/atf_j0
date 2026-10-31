package atf.reporting.html;
import atf.reporting.model.evidence.ReportRequestUiEvidence;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.model.ReportTagNormalizer;
import atf.reporting.ui.network.ReportNetworkInspector;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mutable semantic document built by the reporting lifecycle.
 *
 * <p>The document owns ordering and identity only. It deliberately does not
 * know about HTML, CSS, JavaScript or filesystem publication. This keeps the
 * lifecycle facade free to coordinate scopes while renderers consume a stable
 * suite/class/scenario/phase/request/validation model.</p>
 */
public final class HtmlReportSemanticDocument {
    private final List<ScenarioModel> scenarios = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger();

    ScenarioModel startScenario(String testClass, String scenario, Collection<String> tags) {
        return startScenario(testClass, null, scenario, tags);
    }

    ScenarioModel startScenario(String testClass, String testPackage,
                                String scenario, Collection<String> tags) {
        var model = new ScenarioModel(
                "test-id=" + nextId.incrementAndGet(),
                testClass,
                testPackage,
                scenario,
                tags);
        scenarios.add(model);
        return model;
    }

    List<ScenarioModel> scenarios() {
        return List.copyOf(scenarios);
    }

    ReportExecutionModel snapshot() {
        return HtmlReportSemanticSnapshotMapper.map(scenarios);
    }

    interface NodeModel {
        AtfReportStatus status();
    }

    public static final class ScenarioModel {
        final String id;
        final String testClass;
        final String packageName;
        final String scenario;
        final List<String> tags;
        final List<PhaseModel> phases = new ArrayList<>();
        AtfReportStatus status = AtfReportStatus.INFO;

        ScenarioModel(String id, String testClass, String packageName,
                      String scenario, Collection<String> tags) {
            this.id = id;
            this.testClass = testClass == null ? "" : testClass;
            this.packageName = packageName == null ? "" : packageName;
            this.scenario = scenario == null ? "" : scenario;
            this.tags = ReportTagNormalizer.normalize(tags);
        }

        public String id() {
            return id;
        }

        public PhaseModel addPhase(String name) {
            var phase = new PhaseModel(name);
            phases.add(phase);
            return phase;
        }

        public void complete(AtfReportStatus status) {
            this.status = status;
        }

        public boolean hasWarnings() {
            return status == AtfReportStatus.WARNING
                    || phases.stream().anyMatch(phase -> phase.status == AtfReportStatus.WARNING);
        }

        AtfReportStatus status() {
            return status;
        }
    }

    public static final class PhaseModel {
        atf.reporting.model.timing.ReportPhaseTiming timing;
        long publicationMillis;
        public void executionTiming(atf.reporting.model.timing.ReportPhaseTiming value) {
            if (timing != null) throw new IllegalStateException("Phase timing is already supplied");
            timing = Objects.requireNonNull(value, "timing");
        }
        public void publicationMillis(long value) { publicationMillis = value; }
        final String name;
        final List<ReportField> details = new ArrayList<>();
        final List<NodeModel> children = new ArrayList<>();
        AtfReportStatus status = AtfReportStatus.INFO;

        PhaseModel(String name) {
            this.name = name == null ? "" : name;
        }

        public RequestModel addRequest(String title, String method, String path) {
            var request = new RequestModel(title, method, path);
            children.add(request);
            return request;
        }

        public void addObservation(atf.reporting.model.ReportObservation value) {
            children.add(new ObservationModel(java.util.Objects.requireNonNull(value, "observation")));
        }

        public AssertionStepModel addAssertionStep(String title) {
            var step = new AssertionStepModel(title);
            children.add(step);
            return step;
        }

        public void addDetail(String name, String value) {
            details.add(new ReportField(name, value));
        }

        public void complete(AtfReportStatus status) {
            this.status = status;
        }
    }

    record ObservationModel(atf.reporting.model.ReportObservation value) implements NodeModel {
        @Override public AtfReportStatus status() { return AtfReportStatus.INFO; }
    }

    public static final class AssertionStepModel implements NodeModel {
        final String title;
        List<AtfAssertion> assertions = List.of();
        AtfReportStatus status = AtfReportStatus.INFO;

        AssertionStepModel(String title) { this.title = title; }
        public void assertions(Collection<AtfAssertion> rows) {
            assertions = rows == null ? List.of() : List.copyOf(rows);
        }
        public void complete(AtfReportStatus value) { status = value; }
        @Override public AtfReportStatus status() { return status; }
    }

    public static final class RequestModel implements NodeModel {
        final String title;
        final String method;
        final String path;
        final List<ReportField> details = new ArrayList<>();
        final List<ReportJsonBody> json = new ArrayList<>();
        final List<NodeModel> children = new ArrayList<>();
        final List<ReportExecutionEvidence> executions = new ArrayList<>();
        ReportNetworkInspector.JourneyAggregate networkAggregate;
        final List<AtfUiActionRecord> uiActions = new ArrayList<>();
        final List<ReportScreenshot> screenshots = new ArrayList<>();
        ReportUiActionDetailsMode actionDetailsMode = ReportUiActionDetailsMode.INCLUDED_COLLAPSED;
        public ReportRequestUiEvidence uiEvidence() { return new ReportRequestUiEvidence(uiActions, screenshots, actionDetailsMode); }
        public void uiActionDetailsMode(ReportUiActionDetailsMode mode) { actionDetailsMode = Objects.requireNonNull(mode, "mode"); }
        int uiActionCount;
        int screenshotCount;
        AtfReportStatus status = AtfReportStatus.INFO;

        RequestModel(String title, String method, String path) {
            this.title = title;
            this.method = method;
            this.path = path;
        }

        public void addDetail(String name, String value) {
            details.add(new ReportField(name, value));
        }

        public List<ReportField> details() {
            return List.copyOf(details);
        }

        public List<ReportJsonBody> json() {
            return List.copyOf(json);
        }

        public ReportExecutionEvidence execution() {
            return executions.isEmpty() ? null : executions.get(executions.size() - 1);
        }

        public List<ReportExecutionEvidence> executions() {
            return List.copyOf(executions);
        }


        public void addJson(String label, String json) {
            this.json.add(new ReportJsonBody(label, json));
        }

        public void addBody(ReportJsonBody body) {
            this.json.add(Objects.requireNonNull(body, "body"));
        }

        public void execution(ReportExecutionEvidence result) {
            if (result != null) {
                executions.add(result);
            }
        }

        public void networkAggregate(ReportNetworkInspector.JourneyAggregate aggregate) {
            networkAggregate = aggregate;
        }

        /** Compatibility seam for test-only legacy execution fixtures. */
        public void execution(Object result) {
            execution(ReportModelAdapters.execution(result));
        }

        public void addUiActions(Collection<AtfUiActionRecord> actions) {
            var stable = actions == null ? List.<AtfUiActionRecord>of() : List.copyOf(actions);
            uiActions.addAll(stable);
            uiActionCount += stable.size();
        }

        public void addScreenshot(String title, String base64Png) {
            screenshots.add(new ReportScreenshot(title, base64Png));
            screenshotCount++;
        }

        public void addScreenshot() {
            screenshotCount++;
        }

        public ValidationModel addValidation(String title) {
            var validation = new ValidationModel(title);
            children.add(validation);
            return validation;
        }

        public void complete(AtfReportStatus status) {
            var completedChildren = new ReportStatusAggregator();
            children.forEach(child -> completedChildren.record(child.status()));
            this.status = completedChildren.resolve(status);
        }

        @Override
        public AtfReportStatus status() {
            return status;
        }
    }

    public static final class ValidationModel implements NodeModel {
        ReportUiActionDetailsMode actionDetailsMode = ReportUiActionDetailsMode.INCLUDED_COLLAPSED;

        public void uiActionDetailsMode(ReportUiActionDetailsMode mode) {
            actionDetailsMode = Objects.requireNonNull(mode, "mode");
        }
        ReportEvidenceOwner evidenceOwner = ReportEvidenceOwner.UNSPECIFIED;
        final String title;
        List<AtfAssertion> assertions = List.of();
        final List<NodeModel> children = new ArrayList<>();
        final List<AtfUiActionRecord> uiActions = new ArrayList<>();
        final List<ReportScreenshot> screenshots = new ArrayList<>();
        int uiActionCount;
        int screenshotCount;
        AtfReportStatus status = AtfReportStatus.INFO;

        ValidationModel(String title) {
            this.title = title;
        }

        public void evidenceOwner(ReportEvidenceOwner owner) {
            evidenceOwner = java.util.Objects.requireNonNull(owner, "evidenceOwner");
        }

        public void assertions(Collection<AtfAssertion> assertions) {
            this.assertions = assertions == null ? List.of() : List.copyOf(assertions);
        }

        public String title() {
            return title;
        }

        public List<AtfAssertion> assertions() {
            return assertions;
        }

        public RequestModel addRequest(String title, String method, String path) {
            var request = new RequestModel(title, method, path);
            children.add(request);
            return request;
        }

        public List<NodeModel> children() {
            return List.copyOf(children);
        }

        public void addUiActions(Collection<AtfUiActionRecord> actions) {
            uiActionCount += actions == null ? 0 : actions.size();
            if (actions != null) uiActions.addAll(actions);
        }

        public void addScreenshot(String title, String base64Png) {
            screenshots.add(new ReportScreenshot(title, base64Png));
            screenshotCount++;
        }

        public void complete(AtfReportStatus status) {
            this.status = status;
        }

        @Override
        public AtfReportStatus status() {
            return status;
        }
    }
}

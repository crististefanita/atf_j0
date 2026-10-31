package atf.reporting.extent;
import atf.reporting.evidence.policy.UiActionScreenshotPolicy;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.ReportStatusAggregator;
import atf.reporting.extent.session.ExtentReportTree;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.model.ReportField;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.semantic.AtfReportSemanticRecorder;
import atf.reporting.ui.network.ReportNetworkAggregateMarkup;
import atf.reporting.ui.network.ReportNetworkInspector;

import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import com.aventstack.extentreports.ExtentTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class ExtentRequestScope implements AtfReportPorts.Request {
    private final ExtentReportTree tree;
    private final ExtentPhaseScope phase;
    private final ExtentTest request;
    private final String requestTitle;
    private final List<ReportField> details = new ArrayList<>();
    private final List<ReportJsonBody> json = new ArrayList<>();
    private final List<ReportScreenshot> screenshots = new ArrayList<>();
    private final List<AtfUiActionRecord> uiActions = new ArrayList<>();
    private ReportUiActionDetailsMode detailsMode;
    private final ReportStatusAggregator statuses = new ReportStatusAggregator();
    private final AtfReportSemanticRecorder.RequestHandle semantic;
    private final Consumer<AtfReportStatus> onComplete;
    private final List<ReportExecutionEvidence> executions = new ArrayList<>();
    private ReportNetworkInspector.JourneyAggregate networkAggregate;
    private ExtentTest networkAggregateNode;

    ExtentRequestScope(ExtentReportTree tree, ExtentPhaseScope phase, ExtentTest request,
                       String requestTitle, AtfReportSemanticRecorder.RequestHandle semantic) {
        this(tree, phase, request, requestTitle, semantic, status -> { });
    }

    ExtentRequestScope(ExtentReportTree tree, ExtentPhaseScope phase, ExtentTest request,
                       String requestTitle, AtfReportSemanticRecorder.RequestHandle semantic,
                       Consumer<AtfReportStatus> onComplete) {
        this.tree = tree;
        this.phase = phase;
        this.request = request;
        this.requestTitle = requestTitle;
        this.semantic = Objects.requireNonNull(semantic, "semantic");
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete");
        this.detailsMode = tree.defaultDetailsMode();
        tree.markStarted(request);
    }

    @Override public void detail(String name, String value) {
        ensureRequestOpen();
        String redacted = tree.redactor().redactField(name, value);
        details.add(new ReportField(tree.normalizeDetailName(name), redacted));
        semantic.detail(name);
        tree.consoleDetail(AtfConsoleEvent.Scope.REQUEST,
                tree.normalizeDetailName(name), redacted);
    }

    @Override public void json(String label, String value) {
        ensureRequestOpen();
        body(new ReportJsonBody(label == null || label.isBlank() ? "JSON BODY" : label, value));
    }

    @Override public void body(ReportJsonBody body) {
        ensureRequestOpen();
        String redacted = tree.redactor().redactJson(body.body());
        json.add(new ReportJsonBody(body.label(), redacted, body.direction(), body.format()));
        semantic.json(body.label());
        tree.consoleCode(AtfConsoleEvent.Scope.REQUEST, body.label(), redacted);
    }

    @Override public void screenshot(String title, String base64Png) {
        ensureRequestOpen();
        if (base64Png == null || base64Png.isBlank()) {
            throw new IllegalArgumentException("base64Png must not be blank");
        }
        var screenshot = new ReportScreenshot(title, base64Png);
        var candidate = new ArrayList<>(screenshots); candidate.add(screenshot);
        UiActionScreenshotPolicy.validate(uiActions, candidate);
        screenshots.add(screenshot);
        semantic.screenshot();
    }

    @Override public void uiActions(java.util.Collection<AtfUiActionRecord> actions) {
        ensureRequestOpen();
        var stable = actions == null ? List.<AtfUiActionRecord>of() : List.copyOf(actions);
        var candidate = new ArrayList<>(uiActions); candidate.addAll(stable);
        UiActionScreenshotPolicy.validate(candidate, screenshots);
        semantic.uiActions(stable);
        uiActions.addAll(stable);
    }

    @Override public void uiActionDetailsMode(ReportUiActionDetailsMode mode) {
        ensureRequestOpen();
        detailsMode = Objects.requireNonNull(mode, "mode");
    }

    @Override public void execution(ReportExecutionEvidence result) {
        ensureRequestOpen();
        if (result != null) {
            executions.add(result);
            semantic.execution();
        }
    }

    @Override public void networkAggregate(ReportNetworkInspector.JourneyAggregate aggregate) {
        ensureRequestOpen();
        if (aggregate == null) return;
        semantic.networkAggregate();
        networkAggregate = aggregate;
        if (ReportNetworkAggregateMarkup.hasContent(aggregate) && networkAggregateNode == null) {
            networkAggregateNode = request.createNode(
                    "Network aggregate · " + aggregate.observations() + " exchanges");
            tree.markStarted(networkAggregateNode);
        }
        if (networkAggregateNode != null) {
            networkAggregateNode.getModel().setName("Network aggregate · " + aggregate.observations() + " exchanges");
        }
    }

    @Override public void complete(AtfReportStatus status) {
        ensureRequestOpen();
        UiActionScreenshotPolicy.validate(uiActions, screenshots);
        var effective = statuses.resolve(status);
        semantic.complete(effective);
        if (isSetupEvidence()) {
            tree.renderSetupEvidence(request, details);
            tree.renderUiActions(request, uiActions, screenshots, detailsMode);
        } else if (EvidenceTableFormatter.isCompactUiFlow(details)) {
            tree.renderUiFlowProvenance(request, details);
            tree.renderUiActions(request, uiActions, screenshots, detailsMode);
        } else {
            tree.renderUiFlowProvenance(request, details);
            tree.renderRequestDetails(request, details, json);
            tree.renderResponseDetails(request, details, json);
            executions.forEach(execution -> tree.renderExecution(request, execution));
            tree.renderUiActions(request, uiActions, screenshots, detailsMode);
            if (uiActions.stream().noneMatch(action -> action.source() == AtfUiEvidenceSource.ACTION)) {
                tree.renderScreenshots(request, screenshots);
            }
        }
        if (networkAggregateNode != null) {
            tree.renderNetworkAggregateSubstep(networkAggregateNode, networkAggregate);
            tree.complete(networkAggregateNode, AtfReportStatus.PASS);
            tree.markEnded(networkAggregateNode);
        }
        tree.complete(request, effective);
        tree.markEnded(request);
        phase.record(effective);
        tree.consoleCompleted(AtfConsoleEvent.Scope.REQUEST,
                requestTitle, effective, request);
        onComplete.accept(effective);
    }

    @Override public AtfReportPorts.Validation startValidation(String title) {
        ensureRequestOpen();
        String validationTitle = title == null ? "Validation" : title;
        tree.consoleStarted(AtfConsoleEvent.Scope.VALIDATION, validationTitle);
        // A validation is a substep of the Gherkin-shaped request/statement.
        // Creating it from the phase makes it a misleading sibling in Tests;
        // creating it from the request preserves the semantic hierarchy.
        return new ExtentValidationScope(tree, this, request.createNode(validationTitle), validationTitle,
                semantic.startValidation(validationTitle));
    }

    void record(AtfReportStatus status) { statuses.record(status); }
    ExtentPhaseScope phase() { return phase; }
    @Override public void close() { if (!semantic.isComplete()) complete(AtfReportStatus.FAIL); }

    private void ensureRequestOpen() {
        tree.ensureOpen();
        semantic.ensureOpen();
    }

    private boolean isSetupEvidence() {
        return details.stream().anyMatch(detail ->
                AtfReportEvidenceKeys.EVIDENCE_TYPE.equalsIgnoreCase(detail.name())
                        && AtfReportEvidenceKeys.REPORT_SETUP.equalsIgnoreCase(detail.value()));
    }
}

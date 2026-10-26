package atf.api.reporting.evidence;
import atf.api.reporting.contract.evidence.ReportEvidenceOwner;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.reporting.contract.AtfAssertion;

import atf.api.reporting.configuration.ReportingSettings;
import atf.api.reporting.security.SensitiveDataRedactor;
import atf.api.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.api.reporting.ui.model.AtfUiActionRecord;
import atf.api.reporting.ui.network.ReportNetworkInspector;
import atf.api.reporting.model.ReportField;
import atf.api.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.api.reporting.model.ReportJsonBody;
import atf.api.reporting.model.ReportScreenshot;
import com.aventstack.extentreports.ExtentTest;

import java.util.Collection;
import java.util.List;

/** Coordinates specialized evidence renderers without owning Extent tree lifecycle. */
public final class ExtentEvidenceRenderer {
    private final ExtentTransportEvidenceRenderer transport;
    private final ExtentUiEvidenceRenderer ui;

    public ExtentEvidenceRenderer(ReportingSettings settings, SensitiveDataRedactor redactor) {
        this(settings, redactor, ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    public ExtentEvidenceRenderer(ReportingSettings settings, SensitiveDataRedactor redactor,
                                  ReportUiActionDetailsMode detailsMode) {
        this.transport = new ExtentTransportEvidenceRenderer(settings, redactor);
        this.ui = new ExtentUiEvidenceRenderer(settings, redactor, detailsMode);
    }

    public void renderPhaseDiagnostics(ExtentTest scenario, List<ReportPhaseDiagnostic> diagnostics) {
        transport.renderPhaseDiagnostics(scenario, diagnostics);
    }

    public void renderRequestDetails(ExtentTest node, List<ReportField> details,
                                     List<ReportJsonBody> json) {
        transport.renderRequestDetails(node, details, json);
    }

    public void renderResponseDetails(ExtentTest node, List<ReportField> details,
                                      List<ReportJsonBody> json) {
        transport.renderResponseDetails(node, details, json);
    }

    public void renderUiFlowProvenance(ExtentTest node, List<ReportField> details) {
        transport.renderUiFlowProvenance(node, details);
    }

    public void renderSetupEvidence(ExtentTest node, List<ReportField> details) {
        transport.renderSetupEvidence(node, details);
    }

    public void renderNetworkAggregate(ExtentTest node,
                                       ReportNetworkInspector.JourneyAggregate aggregate) {
        transport.renderNetworkAggregate(node, aggregate);
    }

    public void renderNetworkAggregateSubstep(ExtentTest node,
                                               ReportNetworkInspector.JourneyAggregate aggregate) {
        transport.renderNetworkAggregateSubstep(node, aggregate);
    }

    public void renderExecution(ExtentTest node, ReportExecutionEvidence execution) {
        transport.renderExecution(node, execution);
    }

    public void renderScreenshots(ExtentTest node, List<ReportScreenshot> screenshots) {
        transport.renderScreenshots(node, screenshots);
    }

    public void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions) {
        ui.renderUiActions(node, actions);
    }

    public void renderUiActions(ExtentTest node, List<AtfUiActionRecord> actions,
                                List<ReportScreenshot> screenshots) {
        ui.renderUiActions(node, actions, screenshots);
    }

    public void renderUiActionsAsMedia(ExtentTest node, List<AtfUiActionRecord> actions) {
        ui.renderUiActionsAsMedia(node, actions);
    }

    public void renderAssertions(ExtentTest node, Collection<AtfAssertion> assertions) {
        renderAssertions(node, assertions, ReportEvidenceOwner.UNSPECIFIED);
    }

    public void renderAssertions(ExtentTest node, Collection<AtfAssertion> assertions, ReportEvidenceOwner owner) {
        ui.renderAssertions(node, assertions, owner);
    }
}

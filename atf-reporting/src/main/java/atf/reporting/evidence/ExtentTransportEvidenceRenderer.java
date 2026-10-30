package atf.reporting.evidence;
import atf.reporting.evidence.setup.ReportSetupEvidenceMarkup;
import atf.reporting.api.evidence.ReportExecutionSummary;
import atf.reporting.api.evidence.ReportTransportTiming;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.evidence.markup.UiFlowProvenanceMarkup;
import atf.reporting.evidence.markup.ReportEvidenceJsonFormatter;
import atf.reporting.evidence.diagnostic.ReportPhaseDiagnosticMarkup;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.network.ReportNetworkAggregateMarkup;
import atf.reporting.ui.network.ReportNetworkInspector;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.security.SensitiveDataRedactor;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.MarkupHelper;

import java.util.List;

/** Renders request, response and retry evidence for transport-oriented steps. */
public final class ExtentTransportEvidenceRenderer {
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor;

    ExtentTransportEvidenceRenderer(ReportingSettings settings, SensitiveDataRedactor redactor) {
        this.settings = settings;
        this.redactor = redactor;
    }

    void renderPhaseDiagnostics(ExtentTest scenario, List<ReportPhaseDiagnostic> diagnostics) {
        String markup = ReportPhaseDiagnosticMarkup.render(diagnostics, redactor, settings.assertionMaxDisplayCharacters());
        if (!markup.isBlank()) scenario.info(markup);
    }

    void renderRequestDetails(ExtentTest node, List<ReportField> details,
                              List<ReportJsonBody> json) {
        List<ReportField> requestDetails = EvidenceTableFormatter.requestFields(details);
        List<ReportJsonBody> requestJson = json.stream()
                .filter(item -> item.direction() == ReportJsonBody.Direction.REQUEST).toList();
        if (requestDetails.isEmpty() && requestJson.isEmpty()) return;
        node.info(ReportEvidenceMarkup.disclosure(
                AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE.value(),
                AtfReportLabels.requestDetails(requestDetails.size()),
                requestEvidenceMarkup(requestDetails, requestJson)));
    }

    void renderResponseDetails(ExtentTest node, List<ReportField> details,
                               List<ReportJsonBody> json) {
        List<ReportField> responseDetails = EvidenceTableFormatter.technicalTraceFields(details);
        List<ReportJsonBody> responseJson = json.stream()
                .filter(item -> item.direction() == ReportJsonBody.Direction.RESPONSE).toList();
        if (responseDetails.isEmpty() && responseJson.isEmpty()) return;
        node.info(ReportEvidenceMarkup.disclosure(
                AtfReportHtmlVocabulary.CssClass.RESPONSE_EVIDENCE.value(),
                AtfReportLabels.responseDetails(responseDetails.size()),
                responseEvidenceMarkup(responseDetails, responseJson)));
    }

    void renderUiFlowProvenance(ExtentTest node, List<ReportField> details) {
        String markup = UiFlowProvenanceMarkup.render(details, redactor);
        if (!markup.isBlank()) node.info(markup);
    }

    void renderSetupEvidence(ExtentTest node, List<ReportField> details) {
        var disclosure = ReportSetupEvidenceMarkup.disclosure(details, redactor, settings);
        String markup = ReportEvidenceMarkup.disclosure(disclosure);
        if (disclosure.status() == AtfReportStatus.FAIL) node.fail(markup); else node.info(markup);
    }

    void renderNetworkAggregate(ExtentTest node,
                                ReportNetworkInspector.JourneyAggregate aggregate) {
        String markup = ReportNetworkAggregateMarkup.disclosure(aggregate);
        if (!markup.isBlank()) node.info(markup);
    }

    void renderNetworkAggregateSubstep(ExtentTest node,
                                       ReportNetworkInspector.JourneyAggregate aggregate) {
        ReportNetworkAggregateMarkup.substepSections(aggregate)
                .forEach(node::info);
    }

    private String requestEvidenceMarkup(List<ReportField> details, List<ReportJsonBody> json) {
        return evidenceMarkup(AtfReportHtmlVocabulary.CssClass.HELM_REQUEST_DETAILS.value(), "",
                AtfReportHtmlVocabulary.CssClass.REQUEST_JSON_BODY.value(), details, json);
    }

    private String responseEvidenceMarkup(List<ReportField> details, List<ReportJsonBody> json) {
        return evidenceMarkup(AtfReportHtmlVocabulary.CssClass.HELM_RESPONSE_DETAILS.value(),
                AtfReportHtmlVocabulary.CssClass.TECHNICAL.value(),
                AtfReportHtmlVocabulary.CssClass.RESPONSE_JSON_BODY.value(), details, json);
    }

    private String evidenceMarkup(String rootClass, String classification, String jsonClass,
                                   List<ReportField> details, List<ReportJsonBody> json) {
        var html = new StringBuilder(AtfReportHtmlVocabulary.openTag(
                AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.classAttribute(rootClass + "-content")));
        if (!classification.isBlank() && details.isEmpty()) {
            html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.HELM_API_CLASSIFICATION.value(),
                                    classification)))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SPAN,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.HELM_API_CLASSIFICATION_LABEL)))
                    .append(AtfReportLabels.TECHNICAL_TRACE)
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SPAN))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        }
        if (!details.isEmpty()) {
            String css = classification.isBlank()
                    ? AtfReportHtmlVocabulary.classList(
                    AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                    AtfReportHtmlVocabulary.CssClass.HELM_REQUEST_TABLE)
                    : AtfReportHtmlVocabulary.classList(
                    AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                    AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE);
             html.append(AtfReportMarkup.tableHtml(css, EvidenceTableFormatter.toTable(details)));
        }
        for (ReportJsonBody item : json) {
            html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                            AtfReportHtmlVocabulary.classAttribute(jsonClass)))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.HELM_SCROLL_BLOCK_TITLE)))
                    .append(ReportEvidenceJsonFormatter.label(item))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.HELM_SCROLL_BLOCK_BODY)))
                    .append(MarkupHelper.createCodeBlock(codeBlockText(
                            ReportEvidenceJsonFormatter.displayBody(item, redactor, settings))).getMarkup())
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        }
        return html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV)).toString();
    }

    /** Extent stores code in a textarea: escape RCDATA without changing its visible text. */
    private static String codeBlockText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    void renderExecution(ExtentTest node, ReportExecutionEvidence execution) {
        if (execution == null) return;
        var body = new StringBuilder(AtfReportHtmlVocabulary.openTag(
                AtfReportHtmlVocabulary.Tag.DIV,
                ReportEvidenceMarkup.executionAttributes(execution, redactor),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE_BODY)))
                .append(AtfReportMarkup.table(AtfReportHtmlVocabulary.classList(
                        AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                        AtfReportHtmlVocabulary.CssClass.EXECUTION_TABLE),
                        ReportExecutionSummary.table(execution, redactor, settings)).getMarkup());
        if (!execution.attemptHistory().isEmpty()) {
            body.append(AtfReportMarkup.table(AtfReportHtmlVocabulary.classList(
                            AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                            AtfReportHtmlVocabulary.CssClass.RETRY_HISTORY_TABLE),
                    attemptTable(execution.attemptHistory())).getMarkup());
        }
        if (execution.trace().present()) {
            body.append(AtfReportMarkup.table(AtfReportHtmlVocabulary.classList(
                            AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                            AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE),
                    ReportEvidenceTable.of(List.of(AtfReportLabels.FIELD, AtfReportLabels.VALUE),
                            execution.trace().fields().entrySet().stream()
                                    .map(entry -> List.of(entry.getKey(), entry.getValue())).toList())).getMarkup());
        }
        node.info(ReportEvidenceMarkup.disclosure(
                AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE.value(),
                AtfReportLabels.executionEvidence(execution.attempts()),
                body.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV)).toString()));
    }

    void renderScreenshots(ExtentTest node, List<ReportScreenshot> screenshots) {
        for (ReportScreenshot screenshot : ExtentReportAssets.distinctScreenshotModels(screenshots)) {
            String title = screenshot.title();
            node.info(() -> AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                    AtfReportHtmlVocabulary.classAttribute(
                            AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT))
                    + ExtentReportAssets.base64ScreenshotLink(
                             AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT_LINK.value(), title, screenshot.base64Png())
                    + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        }
    }

    private ReportEvidenceTable attemptTable(List<ReportAttemptRecord> attempts) {
        var rows = attempts.stream().map(attempt -> List.of(
                String.valueOf(attempt.number()),
                attempt.statusLabel(),
                ReportTransportTiming.display(attempt.duration()),
                attempt.outcome(),
                redactor.redactText(attempt.reason()))).toList();
        return ReportEvidenceTable.of(List.of(AtfReportLabels.ATTEMPT, AtfReportLabels.STATUS,
                AtfReportLabels.DURATION, AtfReportLabels.OUTCOME, AtfReportLabels.REASON), rows);
    }
}

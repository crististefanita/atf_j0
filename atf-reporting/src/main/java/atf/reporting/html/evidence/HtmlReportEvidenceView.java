package atf.reporting.html.evidence;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.evidence.setup.ReportSetupEvidenceMarkup;
import atf.reporting.api.evidence.ReportExecutionSummary;
import atf.reporting.api.evidence.ReportTransportTiming;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.evidence.markup.UiFlowProvenanceMarkup;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.table.ReportTableViewport;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.evidence.markup.ReportEvidenceJsonFormatter;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.ui.network.ReportNetworkAggregateMarkup;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.evidence.ui.ExtentUiActionRenderer;
import atf.reporting.evidence.ui.ExtentUiObservationRenderer;
import atf.reporting.markup.ExtentReportAssets;

import java.util.List;
import java.util.Objects;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.attribute;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.classAttribute;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.closeTag;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.openTag;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.NODE_ROLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.ASSERTIONS;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.EXECUTION_TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.HELM_REQUEST_TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.HELM_SCROLL_BLOCK_TITLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.JSON_CODE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.NODE_TITLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_DETAILS_CONTENT;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.REQUEST_JSON_BODY;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_EVIDENCE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_DETAILS_CONTENT;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RESPONSE_JSON_BODY;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.RETRY_HISTORY_TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.CssClass.VALIDATION;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.DIV;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.PRE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.SPAN;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.TABLE;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.TBODY;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.TD;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.TH;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.THEAD;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Tag.TR;

/** Renders request, response, execution and validation evidence for fallback HTML. */
public final class HtmlReportEvidenceView {
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor;

    public HtmlReportEvidenceView(ReportingSettings settings, SensitiveDataRedactor redactor) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    public void renderRequestDetails(StringBuilder html, ReportExecutionModel.Request request) {
        var details = EvidenceTableFormatter.requestFields(request.details());
        var json = request.json().stream()
                .filter(item -> item.direction() == ReportJsonBody.Direction.REQUEST).toList();
        if (details.isEmpty() && json.isEmpty()) return;
        html.append(ReportEvidenceMarkup.disclosureStart(
                        REQUEST_EVIDENCE.value(), AtfReportLabels.requestDetails(details.size())))
                .append(openTag(DIV, classAttribute(REQUEST_DETAILS_CONTENT)));
        renderTable(html, AtfReportHtmlVocabulary.classList(HELM_EVIDENCE_TABLE, HELM_REQUEST_TABLE), details);
        renderJsonBodies(html, json, REQUEST_JSON_BODY);
        html.append(closeTag(DIV)).append(ReportEvidenceMarkup.disclosureEnd());
    }

    public void renderResponseDetails(StringBuilder html, ReportExecutionModel.Request request) {
        var details = EvidenceTableFormatter.technicalTraceFields(request.details());
        var json = request.json().stream()
                .filter(item -> item.direction() == ReportJsonBody.Direction.RESPONSE).toList();
        if (details.isEmpty() && json.isEmpty()) return;
        html.append(ReportEvidenceMarkup.disclosureStart(
                        RESPONSE_EVIDENCE.value(), AtfReportLabels.responseDetails(details.size())))
                .append(openTag(DIV, classAttribute(RESPONSE_DETAILS_CONTENT)));
        renderTable(html, AtfReportHtmlVocabulary.classList(HELM_EVIDENCE_TABLE, HELM_TRACE_TABLE), details);
        renderJsonBodies(html, json, RESPONSE_JSON_BODY);
        html.append(closeTag(DIV)).append(ReportEvidenceMarkup.disclosureEnd());
    }

    public void renderRequestContext(StringBuilder html, ReportExecutionModel.Request request) {
        if (ReportSetupEvidenceMarkup.isSetup(request.details())) {
            html.append(ReportEvidenceMarkup.disclosure(ReportSetupEvidenceMarkup.disclosure(request.details(), redactor, settings)));
        } else if (EvidenceTableFormatter.isCompactUiFlow(request.details())) {
            renderUiFlowProvenance(html, request);
        } else {
            renderUiFlowProvenance(html, request);
            renderRequestDetails(html, request); renderResponseDetails(html, request);
        }
        var ui = request.uiEvidence();
        renderUiEvidence(html, ui.actions(), ui.screenshots(), ui.detailsMode());
    }

    public void renderUiFlowProvenance(StringBuilder html, ReportExecutionModel.Request request) {
        html.append(UiFlowProvenanceMarkup.render(request.details(), redactor));
    }

    private void renderTable(StringBuilder html, String cssClass, List<ReportField> details) {
        if (details.isEmpty()) return;
        html.append(AtfReportMarkup.tableHtml(cssClass, EvidenceTableFormatter.toTable(
                details.stream()
                        .map(detail -> new ReportField(detail.name(),
                                redactor.redactField(detail.name(), detail.value())))
                .toList())));
    }

    private void renderJsonBodies(StringBuilder html, List<ReportJsonBody> json,
                                  AtfReportHtmlVocabulary.CssClass cssClass) {
        for (ReportJsonBody item : json) {
            html.append(openTag(DIV, classAttribute(cssClass)))
                    .append(openTag(DIV, classAttribute(HELM_SCROLL_BLOCK_TITLE)))
                    .append(escape(ReportEvidenceJsonFormatter.label(item)))
                    .append(closeTag(DIV)).append(openTag(PRE, classAttribute(JSON_CODE)))
                    .append(escape(ReportEvidenceJsonFormatter.displayBody(item, redactor, settings)))
                    .append(closeTag(PRE)).append(closeTag(DIV));
        }
    }

    public void renderExecution(StringBuilder html, ReportExecutionEvidence execution) {
        if (execution == null) return;
        html.append(ReportEvidenceMarkup.disclosureStart(
                        EXECUTION_EVIDENCE.value(), AtfReportLabels.executionEvidence(execution.attempts())))
                .append(openTag(DIV, ReportEvidenceMarkup.executionAttributes(execution, redactor, settings)))
                .append(AtfReportMarkup.tableHtml(
                        AtfReportHtmlVocabulary.classList(HELM_EVIDENCE_TABLE, EXECUTION_TABLE),
                        ReportExecutionSummary.table(execution, redactor, settings)
                                .withFirstColumnHeaders()));
        if (!execution.attemptHistory().isEmpty()) {
            var rows = execution.attemptHistory().stream().map(attempt -> List.of(
                    String.valueOf(attempt.number()),
                    attempt.statusLabel(),
                    ReportTransportTiming.display(attempt.duration()),
                    attempt.outcome(),
                    redactor.redactText(attempt.reason()))).toList();
            html.append(AtfReportMarkup.tableHtml(
                    AtfReportHtmlVocabulary.classList(HELM_EVIDENCE_TABLE, RETRY_HISTORY_TABLE),
                    ReportEvidenceTable.of(List.of(AtfReportLabels.ATTEMPT, AtfReportLabels.STATUS,
                            AtfReportLabels.DURATION, AtfReportLabels.OUTCOME, AtfReportLabels.REASON), rows)));
        }
        if (execution.trace().present()) {
            html.append(AtfReportMarkup.tableHtml(
                    AtfReportHtmlVocabulary.classList(HELM_EVIDENCE_TABLE, AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE),
                    ReportEvidenceTable.of(List.of(AtfReportLabels.FIELD, AtfReportLabels.VALUE),
                            execution.trace().fields().entrySet().stream()
                                    .map(entry -> List.of(entry.getKey(), entry.getValue())).toList())));
        }
        html.append(closeTag(DIV)).append(ReportEvidenceMarkup.disclosureEnd());
    }

    public void renderNetworkAggregate(StringBuilder html,
                                       ReportNetworkInspector.JourneyAggregate aggregate) {
        html.append(ReportNetworkAggregateMarkup.disclosure(aggregate));
    }

    public void renderNetworkAggregateSubstep(StringBuilder html,
                                              ReportNetworkInspector.JourneyAggregate aggregate) {
        if (!ReportNetworkAggregateMarkup.hasContent(aggregate)) return;
        html.append(openTag(DIV, classAttribute(VALIDATION.value(), "network-aggregate-substep"),
                        attribute(NODE_ROLE, "network-aggregate")))
                .append(openTag(DIV, classAttribute(NODE_TITLE)))
                .append(openTag(SPAN))
                .append(escape("Network aggregate · " + aggregate.observations() + " exchanges"))
                .append(closeTag(SPAN))
                .append(ReportEvidenceMarkup.statusBadge(AtfReportStatus.PASS))
                .append(closeTag(DIV))
                .append(ReportNetworkAggregateMarkup.substepBody(aggregate))
                .append(closeTag(DIV));
    }

    /** Test/composition seam for legacy execution snapshots; typed production callers stay unchanged. */
    public void renderExecution(StringBuilder html, Object execution) {
        renderExecution(html, ReportModelAdapters.execution(execution));
    }

    public void renderValidation(StringBuilder html, ReportExecutionModel.Validation validation) {
        renderValidation(html, validation, false);
    }

    /** Unified UI flow assertions retain a keyboard-operable disclosure, including the compact presentation. */
    public void renderCompactUiValidation(StringBuilder html, ReportExecutionModel.Validation validation) {
        renderValidation(html, validation, true);
    }

    private void renderValidation(StringBuilder html, ReportExecutionModel.Validation validation, boolean compactUi) {
        html.append(openTag(DIV, classAttribute(VALIDATION), attribute(NODE_ROLE, "validation"),
                        attribute(AtfReportHtmlVocabulary.Attribute.EVIDENCE_OWNER,
                                validation.evidenceOwner().attributeValue())))
                .append(openTag(DIV, classAttribute(NODE_TITLE)))
                .append(openTag(SPAN)).append(escape(validation.title())).append(closeTag(SPAN))
                .append(ReportEvidenceMarkup.statusBadge(validation.status()))
                .append(closeTag(DIV));
        renderUiEvidence(html, validation);
        boolean discloseAssertions = !validation.assertions().isEmpty();
        if (discloseAssertions) html.append(ReportEvidenceMarkup.disclosureStart(
                (compactUi ? AtfReportHtmlVocabulary.CssClass.ATF_UI_ASSERTIONS
                        : AtfReportHtmlVocabulary.CssClass.ASSERTION_EVIDENCE).value(),
                AtfReportLabels.assertions(validation.assertions().size())));
        renderAssertions(html, validation.assertions());
        if (discloseAssertions) html.append(ReportEvidenceMarkup.disclosureEnd());
        validation.children().forEach(child -> {
            if (child instanceof ReportExecutionModel.Request request) {
                renderNestedRequest(html, request);
            }
        });
        html.append(closeTag(DIV));
    }

    public void renderAssertions(StringBuilder html, java.util.List<AtfAssertion> assertions) {
        if (!assertions.isEmpty()) {
            html.append(ReportTableViewport.ASSERTIONS.open())
                .append(openTag(TABLE, classAttribute(ASSERTIONS)))
                .append(openTag(THEAD)).append(openTag(TR))
                .append(openTag(TH)).append(AtfReportLabels.ASSERTION).append(closeTag(TH))
                .append(openTag(TH)).append(AtfReportLabels.EXPECTED).append(closeTag(TH))
                .append(openTag(TH)).append(AtfReportLabels.ACTUAL).append(closeTag(TH))
                .append(openTag(TH)).append(AtfReportLabels.RESULT).append(closeTag(TH))
                .append(closeTag(TR)).append(closeTag(THEAD)).append(openTag(TBODY));
            for (AtfAssertion row : assertions) {
                html.append(openTag(TR)).append(openTag(TD)).append(escape(redactor.redactText(row.name())))
                        .append(closeTag(TD)).append(openTag(TD))
                        .append(escape(ExtentReportNodeSupport.display(
                                redactor.redactField(row.name(), row.expected()), settings))).append(closeTag(TD))
                        .append(openTag(TD)).append(escape(ExtentReportNodeSupport.display(
                                redactor.redactField(row.name(), row.actual()), settings)))
                        .append(closeTag(TD)).append(openTag(TD))
                        .append(ReportEvidenceMarkup.statusBadge(row.resultStatus()))
                        .append(closeTag(TD)).append(closeTag(TR));
            }
            html.append(closeTag(TBODY)).append(closeTag(TABLE)).append(closeTag(DIV));
        }
    }

    private void renderUiEvidence(StringBuilder html, ReportExecutionModel.Validation validation) {
        renderUiEvidence(html, validation.uiActions(), validation.screenshots(), validation.actionDetailsMode());
    }

    private void renderUiEvidence(StringBuilder html, List<AtfUiActionRecord> records, List<ReportScreenshot> captures,
                                  ReportUiActionDetailsMode detailsMode) {
        var actions = records.stream().filter(row -> row.source() == AtfUiEvidenceSource.ACTION).toList();
        var observations = records.stream().filter(row -> row.source() == AtfUiEvidenceSource.OBSERVE).toList();
        var screenshots = ExtentReportAssets.distinctScreenshotModels(captures);
        boolean inlineScreenshot = !actions.isEmpty() && screenshots.size() == 1;
        html.append(new ExtentUiActionRenderer(settings, redactor, detailsMode).markup(actions,
                        inlineScreenshot ? screenshots : List.of()))
                .append(new ExtentUiObservationRenderer(settings, redactor).markup(observations));
        for (ReportScreenshot screenshot : inlineScreenshot ? List.<ReportScreenshot>of() : screenshots) {
            html.append(ExtentReportAssets.base64ScreenshotLink(
                    AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT_LINK.value(),
                    redactor.redactText(screenshot.title()), screenshot.base64Png()));
        }
    }

    private void renderNestedRequest(StringBuilder html, ReportExecutionModel.Request request) {
        html.append(openTag(DIV, classAttribute(AtfReportHtmlVocabulary.CssClass.REQUEST),
                        attribute(AtfReportHtmlVocabulary.Attribute.NODE_ROLE, "request")))
                .append(openTag(DIV, classAttribute(AtfReportHtmlVocabulary.CssClass.NODE_TITLE)))
                .append(openTag(SPAN))
                .append(escape(ExtentReportNodeSupport.renderRequestTitle(
                        request.title(), request.method(), request.path())))
                .append(ReportEvidenceMarkup.statusBadge(request.status()))
                .append(closeTag(DIV));
        renderRequestContext(html, request);
        renderNetworkAggregateSubstep(html, request.networkAggregate());
        request.executions().forEach(execution -> renderExecution(html, execution));
        request.children().forEach(child -> {
            if (child instanceof ReportExecutionModel.Validation validation) {
                renderValidation(html, validation);
            }
        });
        html.append(closeTag(DIV));
    }
}

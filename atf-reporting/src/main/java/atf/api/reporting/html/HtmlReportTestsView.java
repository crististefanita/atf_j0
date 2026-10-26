package atf.api.reporting.html;

import atf.api.reporting.configuration.ReportingSettings;
import atf.api.reporting.contract.AtfReportStatus;
import atf.api.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.api.reporting.html.evidence.HtmlReportEvidenceView;
import atf.api.reporting.markup.ExtentReportNodeSupport;
import atf.api.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.api.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.api.reporting.evidence.diagnostic.ReportPhaseDiagnosticMarkup;
import atf.api.reporting.model.ReportExecutionModel;
import atf.api.reporting.security.SensitiveDataRedactor;

import java.util.List;
import java.util.Objects;

import static atf.api.reporting.html.HtmlReportMarkupSupport.escape;
import static atf.api.reporting.html.HtmlReportMarkupSupport.phaseKey;

/** Renders the Tests projection; evidence details are delegated to one view. */
public final class HtmlReportTestsView {
    private final SensitiveDataRedactor redactor;
    private final HtmlReportEvidenceView evidence;

    HtmlReportTestsView() {
        this(ReportingSettings.defaults(), new SensitiveDataRedactor());
    }

    HtmlReportTestsView(SensitiveDataRedactor redactor) {
        this(ReportingSettings.defaults(), redactor);
    }

    HtmlReportTestsView(ReportingSettings settings, SensitiveDataRedactor redactor) {
        Objects.requireNonNull(settings, "settings");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.evidence = new HtmlReportEvidenceView(settings, this.redactor);
    }

    String render(ReportExecutionModel snapshot) {
        List<ReportExecutionModel.Scenario> scenarios = snapshot.scenarios();
        var html = new StringBuilder();
        html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SECTION,
                        AtfReportHtmlVocabulary.idAttribute(
                                AtfReportHtmlVocabulary.FallbackTab.TESTS.value()),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TAB_PANEL)))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.LAYOUT)))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.ASIDE))
                .append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.H2,
                        AtfReportHtmlVocabulary.FallbackTab.TESTS.label()));
        scenarios.forEach(scenario -> html.append(scenarioLink(scenario)));
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.ASIDE))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TEST_CONTENT)));
        scenarios.forEach(scenario -> renderScenario(html, scenario));
        return html.append(AtfReportHtmlVocabulary.closeTag(
                                AtfReportHtmlVocabulary.Tag.DIV))
                .append(AtfReportHtmlVocabulary.closeTag(
                                AtfReportHtmlVocabulary.Tag.DIV))
                .append(AtfReportHtmlVocabulary.closeTag(
                                AtfReportHtmlVocabulary.Tag.SECTION))
                .toString();
    }

    private String scenarioLink(ReportExecutionModel.Scenario scenario) {
        String label = escape(scenario.scenario())
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SMALL,
                escape(scenario.testClass()));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A, label,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LINK.value(),
                        AtfReportHtmlVocabulary.CssClass.STATUS.value(),
                        AtfReportHtmlVocabulary.statusClass(scenario.status())),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF,
                        "#" + scenario.id()));
    }

    private void renderScenario(StringBuilder html, ReportExecutionModel.Scenario scenario) {
        html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.ARTICLE,
                        AtfReportHtmlVocabulary.idAttribute(scenario.id()),
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.SCENARIO)))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.SCENARIO_TITLE)))
                .append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.H2,
                        escape(scenario.scenario())))
                .append(status(scenario.status()))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        scenario.phases().forEach(phase -> html.append(ReportPhaseDiagnosticMarkup.render(
                ReportPhaseDiagnostic.from(phase.name(), phase.details(), phase.status()), redactor)));
        scenario.phases().forEach(phase -> renderPhase(html, phase));
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.ARTICLE));
    }

    private void renderPhase(StringBuilder html, ReportExecutionModel.Phase phase) {
        html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SECTION,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.PHASE.value(),
                                AtfReportHtmlVocabulary.stateClass("phase-", phase.status())),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.NODE_ROLE, "phase"),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.PHASE_NAME,
                                phaseKey(phase.name()))))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.H3))
                .append(escape(phase.name()))
                .append(status(phase.status()))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.H3));
        phase.children().forEach(child -> renderNode(html, child));
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SECTION));
    }

    private String status(AtfReportStatus status) {
        return ReportEvidenceMarkup.statusBadge(status);
    }

    private void renderNode(StringBuilder html, ReportExecutionModel.Node node) {
        if (node instanceof ReportExecutionModel.Request request) {
            renderRequest(html, request);
        } else if (node instanceof ReportExecutionModel.Validation validation) {
            evidence.renderValidation(html, validation);
        } else {
            throw new IllegalStateException("Unsupported report node: " + node.getClass().getName());
        }
    }

    private void renderRequest(StringBuilder html, ReportExecutionModel.Request request) {
        html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.REQUEST),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.NODE_ROLE, "request")))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.NODE_TITLE)))
                .append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                        escape(ExtentReportNodeSupport.renderRequestTitle(
                                request.title(), request.method(), request.path()))))
                .append(status(request.status()))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        if (atf.api.reporting.evidence.EvidenceTableFormatter.isCompactUiFlow(request.details())) {
            evidence.renderUiFlowProvenance(html, request);
        } else {
            evidence.renderRequestDetails(html, request);
            evidence.renderResponseDetails(html, request);
        }
        evidence.renderNetworkAggregateSubstep(html, request.networkAggregate());
        request.executions().forEach(execution -> evidence.renderExecution(html, execution));
        request.children().forEach(child -> renderNode(html, child));
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
    }
}

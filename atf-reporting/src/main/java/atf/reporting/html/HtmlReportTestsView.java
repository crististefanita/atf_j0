package atf.reporting.html;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.html.evidence.HtmlReportEvidenceView;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.evidence.diagnostic.ReportPhaseDiagnosticMarkup;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.security.SensitiveDataRedactor;

import java.util.List;
import java.util.Objects;
import atf.reporting.model.timing.ReportPhaseDurations;
import atf.reporting.evidence.timing.ReportPhaseTimingMarkup;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;
import static atf.reporting.html.HtmlReportMarkupSupport.phaseKey;

/** Renders the Tests projection; evidence details are delegated to one view. */
public final class HtmlReportTestsView {
    private final SensitiveDataRedactor redactor;
    private final HtmlReportEvidenceView evidence;
    private final int assertionPreviewLimit;
    private final ReportingSettings settings;

    HtmlReportTestsView() {
        this(ReportingSettings.defaults(), new SensitiveDataRedactor());
    }

    HtmlReportTestsView(SensitiveDataRedactor redactor) {
        this(ReportingSettings.defaults(), redactor);
    }

    HtmlReportTestsView(ReportingSettings settings, SensitiveDataRedactor redactor) {
        Objects.requireNonNull(settings, "settings");
        this.settings = settings;
        assertionPreviewLimit = settings.assertionMaxDisplayCharacters();
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
        var duration = ReportPhaseDurations.scenario(scenario);
        if (duration.isPresent()) label += ReportPhaseTimingMarkup.badge(duration.getAsLong(), "Sum of measured phase execution");
        label += status(scenario.status());
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A, label,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.TEST_LINK.value()),
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
                .append(ReportPhaseDurations.scenario(scenario).isPresent()
                        ? ReportPhaseTimingMarkup.badge(ReportPhaseDurations.scenario(scenario).getAsLong(),
                                "Sum of measured phase execution") : "")
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        scenario.phases().forEach(phase -> html.append(ReportPhaseDiagnosticMarkup.render(
                ReportPhaseDiagnostic.from(phase.name(), phase.details(), phase.status()), redactor, assertionPreviewLimit)));
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
                .append(phase.timing().map(value -> ReportPhaseTimingMarkup.phase(value, phase.publicationMillis())).orElse(""))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.H3));
        phase.children().forEach(child -> renderNode(html, child, phase.timing().isPresent()));
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SECTION));
    }

    private String status(AtfReportStatus status) {
        return ReportEvidenceMarkup.statusBadge(status);
    }

    private void renderNode(StringBuilder html, ReportExecutionModel.Node node, boolean capturedPhase) {
        if (node instanceof ReportExecutionModel.Request request) {
            renderRequest(html, request, capturedPhase);
        } else if (node instanceof ReportExecutionModel.Validation validation) {
            evidence.renderValidation(html, validation);
        } else if (node instanceof ReportExecutionModel.AssertionStep step) {
            html.append(ReportEvidenceMarkup.assertionStepStart(step.title(), step.status()));
            if (capturedPhase) html.append(ReportPhaseTimingMarkup.publicationLabel());
            evidence.renderAssertions(html, step.assertions());
            html.append(ReportEvidenceMarkup.disclosureEnd());
        } else if (node instanceof ReportExecutionModel.ObservationStep observation) {
            html.append(atf.reporting.evidence.markup.ReportObservationMarkup.render(observation.value(), settings, redactor));
        } else {
            throw new IllegalStateException("Unsupported report node: " + node.getClass().getName());
        }
    }

    private void renderRequest(StringBuilder html, ReportExecutionModel.Request request, boolean capturedPhase) {
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
                .append(capturedTransport(request, capturedPhase))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        evidence.renderRequestContext(html, request);
        evidence.renderNetworkAggregateSubstep(html, request.networkAggregate());
        request.executions().forEach(execution -> evidence.renderExecution(html, execution));
        request.children().forEach(child -> {
            if (!EvidenceTableFormatter.uiFlowProvenanceFields(request.details()).isEmpty()
                    && child instanceof ReportExecutionModel.Validation validation) {
                evidence.renderCompactUiValidation(html, validation);
            } else {
                renderNode(html, child, capturedPhase);
            }
        });
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
    }
    private static String capturedTransport(ReportExecutionModel.Request request, boolean capturedPhase) {
        if (!capturedPhase) return "";
        var measured = request.executions().stream().map(atf.reporting.model.ReportExecutionEvidence::totalDuration)
                .filter(Objects::nonNull).toList();
        if (measured.isEmpty()) return "";
        long millis = atf.reporting.duration.AtfDurationFormatter.elapsedMillis(
                measured.stream().reduce(java.time.Duration.ZERO, java.time.Duration::plus));
        return ReportPhaseTimingMarkup.transportBadge(millis);
    }

}

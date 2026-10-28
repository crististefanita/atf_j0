package atf.reporting.evidence.ui;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.table.ReportTableViewport;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.security.SensitiveDataRedactor;
import com.aventstack.extentreports.ExtentTest;

import java.util.List;

import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.close;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.escape;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.hasFailure;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.open;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.observationSummary;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.publish;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.resultSpan;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.statusKey;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.tableHeader;

/** Renders only reporter observation/checkpoint evidence. */
public final class ExtentUiObservationRenderer {
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor;

    public ExtentUiObservationRenderer(ReportingSettings settings) {
        this(settings, new SensitiveDataRedactor());
    }

    public ExtentUiObservationRenderer(ReportingSettings settings, SensitiveDataRedactor redactor) {
        this.settings = settings;
        this.redactor = java.util.Objects.requireNonNull(redactor, "redactor");
    }

    public void render(ExtentTest node, List<AtfUiActionRecord> observations) {
        if (observations == null || observations.isEmpty()) return;
        publish(node, markup(observations), hasFailure(observations));
    }

    /** Renderer-neutral markup shared with the fallback HTML projection. */
    public String markup(List<AtfUiActionRecord> observations) {
        if (observations == null || observations.isEmpty()) return "";
        boolean failed = hasFailure(observations);
        String summary = observationSummary(observations, failed);
        var html = new StringBuilder(open(AtfReportHtmlVocabulary.Tag.DETAILS,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_UI_OBSERVATIONS),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.EVIDENCE_STATUS, statusKey(failed)),
                        AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ASSERTION_PREVIEW_LIMIT,
                                Integer.toString(settings.assertionMaxDisplayCharacters()))))
                .append(open(AtfReportHtmlVocabulary.Tag.SUMMARY,
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.ARIA_LABEL, summary)))
                .append(escape(summary)).append(close(AtfReportHtmlVocabulary.Tag.SUMMARY))
                .append(ReportTableViewport.ACTIONS.open())
                .append(open(AtfReportHtmlVocabulary.Tag.TABLE,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_TABLE,
                                AtfReportHtmlVocabulary.CssClass.ATF_UI_OBSERVATION_TABLE)))
                .append(tableHeader(AtfReportLabels.ACTION, AtfReportLabels.TARGET,
                        AtfReportLabels.EXPECTED, AtfReportLabels.OBSERVED,
                        AtfReportLabels.RESULT))
                .append(open(AtfReportHtmlVocabulary.Tag.TBODY));
        for (AtfUiActionRecord observation : observations) {
            html.append(ExtentUiEvidenceMarkup.actionRowStart()).append(escape(redactor.redactText(observation.operation())))
                    .append(close(AtfReportHtmlVocabulary.Tag.SPAN))
                    .append(open(AtfReportHtmlVocabulary.Tag.SPAN,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_NUMBER)))
                    .append("#").append(observation.sequence())
                    .append(close(AtfReportHtmlVocabulary.Tag.SPAN))
                    .append(close(AtfReportHtmlVocabulary.Tag.TD))
                    .append(open(AtfReportHtmlVocabulary.Tag.TD)).append(escape(redactor.redactText(observation.target())))
                    .append(close(AtfReportHtmlVocabulary.Tag.TD))
                    .append(open(AtfReportHtmlVocabulary.Tag.TD))
                    .append(escape(ExtentReportNodeSupport.display(redactor.redactText(observation.expected()), settings)))
                    .append(close(AtfReportHtmlVocabulary.Tag.TD))
                    .append(open(AtfReportHtmlVocabulary.Tag.TD))
                    .append(escape(ExtentReportNodeSupport.display(redactor.redactText(observation.observed()), settings)))
                    .append(close(AtfReportHtmlVocabulary.Tag.TD))
                    .append(open(AtfReportHtmlVocabulary.Tag.TD)).append(resultSpan(observation))
                    .append(close(AtfReportHtmlVocabulary.Tag.TD))
                    .append(close(AtfReportHtmlVocabulary.Tag.TR));
        }
        return html.append(close(AtfReportHtmlVocabulary.Tag.TBODY))
                .append(close(AtfReportHtmlVocabulary.Tag.TABLE))
                .append(close(AtfReportHtmlVocabulary.Tag.DIV))
                .append(close(AtfReportHtmlVocabulary.Tag.DETAILS)).toString();
    }
}

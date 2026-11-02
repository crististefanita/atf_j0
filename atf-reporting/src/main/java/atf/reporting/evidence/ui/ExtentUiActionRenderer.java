package atf.reporting.evidence.ui;
import atf.reporting.evidence.policy.UiActionScreenshotPolicy;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.table.ReportTableViewport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.ui.model.AtfUiActionRecord;
import com.aventstack.extentreports.ExtentTest;

import java.util.List;

import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.actionSummary;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.close;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.escape;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.hasFailure;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.open;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.publish;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.statusKey;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.tableHeader;

/** Renders captured UI actions independently of the consumer transport. */
public final class ExtentUiActionRenderer {
    private final ReportingSettings settings;
    private final ReportUiActionDetailsMode detailsMode;
    private final SensitiveDataRedactor redactor;

    public ExtentUiActionRenderer(ReportingSettings settings) {
        this(settings, new SensitiveDataRedactor(), ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }

    public ExtentUiActionRenderer(ReportingSettings settings,
                                  ReportUiActionDetailsMode detailsMode) {
        this(settings, new SensitiveDataRedactor(), detailsMode);
    }

    public ExtentUiActionRenderer(ReportingSettings settings, SensitiveDataRedactor redactor,
                                  ReportUiActionDetailsMode detailsMode) {
        this.settings = settings;
        this.detailsMode = detailsMode;
        this.redactor = redactor == null ? new SensitiveDataRedactor() : redactor;
    }

    public void render(ExtentTest node, List<AtfUiActionRecord> actions,
                       List<ReportScreenshot> screenshots) {
        if (actions == null || actions.isEmpty()) return;
        publish(node, markup(actions, screenshots), hasFailure(actions));
    }

    /** Renderer-neutral markup shared with the fallback HTML projection. */
    public String markup(List<AtfUiActionRecord> actions, List<ReportScreenshot> screenshots) {
        if (actions == null || actions.isEmpty()) return "";
        List<ReportScreenshot> distinctScreenshots = ExtentReportAssets.distinctScreenshotModels(screenshots);
        UiActionScreenshotPolicy.validate(actions, distinctScreenshots);
        StringBuilder html = actionDisclosure(actions);
        for (ReportScreenshot screenshot : distinctScreenshots) {
            html.append(open(AtfReportHtmlVocabulary.Tag.DIV,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT)))
                    .append(ExtentReportAssets.base64ScreenshotLink(
                            AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT_LINK.value(),
                            redactor.redactText(screenshot.title()), screenshot.base64Png()))
                    .append(close(AtfReportHtmlVocabulary.Tag.DIV));
        }
        return html.append(close(AtfReportHtmlVocabulary.Tag.DETAILS)).toString();
    }

    public void renderAsMedia(ExtentTest node, List<AtfUiActionRecord> actions) {
        if (actions == null || actions.isEmpty()) return;
        publish(node, markup(actions, List.of()), hasFailure(actions));
    }

    private StringBuilder actionDisclosure(List<AtfUiActionRecord> actions) {
        if (detailsMode == ReportUiActionDetailsMode.SUMMARY_ONLY) {
            boolean failed = hasFailure(actions);
            return new StringBuilder(open(AtfReportHtmlVocabulary.Tag.DETAILS,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SEQUENCE),
                            AtfReportHtmlVocabulary.attribute(
                                    AtfReportHtmlVocabulary.Attribute.EVIDENCE_STATUS, statusKey(failed)),
                            AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ASSERTION_PREVIEW_LIMIT,
                                    Integer.toString(settings.assertionMaxDisplayCharacters()))))
                    .append(open(AtfReportHtmlVocabulary.Tag.SUMMARY))
                    .append(actionSummary(actions, failed))
                    .append(close(AtfReportHtmlVocabulary.Tag.SUMMARY));
        }
        return actionTable(actions)
                .append(close(AtfReportHtmlVocabulary.Tag.TBODY))
                .append(close(AtfReportHtmlVocabulary.Tag.TABLE))
                .append(close(AtfReportHtmlVocabulary.Tag.DIV));
    }

    private StringBuilder actionTable(List<AtfUiActionRecord> actions) {
        boolean failed = hasFailure(actions);
        var html = new StringBuilder(open(AtfReportHtmlVocabulary.Tag.DETAILS,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SEQUENCE),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.EVIDENCE_STATUS, statusKey(failed)),
                        AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ASSERTION_PREVIEW_LIMIT,
                                Integer.toString(settings.assertionMaxDisplayCharacters()))))
                .append(open(AtfReportHtmlVocabulary.Tag.SUMMARY))
                .append(actionSummary(actions, failed))
                .append(close(AtfReportHtmlVocabulary.Tag.SUMMARY))
                .append(ReportTableViewport.ACTIONS.open())
                .append(open(AtfReportHtmlVocabulary.Tag.TABLE,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_TABLE)))
                .append(tableHeader(AtfReportLabels.ACTION, AtfReportLabels.TARGET,
                        AtfReportLabels.EXPECTED, AtfReportLabels.OBSERVED,
                        AtfReportLabels.TIME, AtfReportLabels.RESULT))
                .append(open(AtfReportHtmlVocabulary.Tag.TBODY));
        for (AtfUiActionRecord action : actions) {
            html.append(UiEvidenceRowMarkup.start(action, settings, redactor))
                    .append(open(AtfReportHtmlVocabulary.Tag.TD))
                    .append(open(AtfReportHtmlVocabulary.Tag.SPAN,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_TIME)))
                    .append(escape(action.durationText()))
                    .append(close(AtfReportHtmlVocabulary.Tag.SPAN))
                    .append(close(AtfReportHtmlVocabulary.Tag.TD))
                    .append(UiEvidenceRowMarkup.end(action));
        }
        return html;
    }
}

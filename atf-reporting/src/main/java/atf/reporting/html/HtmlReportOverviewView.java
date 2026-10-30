package atf.reporting.html;
import atf.reporting.markup.AtfReportLabels;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.model.ReportExecutionModel;

import static atf.reporting.html.HtmlReportMarkupSupport.escape;

/** Renders only the fallback Overview aggregate. */
public final class HtmlReportOverviewView {
    String render(ReportExecutionModel snapshot) {
        return render(snapshot, atf.reporting.configuration.ReportEnvironment.reportingSettings(
                atf.reporting.configuration.ReportingSettings.defaults()).systemDisplayValues());
    }

    String render(ReportExecutionModel snapshot, java.util.Map<String, String> system) {
        var overview = snapshot.overview();
        String summary = metric("Tests", overview.tests()) + metric("Passed", overview.passed())
                + metric("Failed", overview.failed()) + metric("Skipped", overview.skipped());
        var duration = atf.reporting.model.timing.ReportPhaseDurations.total(snapshot.scenarios());
        if (duration.isPresent()) summary += AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, "Phase execution sum")
                        + atf.reporting.evidence.timing.ReportPhaseTimingMarkup.badge(duration.getAsLong(),
                                "Sum of measured phase execution; not suite wall-clock time"),
                AtfReportHtmlVocabulary.classAttribute(AtfReportHtmlVocabulary.CssClass.METRIC));
        String body = HtmlReportFallbackViewSupport.heading(
                        AtfReportHtmlVocabulary.FallbackTab.OVERVIEW)
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV, summary,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.SUMMARY))
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.P,
                AtfReportLabels.OVERVIEW_DEFAULT_COPY,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.CONTRACT));
        return HtmlReportFallbackViewSupport.sectionStart(
                        AtfReportHtmlVocabulary.FallbackTab.OVERVIEW)
                + body + systemTable(system)
                + HtmlReportFallbackViewSupport.sectionEnd();
    }

    private static String systemTable(java.util.Map<String, String> values) {
        var rows = new StringBuilder();
        rows.append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TR,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TH, "Name")
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TH, "Value")));
        values.forEach((name, value) -> rows.append(AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TR,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TD, escape(name))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TD, escape(value)))));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.H3, "System")
                + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.TABLE, rows.toString());
    }

    private static String metric(String label, long value) {
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN, escape(label))
                        + AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STRONG,
                        Long.toString(value)),
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.METRIC));
    }
}

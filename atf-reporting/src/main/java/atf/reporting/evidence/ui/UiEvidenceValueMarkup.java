package atf.reporting.evidence.ui;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.security.SensitiveDataRedactor;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.*;

/** Short values remain plain cells; long values disclose their complete, already-redacted value. */
final class UiEvidenceValueMarkup {
    private UiEvidenceValueMarkup() { }
    static String render(String value, ReportingSettings settings, SensitiveDataRedactor redactor) {
        String safe = redactor.redactText(value == null ? "" : value);
        String preview = ExtentReportNodeSupport.display(safe, settings);
        if (safe.equals(preview)) return text(safe);
        return ReportEvidenceMarkup.disclosureStart("atf-ui-value", preview)
                + element(Tag.PRE, ReportEvidenceMarkup.preformattedText(safe), classAttribute(CssClass.JSON_CODE, CssClass.OBSERVATION_BODY), attribute(Attribute.TABINDEX, "0"))
                + ReportEvidenceMarkup.disclosureEnd();
    }
    private static String text(String value) { return escapeHtml(value).replace("\r", "&#13;"); }
}

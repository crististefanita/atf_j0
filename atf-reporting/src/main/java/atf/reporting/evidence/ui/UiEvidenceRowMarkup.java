package atf.reporting.evidence.ui;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.ui.model.AtfUiActionRecord;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.*;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.*;

/** One redaction and cell projection boundary for captured ACTION and OBSERVE records. */
final class UiEvidenceRowMarkup {
    private UiEvidenceRowMarkup() { }
    static StringBuilder start(AtfUiActionRecord row, ReportingSettings settings, SensitiveDataRedactor redactor) {
        return new StringBuilder(actionRowStart()).append(text(row.operation(), redactor)).append(close(Tag.SPAN))
                .append(element(Tag.SPAN, "#" + row.sequence(), classAttribute(CssClass.ATF_UI_ACTION_NUMBER)))
                .append(close(Tag.TD)).append(element(Tag.TD, text(row.target(), redactor)))
                .append(element(Tag.TD, UiEvidenceValueMarkup.render(row.expected(), settings, redactor)))
                .append(element(Tag.TD, UiEvidenceValueMarkup.render(row.observed(), settings, redactor)));
    }
    static String end(AtfUiActionRecord row) { return element(Tag.TD, resultSpan(row)) + close(Tag.TR); }
    private static String text(String value, SensitiveDataRedactor redactor) { return escape(redactor.redactText(value)); }
}

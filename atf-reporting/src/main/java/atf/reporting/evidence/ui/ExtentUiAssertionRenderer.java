package atf.reporting.evidence.ui;
import atf.reporting.contract.evidence.ReportEvidenceOwner;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.security.SensitiveDataRedactor;
import com.aventstack.extentreports.ExtentTest;

import java.util.Collection;

import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.close;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.open;
import static atf.reporting.evidence.ui.ExtentUiEvidenceMarkup.publish;

/** Renders only assertion-table evidence for a UI checkpoint. */
public final class ExtentUiAssertionRenderer {
    private final ReportingSettings settings;
    private final SensitiveDataRedactor redactor;

    public ExtentUiAssertionRenderer(ReportingSettings settings, SensitiveDataRedactor redactor) {
        this.settings = settings;
        this.redactor = redactor;
    }

    public void render(ExtentTest node, Collection<AtfAssertion> assertions) {
        render(node, assertions, ReportEvidenceOwner.UNSPECIFIED);
    }

    public void render(ExtentTest node, Collection<AtfAssertion> assertions, ReportEvidenceOwner owner) {
        if (assertions == null || assertions.isEmpty()) return;
        var table = new String[assertions.size() + 1][4];
        table[0] = new String[]{AtfReportLabels.ASSERTION, AtfReportLabels.EXPECTED,
                AtfReportLabels.ACTUAL, AtfReportLabels.RESULT};
        int index = 0;
        for (AtfAssertion row : assertions) {
            table[++index] = new String[]{
                    redactor.redactText(row.name()),
                    ExtentReportNodeSupport.display(redactor.redactField(row.name(), row.expected()), settings),
                    ExtentReportNodeSupport.display(redactor.redactField(row.name(), row.actual()), settings),
                    ExtentReportNodeSupport.resultMarker(row.resultStatus())};
        }
        boolean failed = assertions.stream().anyMatch(row -> row != null
                && ExtentUiEvidenceMarkup.isFailure(row.resultStatus()));
        String markup = AtfReportMarkup.table(AtfReportHtmlVocabulary.classList(
                AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                AtfReportHtmlVocabulary.CssClass.HELM_VALIDATION_TABLE), table).getMarkup();
        publish(node, open(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.ATF_UI_ASSERTIONS),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.EVIDENCE_OWNER, owner.attributeValue()),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.EVIDENCE_STATUS,
                                ExtentUiEvidenceMarkup.statusKey(failed)),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.ASSERTION_PREVIEW_LIMIT,
                                Integer.toString(settings.assertionMaxDisplayCharacters())))
                + markup + close(AtfReportHtmlVocabulary.Tag.DIV), failed);
    }
}

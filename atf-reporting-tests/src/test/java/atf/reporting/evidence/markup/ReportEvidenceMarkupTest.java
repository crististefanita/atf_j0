package atf.reporting.evidence.markup;

import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportEvidenceMarkupTest {
    @Test
    void disclosureUsesOneSharedEscapedSemanticShell() {
        String title = "Response <details> & \"headers\"";

        String markup = ReportEvidenceMarkup.disclosure("response-evidence", title, "body");

        assertTrue(markup.startsWith("<details class=\"step-detail response-evidence\">")
                        && markup.endsWith("</div></details>"),
                "the shared primitive must own the disclosure shell");
        assertTrue(markup.contains("aria-expanded=\"false\""));
        assertTrue(markup.contains("class=\"atf-disclosure-glyph\""));
        assertTrue(markup.contains("Response &lt;details&gt; &amp; &quot;headers&quot;"),
                "the visible title must be escaped once");
        assertTrue(markup.contains("aria-label=\"Response &lt;details&gt; &amp; &quot;headers&quot;\""));
    }

    @Test
    void composedStartAndEndRemainByteEquivalentToConvenienceMethod() {
        assertEquals(
                ReportEvidenceMarkup.disclosure("request-evidence", "Request details", "payload"),
                ReportEvidenceMarkup.disclosureStart("request-evidence", "Request details")
                        + "payload"
                        + ReportEvidenceMarkup.disclosureEnd());
    }

    @Test
    void typedDisclosureOwnsEvidenceMetadataWithoutDuplicatingTheShell() {
        String markup = ReportEvidenceMarkup.disclosure(
                ReportEvidenceDisclosure.withEvidenceStatus(
                        "setup-evidence atf-setup-evidence", "Setup evidence · 1 check",
                        "body", "report-setup", AtfReportStatus.PASS));

        assertTrue(markup.startsWith("<details class=\"step-detail setup-evidence atf-setup-evidence\""));
        assertTrue(markup.contains("data-atf-evidence-type=\"report-setup\""));
        assertTrue(markup.contains("data-atf-evidence-status=\"pass\""));
        assertTrue(markup.endsWith("</div></details>"));
    }

    @Test
    void statusBadgeNormalizesNullAndUsesTheCanonicalVocabulary() {
        assertEquals("<span class=\"status status-info\">Info</span>",
                ReportEvidenceMarkup.statusBadge(null));
        assertEquals("<span class=\"status status-fail\">Fail</span>",
                ReportEvidenceMarkup.statusBadge(AtfReportStatus.FAIL));
    }
}

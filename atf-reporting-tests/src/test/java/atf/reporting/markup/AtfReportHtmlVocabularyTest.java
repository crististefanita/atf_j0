package atf.reporting.markup;
import atf.testkit.AtfUnitTest;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportHtmlVocabularyTest {
    @Test
    void keepsRepeatedEvidenceTokensTypedAndDeterministic() {
        assertAll(
                () -> assertEquals("class=\"step-detail request-evidence\"",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.STEP_DETAIL,
                                AtfReportHtmlVocabulary.CssClass.REQUEST_EVIDENCE)),
                () -> assertEquals("class=\"status status-fail\"",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.statusClasses(AtfReportStatus.FAIL))),
                () -> assertTrue(AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.NODE_ROLE, "validation")
                        .contains("data-atf-node-role=\"validation\"")),
                () -> assertEquals("id=\"atf-fails-view\"",
                        AtfReportHtmlVocabulary.idAttribute(
                                AtfReportHtmlVocabulary.ViewId.FAILS)),
                () -> assertEquals("data-atf-evidence-type=\"report-setup\"",
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.EVIDENCE_TYPE, "report-setup")));
    }

    @Test
    void mapsEveryReportStatusToItsNamedCssState() {
        assertAll(
                () -> assertEquals("status-info", AtfReportHtmlVocabulary.statusClass(AtfReportStatus.INFO)),
                () -> assertEquals("status-pass", AtfReportHtmlVocabulary.statusClass(AtfReportStatus.PASS)),
                () -> assertEquals("status-warning", AtfReportHtmlVocabulary.statusClass(AtfReportStatus.WARNING)),
                () -> assertEquals("status-fail", AtfReportHtmlVocabulary.statusClass(AtfReportStatus.FAIL)),
                () -> assertEquals("status-skip", AtfReportHtmlVocabulary.statusClass(AtfReportStatus.SKIP)),
                () -> assertEquals("Warning", AtfReportHtmlVocabulary.statusLabel(AtfReportStatus.WARNING)),
                () -> assertEquals("phase-fail",
                        AtfReportHtmlVocabulary.stateClass("phase-", AtfReportStatus.FAIL)));
    }

    @Test
    void keepsFallbackNavigationTokensTyped() {
        assertAll(
                () -> assertEquals("data-tab=\"tests\"",
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.FALLBACK_TAB,
                                AtfReportHtmlVocabulary.FallbackTab.TESTS.value())),
                () -> assertEquals("Overview",
                        AtfReportHtmlVocabulary.FallbackTab.OVERVIEW.label()),
                () -> assertEquals("class=\"tab\"",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TAB)));
    }

    @Test
    void serializesTypedHtmlShellTokens() {
        assertEquals("<button class=\"tab\" data-tab=\"tests\">",
                AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.BUTTON,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TAB),
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.FALLBACK_TAB, "tests")));
        assertEquals("</button>", AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.BUTTON));
        assertEquals("<div class=\"technical helm-api-classification\">",
                AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TECHNICAL,
                                AtfReportHtmlVocabulary.CssClass.HELM_API_CLASSIFICATION)));
        assertEquals("aria-expanded=\"false\"",
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.ARIA_EXPANDED, "false"));
        assertEquals("<p class=\"contract\">fallback</p>",
                AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.P, "fallback",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.CONTRACT)));
        assertEquals("<meta charset=\"utf-8\">",
                AtfReportHtmlVocabulary.voidElement(AtfReportHtmlVocabulary.Tag.META,
                        AtfReportHtmlVocabulary.attribute(
                                AtfReportHtmlVocabulary.Attribute.CHARSET, "utf-8")));
    }

    @Test
    void centralizesHtmlEscapingForTextAndAttributeValues() {
        assertEquals("&amp;&quot;&lt;ATF&gt;&#39;",
                AtfReportHtmlVocabulary.escapeHtml("&\"<ATF>'"));
        assertEquals("id=\"&lt;report&gt;\"",
                AtfReportHtmlVocabulary.idAttribute("<report>"));
    }

    @Test
    void keepsFallbackProjectionClassesInTheTypedVocabulary() {
        assertAll(
                () -> assertEquals("class=\"tag-list\"",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.TAG_LIST)),
                () -> assertEquals("class=\"metric\"",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.METRIC)),
                () -> assertEquals("class=\"fail-list\"",
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.FAIL_LIST)));
    }
}

package atf.reporting.ui.model;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportSectionModelTest {
    @Test
    void defaultExtentModelCoversTabsAndEvidenceSurfaces() {
        String html = "id=\"dashboard-view\" id=\"test-view\" id=\"category-view\" "
                + "id=\"atf-fails-view\" data-atf-evidence-type=\"report-setup\" "
                + "UI validation class=\"atf-ui-action-sequence\"";

        assertAll(
                () -> assertTrue(ReportSectionModel.extentUiDefaults().stream()
                        .allMatch(section -> section.isPresentIn(html))),
                () -> assertEquals("UI validation",
                        ReportSectionModel.Section.UI_VALIDATION.displayName()),
                () -> assertEquals("id=\"test-view\"", ReportSectionMarker.id(
                        AtfReportHtmlVocabulary.ViewId.TESTS)
                        .token()),
                () -> assertEquals("data-atf-evidence-type=\"ui-validation\"",
                        ReportSectionMarker.attribute(
                        AtfReportHtmlVocabulary.Attribute.EVIDENCE_TYPE,
                        "ui-validation").token()),
                () -> assertTrue(ReportSectionModel.Section.TESTS.viewId()
                        == AtfReportHtmlVocabulary.ViewId.TESTS),
                () -> assertFalse(new ReportSectionModel(
                        ReportSectionModel.Section.TESTS, "Tests", java.util.List.of(
                                java.util.List.of(ReportSectionMarker.text("missing"))))
                        .isPresentIn(html)));
    }
}

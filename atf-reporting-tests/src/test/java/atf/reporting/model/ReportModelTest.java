package atf.reporting.model;
import atf.testkit.AtfUnitTest;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.ui.model.ReportDocumentModel;
import atf.reporting.ui.model.ReportTabModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportModelTest {
    @Test
    void evidenceModelsAreImmutableAndKeepDataSeparateFromMarkup() {
        var field = new ReportField("HTTP STATUS", "200");
        var json = new ReportJsonBody("RESPONSE JSON", "{\"id\":1}");

        assertEquals("HTTP STATUS", field.name());
        assertEquals("200", field.value());
        assertTrue(json.isPresent());
        assertThrows(IllegalArgumentException.class, () -> new ReportField(" ", "value"));
        assertThrows(IllegalArgumentException.class, () -> new ReportJsonBody(" ", "{}"));
    }

    @Test
    void fallbackDocumentHasOneTypedOwnerForEachNavigableSurface() {
        var document = ReportDocumentModel.fallback();

        assertEquals(List.of("tests", "category", "fails", "overview"),
                document.tabs().stream().map(ReportTabModel::panelId).toList());
        assertEquals("Overview", document.tab(AtfReportHtmlVocabulary.FallbackTab.OVERVIEW).label());
        assertEquals(6, document.sections().size());
        assertThrows(IllegalArgumentException.class, () -> new ReportDocumentModel(
                List.of(new ReportTabModel(AtfReportHtmlVocabulary.FallbackTab.TESTS),
                        new ReportTabModel(AtfReportHtmlVocabulary.FallbackTab.TESTS)), List.of()));
    }
}

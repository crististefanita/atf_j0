package atf.reporting.ui.values;

import atf.reporting.support.values.UiFullValueFixture;
import atf.reporting.contract.*;
import atf.reporting.evidence.ui.*;
import atf.reporting.configuration.ReportingSettings;
import org.junit.jupiter.api.Test;
import atf.reporting.ui.model.*;
import atf.testkit.AtfComponentTest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The oracle owns complete values and suffix failures; renderer summaries cannot satisfy it. */
@AtfComponentTest
class UiFullValueComponentTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void bothSourcesRetainCompleteValuesAndTheirExactOwner(boolean extent) throws Exception {
        Path path = UiFullValueFixture.publish(extent);
        String html = Files.readString(path);
        requireComplete(html, extent);
        assertFalse(html.contains(UiFullValueFixture.SECRET), "Full values and hidden channels are redacted before publication");
        String changed = html.replace("expected-last-2", "expected-lost-2");
        assertThrows(AssertionError.class, () -> requireComplete(changed, extent), "Removing only a final scalar must fail the independent oracle");
        System.out.println("UI_FULL_VALUE_ARTIFACT=" + path);
    }
    @Test void exactPreviewBoundaryKeepsShortValuesPlainAndFullLongValuesIndependentOfBodyLimit() {
        for (int size : List.of(239, 240, 241)) {
            String original = "x".repeat(size);
            var row = new AtfUiActionRecord(1, "captured", "target", original, original, 0, AtfReportStatus.INFO);
            var settings = new ReportingSettings(240, 1);
            for (String markup : List.of(new ExtentUiActionRenderer(settings).markup(List.of(row), List.of()),
                    new ExtentUiObservationRenderer(settings).markup(List.of(row)))) {
                var cell = Jsoup.parse(markup).select("tbody > tr > td").get(2);
                assertEquals(size > 240 ? 1 : 0, cell.select("details.atf-ui-value").size());
                assertEquals(original, size > 240 ? cell.selectFirst("pre").wholeText() : cell.wholeText());
            }
        }
    }
    private static void requireComplete(String html, boolean extent) {
        var doc = Jsoup.parse(html); doc.select("template,noscript").remove();
        var owners = doc.select(extent ? "#test-view .test-list-item > .test-item" : "#tests article.scenario");
        assertEquals(2, owners.size());
        for (int owner = 1; owner <= 2; owner++) {
            var tables = owners.get(owner - 1).select("table.atf-ui-action-table");
            assertEquals(2, tables.size(), "Both ACTION and OBSERVE retain their own table");
            for (Element table : tables) {
                var cells = table.select("tbody > tr").getFirst().children();
                for (int column = 2; column <= 3; column++) {
                    String expected = UiFullValueFixture.value(column == 2, owner);
                    Element disclosure = cells.get(column).selectFirst("details.atf-ui-value");
                    assertNotNull(disclosure, "Long values require an owned disclosure");
                    assertFalse(disclosure.hasAttr("open"));
                    assertEquals(240, disclosure.selectFirst("summary .se-title").text().length());
                    assertEquals(expected, disclosure.selectFirst("pre").wholeText());
                }
            }
        }
    }
}

package atf.reporting.evidence;

import atf.testkit.AtfComponentTest;
import atf.reporting.markup.AtfReportMarkup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ReportEvidenceTableTest {

    @Test
    void keepsEvidenceDataImmutableAndRoundTripsLegacyRows() {
        String[][] legacy = {
                {"FIELD", "VALUE"},
                {"response.status", "200"}
        };

        var table = ReportEvidenceTable.from(legacy);
        legacy[1][1] = "mutated-after-construction";

        assertAll(
                () -> assertEquals(List.of("FIELD", "VALUE"), table.headers()),
                () -> assertEquals(List.of(List.of("response.status", "200")), table.rows()),
                () -> assertEquals("200", table.toArray()[1][1]),
                () -> assertFalse(table.firstColumnHeader())
        );
    }

    @Test
    void supportsExplicitRowHeadersWithoutChangingOtherCells() {
        var table = ReportEvidenceTable.of(
                List.of("FIELD", "VALUE"),
                List.of(List.of("response.duration.ms", "301 ms")))
                .withFirstColumnHeaders();

        String html = AtfReportMarkup.tableHtml("execution-table", table);

        assertAll(
                () -> assertTrue(table.firstColumnHeader()),
                () -> assertTrue(html.contains("<th>response.duration.ms</th>")),
                () -> assertTrue(html.contains("<td>0.301 s</td>")),
                () -> assertFalse(html.contains("301 ms</td>"))
        );
    }
}

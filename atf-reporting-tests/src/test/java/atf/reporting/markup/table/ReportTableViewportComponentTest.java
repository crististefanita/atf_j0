package atf.reporting.markup.table;

import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.evidence.ReportEvidenceStyleContract;
import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.theme.AtfReportTheme;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ReportTableViewportComponentTest {
    @ParameterizedTest
    @ValueSource(strings = {"atf-setup-evidence-table", "execution-table", "helm-validation-table",
            "helm-trace-table", "helm-request-table", "retry-history-table", "future-consumer-table"})
    void everyDataTableIncludingUnknownFamiliesHasExactlyOneViewport(String family) {
        String html = AtfReportMarkup.tableHtml(family,
                ReportEvidenceTable.of(List.of("FIELD", "VALUE"), List.of(List.of("<key>", "value"))));
        assertAll(
                () -> assertEquals(1, html.split("<div class=\"atf-", -1).length - 1),
                () -> assertTrue(html.endsWith("</table></div>")),
                () -> assertTrue(html.contains("<table class=\"" + family + "\">")),
                () -> assertTrue(html.contains("&lt;key&gt;")));
    }

    @Test
    void emptyAndNullTablesStillPublishBalancedBoundedMarkup() {
        for (ReportEvidenceTable table : new ReportEvidenceTable[]{null, ReportEvidenceTable.from(null)}) {
            String html = AtfReportMarkup.tableHtml(null, table);
            assertTrue(html.startsWith(ReportTableViewport.DATA.open()));
            assertTrue(html.endsWith("</tbody></table></div>"));
        }
    }

    @Test
    void bothThemesPublishTheSamePolicyWithoutValidationParentDependency() {
        String policy = ReportAssetComposer.tableViewportCss();
        assertAll(
                () -> assertTrue(AtfReportTheme.css().endsWith(policy)),
                () -> assertTrue(AtfReportTheme.fallbackCss().endsWith(policy)),
                () -> assertFalse(policy.contains("data-atf-node-role")),
                () -> assertFalse(policy.contains("500px")),
                () -> assertTrue(policy.contains("max-height:var(--atf-table-scroll-height)!important")),
                () -> assertTrue(policy.contains("position:sticky!important")));
    }

    @Test
    void staticInspectorRecognizesShorthandContainmentAndRejectsItsRemoval() {
        String css = AtfReportTheme.css();
        var path = Path.of("viewport-contract.html");
        assertTrue(ReportEvidenceStyleContract.inspectHtml(path,
                css, true).checks().get("local action overflow"));
        assertFalse(ReportEvidenceStyleContract.inspectHtml(path,
                css.replace("overscroll-behavior:contain!important", "overscroll-behavior:auto!important"),
                true).checks().get("local action overflow"));
    }
}

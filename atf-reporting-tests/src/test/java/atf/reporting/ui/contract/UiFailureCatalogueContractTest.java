package atf.reporting.ui.contract;

import atf.testkit.AtfComponentTest;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.ui.tests.extent.support.inspection.provenance.UiFailureCatalogueFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** The secondary catalogue preserves the exact original row order and independent assertion population. */
@AtfComponentTest
class UiFailureCatalogueContractTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void uiFailureLabelsAndCausesUseTheSameRedactionAsTheirOriginalRows(boolean requestOwner, @TempDir Path directory)
            throws Exception {
        Path report = directory.resolve("redacted-failure.html");
        var records = List.of(new AtfUiActionRecord(2, "Activate token=raw-operation-secret",
                "button=Continue; password=raw-target-secret", "accepted", "password=raw-cause-secret", 11, AtfReportStatus.FAIL));
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("UiCatalogue", "Redacted captured dispatch", List.of("component", "mock"));
            try (var phase = scenario.startPhase("TEST BODY")) {
                try (var request = phase.startRequest("Captured UI flow", "UI", "device://captured")) {
                    if (requestOwner) request.uiActions(records);
                    else try (var validation = request.startValidation("Captured UI checkpoint")) {
                        validation.uiActions(records);
                        validation.complete(AtfReportStatus.FAIL);
                    }
                    request.complete(AtfReportStatus.FAIL);
                }
                phase.complete(AtfReportStatus.FAIL);
            }
            scenario.complete(AtfReportStatus.FAIL);
        }
        String html = Files.readString(report);
        for (String secret : List.of("raw-operation-secret", "raw-target-secret", "raw-cause-secret")) assertFalse(html.contains(secret));
        var failure = Jsoup.parse(html).select("#fails a.fail-row");
        assertEquals(1, failure.size());
        assertTrue(failure.text().contains("UI action"));
        assertTrue(failure.text().contains("***REDACTED***"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actionObservationAndComparisonKeepTheirOwnFailureAndOrdinal(boolean requestOwner) throws Exception {
        var document = Jsoup.parse(Files.readString(UiFailureCatalogueFixture.publish(requestOwner)));
        var links = document.select("#fails a.fail-row");
        assertEquals(3, links.size());
        assertTrue(links.get(0).text().contains("UI action · Activate #2 → button=Continue"));
        assertTrue(links.get(1).text().contains("Audit observation · Inspect #3 → dialog=Receipt"));
        assertTrue(links.get(2).text().contains("Receipt total"));
        for (int index = 0; index < links.size(); index++) {
            assertEquals(Integer.toString(index + 1), links.get(index).attr("data-atf-failure-ordinal"));
            assertTrue(links.get(index).text().contains(UiFailureCatalogueFixture.CAUSES.get(index)));
        }
        var scenario = document.select("article.scenario").first();
        assertEquals(2, scenario.select("table.assertions>tbody>tr").size(), "UI records must not inflate assertions");
        assertEquals(1, scenario.select("table.assertions .status-fail").size());
        assertEquals(1, scenario.select("a[data-atf-screenshot='base64']").size());
        assertTrue(document.select("article.scenario").last().select(".atf-ui-action-table,table.assertions").isEmpty());
    }
}

package atf.reporting.ui.values;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.support.values.UiRowEvidenceFixture;
import static atf.reporting.support.values.UiRowEvidenceFixture.CANARY;
import static atf.reporting.support.values.UiRowEvidenceFixture.row;
import atf.reporting.contract.*;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.evidence.ui.*;
import atf.reporting.html.evidence.HtmlReportEvidenceView;
import atf.reporting.model.*;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.model.*;
import atf.testkit.AtfComponentTest;
import java.nio.file.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Captured values and an independently authored redaction policy own the expected DOM values. */
@AtfComponentTest
class UiRowProjectionComponentTest {

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void originalCellsKeepTheirWhitespaceIdentityAndRedactedMetadata(boolean extent) throws Exception {
        Path file = UiRowEvidenceFixture.publish(extent);
        String html = Files.readString(file); assertFalse(html.contains(CANARY), "No raw credential in visible or hidden channels");
        var owner = Jsoup.parse(html).selectFirst(extent ? "#test-view .test-list-item > .test-item" : "#tests article.scenario");
        assertNotNull(owner); var tables = owner.select("table.atf-ui-action-table"); assertEquals(2, tables.size());
        for (int index = 0; index < 2; index++) {
            var cells = tables.get(index).select("tbody > tr > td");
            assertEquals("operation token=***REDACTED***", cells.get(0).selectFirst(".atf-ui-action-operation").text());
            assertEquals(index == 0 ? "#7" : "#11", cells.get(0).selectFirst(".atf-ui-action-number").text());
            assertEquals("target token=***REDACTED***", cells.get(1).text());
            for (int column = 2; column <= 3; column++) {
                var disclosure = cells.get(column).selectFirst("details.atf-ui-value"); assertNotNull(disclosure);
                assertFalse(disclosure.hasAttr("open"));
                assertEquals(UiRowEvidenceFixture.value(index == 0 ? AtfUiEvidenceSource.ACTION : AtfUiEvidenceSource.OBSERVE), disclosure.selectFirst("pre").wholeText());
                assertTrue(disclosure.selectFirst("summary .se-title").wholeText().length() <= 240);
            }
            assertEquals(1, cells.getLast().select(".atf-ui-action-result-info").size());
        }
        var image = owner.selectFirst("a[data-atf-screenshot=base64]"); assertNotNull(image);
        assertEquals("Open checkpoint token=***REDACTED***", image.attr("aria-label"));
        assertEquals("data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG, image.attr("href"));
        System.out.println("ROW_PROJECTION_ARTIFACT=" + file);
    }
    @Test void oneCustomPolicyReachesBothSourcesWithoutRewritingTheOriginalRecord() {
        var policy = new SensitiveDataRedactor("[masked value]"); var settings = ReportingSettings.defaults();
        var actions = List.of(row(7, AtfUiEvidenceSource.ACTION), row(11, AtfUiEvidenceSource.OBSERVE));
        var captures = List.of(new ReportScreenshot("checkpoint token=" + CANARY, ReportTestScreenshots.ONE_PIXEL_PNG));
        var validation = new ReportExecutionModel.Validation("captured", List.of(), AtfReportStatus.INFO, 2, 1, List.of(), actions, captures);
        var fallback = new StringBuilder(); new HtmlReportEvidenceView(settings, policy).renderValidation(fallback, validation);
        String extent = new ExtentUiActionRenderer(settings, policy, ReportUiActionDetailsMode.INCLUDED_COLLAPSED).markup(List.of(actions.getFirst()), captures)
                + new ExtentUiObservationRenderer(settings, policy).markup(List.of(actions.getLast()));
        for (String markup : List.of(fallback.toString(), extent)) {
            assertFalse(markup.contains(CANARY)); var doc = Jsoup.parse(markup);
            assertEquals(List.of("operation token=[masked value]", "operation token=[masked value]"), doc.select(".atf-ui-action-operation").eachText());
            assertEquals("Open checkpoint token=[masked value]", doc.selectFirst("a[data-atf-screenshot=base64]").attr("aria-label"));
        }
        assertEquals("operation token=" + CANARY, actions.getFirst().operation());
    }
    @Test void anInitialLineBreakRemainsPartOfTheDisclosedValue() {
        var settings = ReportingSettings.defaults();
        for (String opening : List.of("\n", "\r\n", "\r")) {
            String captured = opening + "captured-state-".repeat(30) + "last\n ";
            var row = new AtfUiActionRecord(1, "observe", "target", captured, captured, 0, AtfReportStatus.INFO);
            for (String markup : List.of(new ExtentUiActionRenderer(settings).markup(List.of(row), List.of()),
                    new ExtentUiObservationRenderer(settings).markup(List.of(row)))) {
                assertEquals(captured, Jsoup.parse(markup).selectFirst("details.atf-ui-value pre").wholeText());
            }
        }
    }
}

package atf.reporting.ui.contract;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.provenance.UiActionAttributionFixture;
import atf.ui.tests.extent.support.inspection.provenance.UiActionAttributionFixture.Mode;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Independent public HTML contract; a legitimate tool name in captured evidence remains permitted. */
@AtfComponentTest
class UiActionAttributionContractTest {
    static Stream<Arguments> inputs() {
        return Stream.of(false, true).flatMap(fallback -> Stream.of("web", "android").flatMap(context ->
                Stream.of(Mode.values()).map(mode -> Arguments.of(fallback, context, mode))));
    }

    @ParameterizedTest
    @MethodSource("inputs")
    void neutralInputsKeepTruthfulHeadingsRowsAndObservationOnlyAbsence(boolean fallback, String context, Mode mode)
            throws Exception {
        var document = Jsoup.parse(Files.readString(UiActionAttributionFixture.publish(fallback, context, mode)));
        verify(document, fallback, mode);
    }

    static void verify(org.jsoup.nodes.Document document, boolean fallback, Mode mode) {
        var disclosures = document.select("details.atf-ui-action-sequence");
        assertTrue(document.text().contains(UiActionAttributionFixture.LEGITIMATE_AUDIT_TOOL));
        assertEquals(1, document.select(fallback ? "details.assertion-evidence table.assertions>tbody>tr"
                : ".atf-ui-assertions table>tbody>tr").size());
        assertFalse(document.select("a[href^='data:image/png;base64,']").isEmpty());
        if (fallback) {
            var failures = document.select("#fails a.fail-row");
            assertEquals(mode == Mode.DISPATCH_FAIL ? 1 : 0, failures.size(), "A failed dispatch is not a failed assertion");
            if (mode == Mode.DISPATCH_FAIL) {
                assertTrue(failures.text().contains("UI action · Activate #2"));
                assertTrue(failures.text().contains(UiActionAttributionFixture.CAUSE));
                assertTrue(failures.text().contains(UiActionAttributionFixture.TARGET));
            }
        }
        if (mode == Mode.OBSERVE_ONLY) {
            assertTrue(disclosures.isEmpty(), "Read-only input does not invent a dispatched action");
            return;
        }
        assertEquals(1, disclosures.size());
        var disclosure = disclosures.first();
        assertFalse(disclosure.hasAttr("open"));
        var summary = disclosure.children().stream().filter(e -> e.normalName().equals("summary")).findFirst().orElseThrow();
        assertNeutral(summary);
        var rows = disclosure.select("table.atf-ui-action-table > tbody > tr");
        assertEquals(2, rows.size());
        assertTrue(rows.get(1).text().contains(UiActionAttributionFixture.TARGET));
        assertEquals(mode == Mode.DISPATCH_FAIL ? 1 : 0, rows.select(".atf-ui-action-result-fail").size());
        List<String> originalRows = rows.eachText();
        List<String> originalImages = document.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]").eachAttr("href");
        summary.text(summary.text().replace("UI action sequence", "Playwright action sequence"));
        assertThrows(AssertionError.class, () -> assertNeutral(summary), "False driver attribution must be refused");
        assertEquals(originalRows, rows.eachText());
        assertEquals(originalImages, document.select("a[data-atf-screenshot],a[data-atf-screenshot-ref]").eachAttr("href"));
    }

    private static void assertNeutral(Element summary) {
        assertTrue(summary.text().contains("UI action sequence"), summary.text());
        assertFalse(summary.text().contains("Playwright action sequence"), summary.text());
    }
}

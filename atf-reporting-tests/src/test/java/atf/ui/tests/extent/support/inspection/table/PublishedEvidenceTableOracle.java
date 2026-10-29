package atf.ui.tests.extent.support.inspection.table;

import atf.reporting.ui.contract.ReportUiContract.Tab;
import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import static atf.ui.tests.extent.support.inspection.table.PublishedEvidenceTableMeasurements.*;
import static org.junit.jupiter.api.Assertions.*;

/** Independent physical geometry and literal assertion values; never calls the column allocator. */
final class PublishedEvidenceTableOracle {
    private static final PublishedEvidenceTableLocators LOCATORS = new PublishedEvidenceTableLocators();
    private static final List<String> ASSERTION_HEADERS = List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT");
    private static final List<List<String>> SHORT_ROWS = List.of(
            List.of("Page route", "/", "/"), List.of("catalog product cards", "visible", "visible"),
            List.of("catalog product names", "visible", "visible"), List.of("catalog product images", "visible", "visible"),
            List.of("catalog pagination", "visible", "visible"), List.of("catalog footer", "visible", "visible"),
            List.of("Product cards", "> 0", "9"), List.of("Product card names", "9", "9"),
            List.of("Product card images", "9", "9"), List.of("catalog pagination count", ">= 1", "1"),
            List.of("Style · catalog product cards · display", "flex", "flex"),
            List.of("Style · catalog footer · display", "block", "block"));

    private PublishedEvidenceTableOracle() { }

    static List<Locator> dataTables(Page page) { return PublishedEvidenceTableMeasurements.dataTables(page); }
    static List<String> contents(Page page) { return PublishedEvidenceTableMeasurements.contents(page); }

    static void verify(Page page, String context) {
        Map<?, ?> measured = geometry(page);
        List<?> tables = (List<?>) measured.get("tables");
        assertFalse(tables.isEmpty(), "missing data-table inventory: " + context);
        for (Object value : tables) {
            var table = (Map<?, ?>) value;
            assertEquals(List.of(), table.get("problems"), context + ": " + table.get("family"));
        }
        verifyDocument(measured, context);
    }

    static void verifyShortAssertions(Page page) { verifyShortAssertions(snapshot(page)); }

    private static void verifyShortAssertions(List<Table> tables) {
        int matched = 0;
        for (Table table : tables) {
            if (!table.assertions() || table.rows().size() != 12) continue;
            assertEquals(ASSERTION_HEADERS, table.headers(), "all four assertion columns remain present");
            assertEquals(SHORT_ROWS, evidenceRows(table), "the screenshot-like evidence remains literal and complete");
            verifyResults(table, true);
            matched++;
        }
        assertTrue(matched >= 1, "the 12-row assertion case may not silently disappear");
    }

    static void verifyInventory(Page page, PublishedEvidenceTableFixture.Profile profile) {
        List<Table> tables = snapshot(page);
        int ui = profile == PublishedEvidenceTableFixture.Profile.API ? 0 : 3;
        int api = profile == PublishedEvidenceTableFixture.Profile.UI ? 0 : 3;
        count(tables, t -> t.has("atf-setup-evidence-table"), 3, "all setup tables");
        count(tables, t -> t.has("atf-ui-action-table") && !t.has("atf-ui-observation-table"), ui, "ACTION owners");
        count(tables, t -> t.has("atf-ui-observation-table"), ui, "OBSERVE owners");
        count(tables, t -> t.has("helm-request-table") && t.inDisclosure("request-evidence"), api, "request owners");
        count(tables, t -> t.has("helm-trace-table") && t.inDisclosure("response-evidence"), api, "response owners");
        count(tables, t -> t.has("execution-table") && t.inDisclosure("execution-evidence"), api, "execution owners");
        count(tables, t -> t.has("retry-history-table") && t.inDisclosure("execution-evidence"), api, "retry owners");
        count(tables, t -> t.has("helm-trace-table") && t.inDisclosure("execution-evidence"), api, "execution trace owners");
        count(tables, t -> t.has("helm-trace-table") && t.inDisclosure("network-aggregate-section"), api,
                "network authentication trace owners");
        count(tables, t -> t.has("helm-trace-table"), api * 3,
                "trace partition: response details, execution trace and network authentication");
        verifyShapes(tables, profile);
    }

    static void verifyShapes(Page page, PublishedEvidenceTableFixture.Profile profile) {
        verifyShapes(snapshot(page), profile);
    }

    private static void verifyShapes(List<Table> tables, PublishedEvidenceTableFixture.Profile profile) {
        int copies = profile == PublishedEvidenceTableFixture.Profile.UI_API ? 3 : 2;
        int shortTables = 0, expectedTables = 0, actualTables = 0;
        for (Table table : tables) {
            if (!table.assertions()) continue;
            if (table.rows().size() == 12) { shortTables++; continue; }
            if (table.rows().size() != 40) continue;
            assertEquals(ASSERTION_HEADERS, table.headers(), "all four long-comparison columns remain present");
            boolean longExpected = table.rows().getFirst().get(1).startsWith("Expected Ω <edge>");
            String expectedValue = longExpected ? "Expected Ω <edge> ".repeat(7) + "W".repeat(60) : "ready";
            String actualValue = longExpected ? "different" : "Observed Ω <edge> ".repeat(7) + "W".repeat(60);
            var expectedRows = IntStream.rangeClosed(1, 40).mapToObj(i ->
                    List.of("Independent comparison " + i, expectedValue, actualValue)).toList();
            assertEquals(expectedRows, evidenceRows(table), "long evidence matches the independent fixture contract");
            verifyResults(table, false);
            if (longExpected) expectedTables++; else actualTables++;
        }
        assertEquals(copies, shortTables, "all SHORT assertion owners");
        assertEquals(copies, expectedTables, "all LONG_EXPECTED assertion owners");
        assertEquals(copies, actualTables, "all LONG_ACTUAL assertion owners");
        verifyShortAssertions(tables);
    }

    private static List<List<String>> evidenceRows(Table table) {
        return table.rows().stream().map(row -> row.subList(0, Math.min(3, row.size()))).toList();
    }

    static void verifyDerivedFailures(Page page, PublishedEvidenceTableFixture.Profile profile) {
        int copies = switch (profile) { case UI -> 1; case API -> 2; case UI_API -> 3; };
        List<Table> derived = snapshot(page).stream().filter(t -> t.owns("atf-ui-failure-evidence")).toList();
        assertEquals(copies * 2, derived.size(), "one derived failed comparison per failed runtime evidence owner");
        int expectedSide = 0;
        for (Table table : derived) {
            assertEquals(ASSERTION_HEADERS, table.headers());
            assertEquals(1, table.rows().size());
            boolean expected = table.rows().getFirst().get(1).startsWith("Expected Ω <edge>");
            String value = expected ? "Expected Ω <edge> ".repeat(7) + "W".repeat(60) : "ready";
            String observed = expected ? "different" : "Observed Ω <edge> ".repeat(7) + "W".repeat(60);
            assertEquals(List.of(List.of("Independent comparison 1", value, observed)), evidenceRows(table),
                    "derived evidence preserves the first original failed comparison");
            verifyResults(table, false);
            if (expected) expectedSide++;
        }
        assertEquals(copies, expectedSide, "both long shapes have their own derived evidence");
    }

    private static void verifyResults(Table table, boolean pass) {
        assertTrue(table.rows().stream().allMatch(row -> row.size() == 4
                && row.get(3).toLowerCase(Locale.ROOT).matches(pass ? ".*(✅|pass).*" : ".*(❌|fail).*")),
                pass ? "all positive results remain PASS" : "every deliberate mismatch remains FAIL");
    }

    static void verifyView(Page page, boolean fallback, Tab tab) {
        List<Table> tables = snapshot(page);
        if (fallback) {
            assertEquals(0, tables.size(), "fallback secondary views use cards and links");
            verifyDocument(geometry(page), "fallback " + tab);
            String visible = LOCATORS.view(page, true, tab).innerText();
            assertFalse(visible.isBlank(), "secondary projection may not disappear");
            if (tab == Tab.FAILS) assertTrue(visible.contains("Independent comparison 40"),
                    "fallback Fails retains the final failed comparison");
            return;
        }
        if (tab == Tab.FAILS) {
            assertEquals(0, tables.size(), "native Fails uses an index and comparison fields, not a data table");
            verifyDocument(geometry(page), "native Fails");
            verifyFailureRail(page);
            String detail = LOCATORS.failureDetail(page).innerText();
            assertTrue(detail.contains("Independent comparison 1"), "the original failed comparison remains inspectable");
            assertTrue(detail.contains("Expected Ω <edge> ".repeat(7) + "W".repeat(60)), "full expected value in Fails");
            assertTrue(detail.contains("different"), "independent actual value in Fails");
            assertTrue(LOCATORS.firstFailure(page).innerText().contains("Fail"), "failure status remains visible");
            return;
        }
        assertFalse(tables.isEmpty(), "required native data tables disappeared from " + tab);
        verify(page, "native " + tab);
        if (tab == Tab.OVERVIEW) {
            count(tables, t -> t.owns("sysenv-container") && t.headers().equals(List.of("NAME", "VALUE")), 1,
                    "Overview System table");
            count(tables, t -> t.owns("category-container") && t.headers().containsAll(List.of("NAME", "PASSED", "FAILED")), 1,
                    "Overview Tags table");
        } else if (tab == Tab.CATEGORIES) {
            count(tables, t -> t.owns("atf-category-detail") && t.rows().size() == 3, 1,
                    "Category retains all three scenarios");
        }
    }

    private static void count(List<Table> tables, Predicate<Table> predicate, long expected, String context) {
        assertEquals(expected, tables.stream().filter(predicate).count(), context);
    }

    private static void verifyFailureRail(Page page) {
        Map<?, ?> bounds = (Map<?, ?>) LOCATORS.failurePanels(page).evaluateAll("panels=>{"
                + "const rail=document.getElementsByClassName('side-nav')[0].getBoundingClientRect();"
                + "return {rail:rail.right,width:document.documentElement.clientWidth,"
                + "panels:panels.map(p=>p.getBoundingClientRect()).filter(r=>r.width>0&&r.height>0)"
                + ".map(r=>({left:r.left,right:r.right}))};}");
        List<?> panels = (List<?>) bounds.get("panels");
        assertEquals(2, panels.size(), "both failure index and detail remain visible");
        double rail = ((Number) bounds.get("rail")).doubleValue(), width = ((Number) bounds.get("width")).doubleValue();
        for (Object value : panels) {
            Map<?, ?> box = (Map<?, ?>) value;
            assertTrue(((Number) box.get("left")).doubleValue() >= rail - 2
                    && ((Number) box.get("right")).doubleValue() <= width + 2,
                    "failure content may not hide behind navigation or leave the viewport: " + bounds);
        }
    }

    private static void verifyDocument(Map<?, ?> measured, String context) {
        assertEquals(Boolean.FALSE, measured.get("overflow"), context + "; document width="
                + measured.get("width") + "; offenders=" + measured.get("offenders"));
    }
}


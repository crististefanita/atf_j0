package atf.ui.tests.extent.support.inspection.duration;

import atf.reporting.asset.ReportAssetComposer;
import atf.testkit.files.AtfTestResourceReader;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executes the production projection against controlled canonical durations and stale native charts. */
public final class OverviewDurationProjectionProbe {
    private static final String FIXTURE_ROOT = "/atf/reporting/fixtures/overview-duration";
    private OverviewDurationProjectionProbe() { }

    public static void verifyMeasuredProjectionAndRefresh() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = fixture(browser);
            var first = snapshot(page);
            assertProjection(first, 30_000, List.of(10, 20), 1);
            assertProjection(snapshot(page), 30_000, List.of(10, 20), 1);
            assertProjection(change(page, "cart"), 35_000, List.of(15, 20), 2);
            assertProjection(change(page, "array-registry"), 35_000, List.of(15, 20), 2);
            assertProjection(change(page, "duplicate"), 35_000, List.of(15, 20), 3);
            assertProjection(snapshot(page), 35_000, List.of(15, 20), 3);
        }
    }

    public static void verifyZeroAndNativeFallback() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page measured = fixture(browser);
            assertProjection(change(measured, "zero"), 0, List.of(0, 0), 1);
            var unmeasured = change(fixture(browser), "unmeasured");
            assertEquals(500_000, number(unmeasured.get("total")));
            assertEquals(List.of(300L, 200L), numbers(unmeasured.get("nativeValues")));
            assertEquals(List.of(300L, 200L), numbers(unmeasured.get("chartValues")));
            assertEquals(0, number(unmeasured.get("updates")));
        }
    }

    public static void verifyAuditRejectsNumericButInconsistentProjection() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            for (String target : List.of("marker", "model", "chart")) {
                Page page = fixture(browser);
                snapshot(page);
                var before = ApiReportDurationProjectionAuditor.inspectCurrentDom(page);
                assertTrue(before.stream().allMatch(projection -> projection.valid()));
                page.evaluate("target => corruptOverviewDuration(target)", target);
                var overview = ApiReportDurationProjectionAuditor.inspectCurrentDom(page).stream()
                        .filter(projection -> "Overview".equals(projection.projection())).findFirst().orElseThrow();
                assertFalse(overview.valid(), target + " must fail even when every marker is numeric");
                assertTrue(overview.violations().stream().anyMatch(value -> value.contains("Timeline " + target)));
            }
        }
    }

    private static Page fixture(Browser browser) {
        Page page = browser.newPage();
        page.setContent(resource(".html"));
        page.addScriptTag(new Page.AddScriptTagOptions().setContent(resource(".js")));
        page.addScriptTag(new Page.AddScriptTagOptions().setContent(
                ReportAssetComposer.durationParsingScript()
                        + ReportAssetComposer.durationProjectionViewsScript()));
        return page;
    }

    private static String resource(String extension) {
        return AtfTestResourceReader.readResourceUtf8(OverviewDurationProjectionProbe.class, FIXTURE_ROOT + extension);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> snapshot(Page page) {
        return (Map<String, Object>) page.evaluate("overviewDurationSnapshot()");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> change(Page page, String change) {
        return (Map<String, Object>) page.evaluate("argument => changeOverviewDuration(argument)", change);
    }

    private static void assertProjection(Map<String, Object> snapshot, long total, List<Integer> seconds, int updates) {
        assertEquals(total, number(snapshot.get("total")));
        assertEquals("aggregated", snapshot.get("source"));
        assertEquals(seconds.stream().map(Integer::longValue).toList(), numbers(snapshot.get("nativeValues")));
        assertEquals(seconds.stream().map(Integer::longValue).toList(), numbers(snapshot.get("chartValues")));
        assertEquals(List.of(7L, 0L), numbers(snapshot.get("statusValues")));
        assertEquals(updates, number(snapshot.get("updates")));
    }

    private static long number(Object value) { return ((Number) value).longValue(); }
    private static List<Long> numbers(Object values) {
        return ((List<?>) values).stream().map(OverviewDurationProjectionProbe::number).toList();
    }
}

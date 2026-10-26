package atf.reporting.extent.time;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportFinalizer;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.extent.session.ExtentReportTimestampReporter;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reporting component proof; no browser, consumer or service dependency. */
@AtfComponentTest
class ReportingTimestampComponentTest {
    private static final LocalDate FIXTURE_DATE = LocalDate.of(2026, 7, 2);
    private static final String CATEGORY = "clock-format";
    private static final Pattern MERIDIEM = Pattern.compile("(?i)\\b(?:AM|PM)\\b");
    @TempDir Path temporary;

    @ParameterizedTest
    @ValueSource(ints = {0, 12, 15, 23})
    void vendorSurfacesUse24HoursWithoutChangingTheRecordedInstants(int hour) throws Exception {
        Path report = output("time-24h-" + hour + ".html");
        var reports = new ExtentReports();
        reports.setReportUsesManualConfiguration(true);
        var spark = new ExtentReportTimestampReporter(report.toString());
        spark.config().setOfflineMode(true);
        spark.config().setReportName("24-hour clock fixture");
        reports.attachReporter(spark);
        Date start = at(hour, 5, 7);
        Date end = at(hour, 5, 9);
        ExtentTest test = reports.createTest("clock sample " + hour).assignCategory(CATEGORY);
        test.info("controlled timestamp evidence");
        test.getModel().setStartTime(start);
        test.getModel().setEndTime(end);
        test.getModel().getLogs().forEach(log -> log.setTimestamp(start));
        reports.getReport().setStartTime(start);
        reports.getReport().setEndTime(end);
        reports.flush();
        assertEquals(start, test.getModel().getStartTime());
        assertEquals(end, test.getModel().getEndTime());
        assertEquals(2_000L, test.getModel().timeTaken());
        assertEquals(start, reports.getReport().getStartTime());
        assertEquals(end, reports.getReport().getEndTime());
        String vendor = Files.readString(report);
        String expectedTime = String.format(Locale.ROOT, "%02d:05:07", hour);
        String expectedEnd = String.format(Locale.ROOT, "%02d:05:09", hour);
        assertTrue(vendor.contains("<span class=\"badge badge-primary\">2026-07-02 " + expectedTime + "</span>"),
                "navbar uses the report start instant");
        assertTrue(vendor.contains("<h3>2026-07-02 " + expectedTime + "</h3>"), "Overview started uses 24h");
        assertTrue(vendor.contains("<h3>2026-07-02 " + expectedEnd + "</h3>"), "Overview ended uses 24h");
        assertTrue(vendor.contains("<span>" + expectedTime + "</span> /"), "Tests list uses 24h");
        assertTrue(Pattern.compile("badge badge-success['\"]>[^<]*" + expectedTime + "</span>").matcher(vendor).find(),
                "Tests start badge uses the same instant");
        assertTrue(Pattern.compile("badge badge-danger['\"]>[^<]*" + expectedEnd + "</span>").matcher(vendor).find(),
                "Tests end badge uses the same instant");
        assertEquals(2, vendor.split(Pattern.quote("<td>" + expectedTime + "</td>"), -1).length - 1,
                "category and log timestamps both use 24h");
        assertFalse(MERIDIEM.matcher(withoutScripts(vendor)).find(), "no visible surface may append AM/PM");
        ExtentReportFinalizer.finalizeReport(report);
        String published = Files.readString(report);
        assertTrue(published.contains("<td>" + expectedTime + "</td>"));
        assertTrue(published.contains("class=\"linked\""), "category links are preserved");
        assertFalse(MERIDIEM.matcher(withoutScripts(published)).find());
    }

    @Test
    void canonicalSessionUsesTheSameTimestampAdapter() throws Exception {
        Path report = output("time-24h-session.html");
        publishSession(report);
        String html = Files.readString(report);
        assertFalse(MERIDIEM.matcher(withoutScripts(html)).find());
        assertTrue(html.contains(CATEGORY));
        assertTrue(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}").matcher(html).find());
    }

    private static void publishSession(Path report) {
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ClockSessionFixture", "canonical session", List.of(CATEGORY));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("THEN timestamps use the 24-hour contract", "GET", "/clock");
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }

    private Path output(String fileName) throws Exception {
        Path directory = Path.of(System.getProperty("atf.test.timestampReportDirectory", temporary.toString()));
        Files.createDirectories(directory);
        return directory.resolve(fileName);
    }

    private static Date at(int hour, int minute, int second) {
        return Date.from(FIXTURE_DATE.atTime(LocalTime.of(hour, minute, second))
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    private static String withoutScripts(String html) {
        return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "");
    }
}

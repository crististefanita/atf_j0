package atf.reporting.evidence;
import atf.testkit.AtfUnitTest;

import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.model.ReportScreenshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ExtentReportAssetsTest {
    @Test
    void inlinesSupportedSparkAssetsWithoutLeavingExternalReferences() throws Exception {
        Path root = Files.createTempDirectory("atf-assets-");
        Path spark = Files.createDirectory(root.resolve("spark"));
        Files.writeString(spark.resolve("spark-style.css"), ".spark{color:red}");
        Files.writeString(spark.resolve("spark-script.js"), "window.sparkLoaded=true;");
        Files.write(spark.resolve("logo.png"), new byte[]{1, 2, 3});

        String source = "<link rel=\"stylesheet\" href=\"spark/spark-style.css\">"
                + "<script src=\"spark/spark-script.js\"></script>spark/logo.png";
        String result = ExtentReportAssets.inlineSparkAssets(source, spark);

        assertTrue(result.contains("data-atf-inline=\"spark-style.css\""));
        assertTrue(result.contains(".spark{color:red}"));
        assertTrue(result.contains("data-atf-inline=\"spark-script.js\""));
        assertTrue(result.contains("window.sparkLoaded=true;"));
        assertTrue(result.contains("data:image/png;base64,AQID"));
        assertFalse(result.contains("spark/spark-style.css"));
        assertFalse(result.contains("spark/spark-script.js"));

        ExtentReportAssets.removeGeneratedAssets(spark);
        assertFalse(Files.exists(spark));
        Files.deleteIfExists(root);
    }

    @Test
    void missingAssetsAreAStableNoOpAndNonSparkDirectoriesAreNeverRemoved() throws Exception {
        Path root = Files.createTempDirectory("atf-assets-");
        Path nonSpark = Files.createDirectory(root.resolve("not-spark"));
        Path marker = Files.writeString(nonSpark.resolve("marker.txt"), "keep");

        assertEquals("plain", ExtentReportAssets.inlineSparkAssets("plain", nonSpark));
        ExtentReportAssets.removeGeneratedAssets(nonSpark);

        assertTrue(Files.exists(marker));
        assertTrue(Files.exists(nonSpark));
        Files.deleteIfExists(marker);
        Files.deleteIfExists(nonSpark);
        Files.deleteIfExists(root);
    }

    @Test
    void rendersOneSharedNativeBase64ScreenshotLinkContract() {
        String markup = ExtentReportAssets.base64ScreenshotLink(
                "atf-ui-evidence-screenshot-link", "Tests <tab>", ReportTestScreenshots.ONE_PIXEL_PNG);

        assertAll(
                () -> assertTrue(markup.contains("class=\"atf-ui-evidence-screenshot-link\"")),
                () -> assertTrue(markup.contains("data-atf-screenshot=\"base64\"")),
        () -> assertTrue(markup.contains("href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\"")),
                () -> assertTrue(markup.contains("target=\"_blank\" rel=\"noopener\"")),
                () -> assertTrue(markup.contains("📷 base64 img")),
                () -> assertTrue(markup.contains("Open Tests &lt;tab&gt;")),
        () -> assertTrue(count(markup, "data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG) == 1),
                () -> assertFalse(markup.contains("<img")),
                () -> assertFalse(markup.contains("onclick=")),
                () -> assertTrue(markup.contains("data-featherlight=\"image\"")));
    }

    @Test
    void keepsFirstTitleAndOrderWhenTheSameScreenshotIsPublishedTwice() {
        List<String[]> screenshots = ExtentReportAssets.distinctScreenshots(List.of(
                new String[]{"first state", " " + ReportTestScreenshots.ONE_PIXEL_PNG + " "},
                new String[]{"duplicate state", ReportTestScreenshots.ONE_PIXEL_PNG},
                new String[]{"second state", ReportTestScreenshots.SECOND_PIXEL_PNG}));

        assertAll(
                () -> assertEquals(2, screenshots.size()),
                () -> assertArrayEquals(new String[]{"first state", ReportTestScreenshots.ONE_PIXEL_PNG}, screenshots.get(0)),
                () -> assertArrayEquals(new String[]{"second state", ReportTestScreenshots.SECOND_PIXEL_PNG}, screenshots.get(1)));
    }

    @Test
    void keepsTheFirstTypedScreenshotForEachPayload() {
        List<ReportScreenshot> screenshots = ExtentReportAssets.distinctScreenshotModels(List.of(
                new ReportScreenshot("first state", " " + ReportTestScreenshots.ONE_PIXEL_PNG + " "),
                new ReportScreenshot("duplicate state", ReportTestScreenshots.ONE_PIXEL_PNG),
                new ReportScreenshot("second state", ReportTestScreenshots.SECOND_PIXEL_PNG)));

        assertAll(
                () -> assertEquals(2, screenshots.size()),
                () -> assertEquals("first state", screenshots.get(0).title()),
                () -> assertEquals(ReportTestScreenshots.ONE_PIXEL_PNG, screenshots.get(0).base64Png()),
                () -> assertEquals("second state", screenshots.get(1).title()));
    }

    @Test
    void deduplicatesIdenticalScreenshotPayloadsButKeepsDistinctImages() {
        String repeated = ExtentReportAssets.base64ScreenshotLink(
                "atf-ui-action-screenshot-link", "same", ReportTestScreenshots.ONE_PIXEL_PNG);
        String distinct = ExtentReportAssets.base64ScreenshotLink(
                "atf-ui-action-screenshot-link", "different", ReportTestScreenshots.SECOND_PIXEL_PNG);

        String result = ExtentReportScreenshotDeduplicator.deduplicate(repeated + repeated + distinct);

        assertAll(
                () -> assertEquals(2, count(result, "data-atf-screenshot=\"base64\"")),
                () -> assertEquals(1, count(result, "data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG)),
                () -> assertEquals(1, count(result, "data:image/png;base64," + ReportTestScreenshots.SECOND_PIXEL_PNG)),
                () -> assertEquals(1, count(result, "identical to an earlier captured state in this run")),
                () -> assertTrue(result.contains("data-atf-screenshot-ref=\"")),
                () -> assertTrue(result.contains("data-atf-screenshot-ref=\"screenshot-1\"")),
                () -> assertTrue(result.contains("href=\"#atf-screenshot-screenshot-1\"")),
                () -> assertFalse(result.matches(".*[0-9a-f]{64}.*")),
                () -> assertFalse(result.contains("<img")));
    }

    private static int count(String value, String token) {
        int result = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            result++;
            offset += token.length();
        }
        return result;
    }
}

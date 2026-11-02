package atf.reporting.extent.publication;

import atf.reporting.support.TestReportPaths;
import atf.reporting.support.publication.CapturedVolumeProbe;
import atf.reporting.support.publication.VolumeEvidenceSnapshot;
import atf.testkit.AtfComponentTest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class CapturedVolumePublicationComponentTest {
    @Test void realCheckpointModelPublishesEveryOriginalPngWithinOneGiB() throws Exception {
        Path report = TestReportPaths.report("captured-volume/publication.html");
        Files.createDirectories(report.getParent());
        Path log = report.resolveSibling("publication.log");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Xms64m", "-Xmx1g", "-XX:MaxMetaspaceSize=128m", "-cp", System.getProperty("java.class.path"),
                CapturedVolumeProbe.class.getName(), report.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(180, TimeUnit.SECONDS)) {
            process.destroyForcibly(); process.waitFor(); fail("Volume publication timed out; retained log: " + log);
        }
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), () -> "Volume publication failed; retained log: " + log + "\n" + output);
        assertTrue(Files.size(report) > 100_000_000);
        String html = VolumeEvidenceSnapshot.read(report);
        var doc = Jsoup.parse(html);
        var originals = Files.readAllLines(report.resolveSibling("expected-png-sha256.txt"));
        requirePngs(doc, originals);
        var badPng = doc.clone();
        badPng.select("#test-view a[data-atf-screenshot=base64]").last()
                .attr("href", VolumeEvidenceSnapshot.PREFIX + VolumeEvidenceSnapshot.MARKER + "0".repeat(64));
        assertThrows(AssertionError.class, () -> requirePngs(badPng, originals), "Changing only the final PNG is refused");
        assertEquals(6_634, count(html, "<td>Comparison "));
        assertEquals(577, count(html, "<span class=\"atf-ui-action-number\">"));
        var model = doc;
        requireModel(model);
        var changed = model.clone();
        changed.select("#test-view table.helm-validation-table").last().select("tbody > tr").last().remove();
        assertThrows(AssertionError.class, () -> requireModel(changed), "Losing only the final comparison is refused");
        assertTrue(html.contains("Comparison 320/19"));
        assertTrue(html.contains("controlled-different-suffix"));
        assertTrue(html.contains("id=\"atf-report-behaviour\""));
        assertTrue(html.contains("</html>"));
        assertFalse(html.contains(".atf-png-payload-"));
        for (String metric : List.of("SAMPLED_PEAK_HEAP_BYTES=", "PUBLISH_ALLOCATED_BYTES=", "PUBLISH_MILLIS=", "PNG_BYTES="))
            assertTrue(output.contains(metric), "Missing measured " + metric);
        System.out.println("VOLUME_ARTIFACT=" + report);
        System.out.println(output);
    }
    private static void requirePngs(Document doc, List<String> originals) {
        var observed = new ArrayList<String>();
        for (var image : doc.select("#test-view a[data-atf-screenshot=base64]")) {
            String prefix = VolumeEvidenceSnapshot.PREFIX + VolumeEvidenceSnapshot.MARKER;
            assertTrue(image.attr("href").startsWith(prefix));
            observed.add(image.attr("href").substring(prefix.length()));
        }
        assertEquals(321, originals.size());
        assertEquals(321, observed.size(), "Only captured anchors count; favicon assets are separate");
        for (int i = 0; i < originals.size(); i++) assertEquals(originals.get(i), observed.get(i), "Complete PNG at checkpoint " + i);
    }
    private static void requireModel(Document doc) {
        var owners = doc.select("#test-view .test-list-item > .test-item");
        assertEquals(1, owners.size());
        var tables = owners.getFirst().select("table.helm-validation-table");
        assertEquals(321, tables.size());
        for (int checkpoint = 0; checkpoint < 321; checkpoint++) {
            var table = tables.get(checkpoint); var rows = table.select("tbody > tr");
            int expectedRows = 20 + (checkpoint < 214 ? 1 : 0);
            assertEquals(expectedRows, rows.size(), "Captured comparison inventory " + checkpoint);
            for (int row = 0; row < expectedRows; row++) assertEquals("Comparison " + checkpoint + "/" + row, rows.get(row).child(0).text());
            var validation = table.closest("div.card");
            var images = validation.select("a[data-atf-screenshot=base64]");
            assertEquals(1, images.size(), "PNG remains at its original validation owner");
            assertEquals("atf-screenshot-screenshot-" + (checkpoint + 1), images.getFirst().id());
            assertEquals(checkpoint < 256 ? 2 : 1, validation.select("table.atf-ui-action-table tbody > tr").size());
        }
        assertEquals("controlled-different-suffix", tables.getLast().select("tbody > tr").last().child(2).text());
        assertEquals("❌", tables.getLast().select("tbody > tr").last().child(3).text());
    }
    private static int count(String text, String marker) {
        int count = 0, offset = 0;
        while ((offset = text.indexOf(marker, offset)) >= 0) { count++; offset += marker.length(); }
        return count;
    }
}

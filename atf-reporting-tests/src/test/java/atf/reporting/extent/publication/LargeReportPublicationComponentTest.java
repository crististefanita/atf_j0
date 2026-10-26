package atf.reporting.extent.publication;

import atf.reporting.support.TestReportPaths;
import atf.reporting.support.publication.LargeReportPublicationProbe;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Bounded-heap child JVM proof for inline PNG publication. */
@AtfComponentTest
class LargeReportPublicationComponentTest {
    private static final long MAX_PROFILE_PEAK_HEAP_BYTES = 345L * 1024 * 1024;
    private static final Pattern INLINE_PNG = Pattern.compile(
            "href=\\\"data:image/png;base64,([^\\\"]+)\\\"");

    @Test
    void publishesLargeUnicodeAndRepeatedInlinePngsWithinBoundedHeap() throws Exception {
        Path report = TestReportPaths.report("large-publication-memory-probe.html");
        String output = runProbe(report, "repeated-pixels");
        assertProfileMetrics(output, "repeated-pixels");
        assertPublishedReport(report);
        assertTrue(Files.size(report) > 9_000_000,
                "published report must retain representative Unicode volume");
        String html = Files.readString(report);
        assertTrue(html.contains("atf-screenshot-screenshot-1"),
                "the first PNG must stay actionable");
        assertTrue(html.contains("atf-screenshot-screenshot-2"),
                "the distinct PNG must stay actionable");
    }

    @Test
    void publishesDistinctMediumInlinePngsWithinBoundedHeap() throws Exception {
        Path report = TestReportPaths.report("large-publication-distinct-png-probe.html");
        String output = runProbe(report, "distinct-pngs");
        assertProfileMetrics(output, "distinct-pngs");
        assertPublishedReport(report);
        assertTrue(Files.size(report) > 2_000_000,
                "distinct media profile must remain representative after publication");
        String html = Files.readString(report);
        assertEquals(4, occurrences(html, "href=\"data:image/png;base64,"),
                "four distinct PNG payloads should remain inline after duplicate references collapse");
        assertTrue(inlinePngCharacters(html) > 900_000,
                "distinct PNG payloads must exercise meaningful media volume");
        for (int index = 1; index <= 4; index++) {
            assertTrue(html.contains("id=\"atf-screenshot-screenshot-" + index + "\""),
                    "distinct screenshot " + index + " must remain actionable");
        }
        assertTrue(html.contains("atf-ui-screenshot-duplicate"),
                "repeated references to a distinct PNG should remain actionable without duplicating payloads");
    }

    private static String runProbe(Path report, String profile) throws Exception {
        Path log = report.resolveSibling(report.getFileName() + ".log");
        Files.createDirectories(report.getParent());
        long timeoutSeconds = Long.getLong("atf.test.publication.timeout.seconds", 60L);
        assertTrue(timeoutSeconds > 0, "publication timeout must be positive");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var command = List.of(java, "-Xms64m", "-Xmx384m", "-cp",
                System.getProperty("java.class.path"),
                LargeReportPublicationProbe.class.getName(), report.toString(), profile);
        Process child = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        if (!child.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            child.destroyForcibly();
            child.waitFor();
            fail("bounded publication exceeded " + timeoutSeconds + " s; inspect " + log);
        }
        String output = Files.readString(log);
        assertEquals(0, child.exitValue(), () -> "bounded publication failed: " + output);
        assertTrue(output.contains("PEAK_HEAP_BYTES="), () -> "missing heap observation: " + output);
        assertTrue(output.contains("PUBLISH_MILLIS="), () -> "missing duration observation: " + output);
        return output;
    }

    private static void assertPublishedReport(Path report) throws Exception {
        String html = Files.readString(report);
        assertTrue(html.contains("evidence-end-marker"), "the final evidence must survive publication");
        assertTrue(html.contains("id=\"atf-report-theme\""), "theme must be fully published");
        assertTrue(html.contains("id=\"atf-report-behaviour\""), "runtime must be fully published");
        assertTrue(html.contains("</html>"), "the report must end as a complete document");
    }

    private static void assertProfileMetrics(String output, String profile) {
        assertTrue(output.contains("PROFILE=" + profile), () -> "wrong child profile: " + output);
        assertTrue(metric(output, "SAMPLED_PEAK_HEAP_BYTES=") < MAX_PROFILE_PEAK_HEAP_BYTES,
                () -> "profile left insufficient headroom under the 384 MiB child heap: " + output);
        assertTrue(metric(output, "PUBLISH_MILLIS=") > 0,
                () -> "publication duration was not measured: " + output);
    }

    private static long metric(String output, String name) {
        int start = output.indexOf(name);
        assertTrue(start >= 0, () -> "missing metric " + name + ": " + output);
        start += name.length();
        int end = output.indexOf('\n', start);
        return Long.parseLong(output.substring(start, end < 0 ? output.length() : end).trim());
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(token, index)) >= 0; index += token.length()) count++;
        return count;
    }

    private static long inlinePngCharacters(String html) {
        Matcher matcher = INLINE_PNG.matcher(html);
        long characters = 0;
        while (matcher.find()) characters += matcher.group(1).length();
        return characters;
    }
}

package atf.reporting.standalone;

import atf.reporting.configuration.ReportEnvironment;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.security.SensitiveDataRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import atf.reporting.html.HtmlReportSession;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Standalone proof for typed operational metadata used by Overview/System. */
class ReportingStandaloneEnvironmentSnapshotTest {
    @TempDir Path directory;

    @Test
    void fallbackPublishesCommonFactsWithItsOwnRendererIdentity() {
        try (var session = new HtmlReportSession(directory.resolve("default.html"))) {
            String system = system(session.render());
            for (String key : java.util.List.of("Environment", "Runtime", "OS", "Client", "Reporting", "Assertion preview"))
                assertTrue(system.contains("<td>" + key + "</td>"), key);
            assertTrue(system.contains("<td>Fallback · embedded offline HTML</td>"));
            assertTrue(system.contains("<td>240 characters</td>"));
            assertFalse(system.contains("Extent"));
        }
    }

    @Test
    void fallbackOverridesAreAllowlistedRedactedAndIndependentFromComputedFacts() {
        var input = ReportEnvironmentSnapshot.builder().put(ReportEnvironmentField.ENVIRONMENT, "ACCEPTANCE")
                .put(ReportEnvironmentField.CLIENT, "unknown")
                .put(ReportEnvironmentField.BROWSER, "adapter token=synthetic-metadata-secret")
                .put(ReportEnvironmentField.REPORTING, "Extent supplied by caller")
                .put(ReportEnvironmentField.ASSERTION_PREVIEW, "1 character")
                .put(ReportEnvironmentField.SUITE, "private-metadata-only-suite").build();
        var original = Map.copyOf(input.values());
        try (var session = new HtmlReportSession(directory.resolve("overrides.html"))) {
            session.updateEnvironment(input);
            String rendered = session.render(), system = system(rendered);
            assertTrue(system.contains("<td>Environment</td><td>ACCEPTANCE</td>"));
            assertTrue(system.contains("<td>Client</td><td>unknown</td>"));
            assertTrue(system.contains("<td>240 characters</td>"));
            assertTrue(system.contains("<td>Fallback · embedded offline HTML</td>"));
            assertFalse(rendered.contains("synthetic-metadata-secret"));
            assertFalse(rendered.contains("private-metadata-only-suite"));
            assertThrows(NullPointerException.class, () -> session.updateEnvironment(null));
            assertEquals(rendered, session.render(), "a rejected update cannot mutate the published context");
            assertEquals(original, input.values());
        }
    }

    @Test
    void closedFallbackRefusesMetadataUpdatesWithoutRewritingTheArtifact() throws Exception {
        Path file = directory.resolve("closed.html");
        var session = new HtmlReportSession(file); session.close();
        String before = Files.readString(file);
        assertThrows(IllegalStateException.class, () -> session.updateEnvironment(ReportEnvironmentSnapshot.empty()));
        assertEquals(before, Files.readString(file));
    }

    private static String system(String html) {
        int start = html.indexOf("<h3>System</h3>");
        assertTrue(start >= 0, "the real System heading must exist");
        return html.substring(start, html.indexOf("</table>", start) + "</table>".length());
    }

    @Test
    void publishesClientAsBrowserMetadataWithoutPublishingTestIdentity() {
        var snapshot = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.CLIENT, "Playwright browser")
                .put(ReportEnvironmentField.SUITE, "internal suite")
                .build();

        var systemInfo = ReportEnvironment.with(snapshot, new SensitiveDataRedactor());

        assertAll(
                () -> assertEquals("Client", ReportEnvironmentField.CLIENT.label()),
                () -> assertEquals("Browser", ReportEnvironmentField.CLIENT.group()),
                () -> assertTrue(ReportEnvironmentField.CLIENT.isSystemVisible()),
                () -> assertEquals("Playwright browser",
                        snapshot.displayValues().get(ReportEnvironmentField.CLIENT.label())),
                () -> assertEquals("Playwright browser",
                        systemInfo.get(ReportEnvironmentField.CLIENT.label())),
                () -> assertFalse(systemInfo.containsKey(ReportEnvironmentField.SUITE.label())));
    }
}

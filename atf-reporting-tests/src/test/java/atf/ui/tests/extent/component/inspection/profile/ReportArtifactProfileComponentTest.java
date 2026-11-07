package atf.ui.tests.extent.component.inspection.profile;

import org.junit.jupiter.api.Tag;

import atf.reporting.artifact.*;
import atf.reporting.browser.lifecycle.BrowserProbeProcess;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.ArtifactProfileFixture;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

/** A declared inventory accepts legitimate no-HTTP reports and refuses degradation at the exact original owner. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("report-browser")
class ReportArtifactProfileComponentTest {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path directory;

    static Stream<Arguments> matrix() {
        return Stream.of(false, true).flatMap(fallback -> Stream.of(false, true)
                .flatMap(aggregate -> Stream.of(false, true).map(http -> Arguments.of(fallback, aggregate, http))));
    }

    @Test void aReconstructedProfileResultCannotDropACheckAndItsMatchingDiagnosticTogether() throws Exception {
        Path file = directory.resolve("incomplete-profile-result.html");
        var profile = ArtifactProfileFixture.publish(file, true, false, false);
        var original = ReportArtifactInspector.inspect(file, profile);
        assertTrue(original.isCompliant());
        var checks = new java.util.LinkedHashMap<>(original.checks());
        String removed = checks.keySet().stream().filter(name -> name.endsWith("/row[2] identity")).findFirst().orElseThrow();
        checks.remove(removed);
        var partial = new ReportArtifactInspector.ReportArtifactInspection(file, checks,
                original.diagnostics().stream().filter(diagnostic -> !diagnostic.check().equals(removed)).toList(), "");
        var reconstructed = new ReportArtifactInspector.ProfileInspection(profile, partial);
        assertFalse(reconstructed.isCompliant());
        assertTrue(reconstructed.violations().contains("declared profile check inventory"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void advisoryVerdictCannotBecomePass(boolean fallback) throws Exception {
        Path file = directory.resolve("advisory-" + fallback + ".html");
        var warning = AtfReportStatus.WARNING;
        var group = new ReportArtifactProfile.Assertions("Original advisory",
                List.of(new ReportArtifactProfile.Comparison("advisory numeric", warning)));
        var profile = new ReportArtifactProfile(fallback ? ReportArtifactProfile.Renderer.FALLBACK : ReportArtifactProfile.Renderer.EXTENT,
                List.of(new ReportArtifactProfile.Scenario("AdvisoryFixture", "original advisory", warning,
                        List.of(new ReportArtifactProfile.Phase("TEST BODY", warning, List.of(group))))));
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file)
                : new ExtentReportSession(file);
             var scenario = session.startScenario("AdvisoryFixture", "original advisory", List.of("component"));
             var phase = scenario.startPhase("TEST BODY"); var step = phase.startAssertionStep("Original advisory")) {
            step.assertions(List.of(new AtfAssertion("advisory numeric", "12", "13", false, warning)));
            step.complete(warning); phase.complete(warning); scenario.complete(warning);
        }
        assertTrue(ReportArtifactInspector.inspect(file, profile).isCompliant());
        String selector = fallback ? "#tests table.assertions" : "#test-view table.helm-validation-table";
        refuseMutation(file, profile, "advisory-pass", doc -> doc.select(selector).first()
                .select("tbody > tr > td:last-child").first().text(fallback ? "Pass" : "✅"));
    }

    @ParameterizedTest @MethodSource("matrix")
    void declaredOwnersKeepTheirOriginalRowsAndRequiredEvidence(boolean fallback, boolean aggregate, boolean http) throws Exception {
        Path file = directory.resolve("profile-" + fallback + "-" + aggregate + "-" + http + ".html");
        var profile = ArtifactProfileFixture.publish(file, fallback, aggregate, http);
        var nominal = ReportArtifactInspector.inspectFresh(file, Instant.EPOCH, profile);
        assertTrue(nominal.isCompliant(), () -> nominal.violations().toString());
        assertEquals(http && !fallback, ReportArtifactInspector.inspect(file).isCompliant(), "legacy strict contract remains explicit");
        refuseMutation(file, profile, "runtime", doc -> doc.select("script").remove());
        refuseMutation(file, profile, "theme", doc -> doc.select("style").remove());
        int original = aggregate ? 1 : 0;
        String selector = fallback ? "#tests table.assertions" : "#test-view table.helm-validation-table";
        refuseMutation(file, profile, "table", doc -> doc.select(selector).get(original).remove());
        refuseMutation(file, profile, "last-row", doc -> doc.select(selector).get(original).select("tbody > tr").last().remove());
        refuseMutation(file, profile, "verdict", doc -> doc.select(selector).get(original).select("tbody > tr > td:last-child").first().text(fallback ? "Pass" : "✅"));
        if (aggregate) refuseMutation(file, profile, "sibling", doc ->
                doc.select(selector).first().parent().appendChild(doc.select(selector).get(original)));
        if (http) refuseMutation(file, profile, "required-request", doc -> doc.select(fallback ? ".request-details-content" : ".helm-request-details-content").get(original).remove());
        else refuseMutation(file, profile, "observation-row", doc -> doc.select("[data-atf-node-role=observation] table tbody > tr").get(original * 2).remove());
        var wrongRenderer = new ReportArtifactProfile(fallback ? ReportArtifactProfile.Renderer.EXTENT : ReportArtifactProfile.Renderer.FALLBACK, profile.scenarios());
        assertFalse(ReportArtifactInspector.inspect(file, wrongRenderer).isCompliant());
        BrowserProbeProcess.run(directory, ArtifactProfileBrowserProbe.class, file.toString(),
                Boolean.toString(fallback), Integer.toString(original), Boolean.toString(http));
    }

    private static void refuseMutation(Path original, ReportArtifactProfile profile, String name, Consumer<Document> mutation) throws Exception {
        var document = Jsoup.parse(Files.readString(original)); mutation.accept(document);
        Path file = original.resolveSibling(original.getFileName().toString().replace(".html", "-" + name + ".html"));
        Files.writeString(file, document.outerHtml());
        var result = ReportArtifactInspector.inspect(file, profile);
        assertFalse(result.isCompliant(), name + " must be refused at the original owner");
        assertFalse(result.violations().isEmpty());
    }
}

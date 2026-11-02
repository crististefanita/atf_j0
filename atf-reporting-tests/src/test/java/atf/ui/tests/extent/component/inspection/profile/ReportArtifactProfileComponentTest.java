package atf.ui.tests.extent.component.inspection.profile;

import atf.reporting.artifact.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.ArtifactProfileFixture;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** A declared inventory accepts legitimate no-HTTP reports and refuses degradation at the exact original owner. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportArtifactProfileComponentTest {
    private static final ReportPhaseAuditLocators LOCATORS = new ReportPhaseAuditLocators();
    @TempDir Path directory;
    private Playwright playwright;
    private Browser browser;
    @BeforeAll void start() { playwright = Playwright.create(); browser = playwright.chromium().launch(); }
    @AfterAll void stop() { if (browser != null) browser.close(); if (playwright != null) playwright.close(); }

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
        verifyBrowser(file, fallback, original, http);
    }

    private void verifyBrowser(Path file, boolean fallback, int original, boolean http) {
        try (var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1100, 900))) {
            page.setDefaultTimeout(5_000); page.setDefaultNavigationTimeout(30_000);
            page.navigate(file.toUri() + (fallback ? "" : "#test-view"));
            if (fallback) LOCATORS.testsTab(page, true).click();
            else {
                LOCATORS.firstOwner(page).click();
                page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'");
            }
            var scenario = LOCATORS.scenarios(page, fallback).nth(original);
            if (!fallback) LOCATORS.header(scenario).click();
            var phase = LOCATORS.phases(scenario).first();
            if (!fallback) LOCATORS.header(phase).click();
            Locator group;
            if (http) {
                var request = LOCATORS.requests(phase, fallback).first();
                if (!fallback) LOCATORS.header(request).click();
                var validation = request.locator("[data-atf-node-role=validation]").first();
                if (fallback) validation.locator("details.assertion-evidence > summary").click(); else LOCATORS.header(validation).click();
                group = validation;
            } else {
                group = LOCATORS.assertionStep(phase).first();
                if (fallback) group.locator("summary").first().press("Space"); else LOCATORS.header(group).press("Enter");
            }
            var table = group.locator((fallback ? "table.assertions" : "table.helm-validation-table") + ":visible");
            assertThat(table).isVisible();
            var rows = table.locator("tbody > tr"); assertEquals(3, rows.count());
            assertEquals("13", rows.first().locator("td").nth(2).innerText());
            assertEquals(fallback ? "Fail" : "❌", rows.first().locator("td").nth(3).innerText());
            assertEquals(rows.nth(1).locator("td").nth(1).innerText(), rows.nth(1).locator("td").nth(2).innerText(), "bounded previews may match while the original verdict stays FAIL");
            assertEquals(fallback ? "Fail" : "❌", rows.nth(1).locator("td").nth(3).innerText());
            assertThat(rows.last()).isVisible(); assertTrue(rows.last().innerText().contains("final original comparison"));
        }
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

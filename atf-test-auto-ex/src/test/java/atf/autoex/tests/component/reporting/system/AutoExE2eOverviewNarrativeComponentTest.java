package atf.autoex.tests.component.reporting.system;

import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.config.AutoExProjectRoot;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.tests.extension.AutoExNetworkDocuments;
import atf.autoex.tests.support.reporting.AutoExCatalogApiReportFixture;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.evidence.AtfUiEvidence;
import atf.testkit.evidence.AtfUiEvidenceFlow;
import atf.testkit.evidence.AtfUiScreenshotPolicy;
import atf.testkit.evidence.AtfUiActionDetails;
import atf.testkit.scope.TestRunScope;
import atf.testkit.files.AtfTestResourceReader;
import atf.ui.playwright.PlaywrightUiSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.net.InetSocketAddress;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;
import static atf.reporting.contract.AtfGherkinKeyword.WHEN;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;

/** Exercises the actual JUnit composition root, browser runtime, shared report and scenario correlation. */
@AutoExComponentTest
class AutoExE2eOverviewNarrativeComponentTest {
    @Test void aMixedSuitePublishesCommonMetadataAndOneEffectiveNarrativePerScenario() throws Exception {
        Path root = AutoExProjectRoot.discover();
        Path directory = Files.createDirectories(root.resolve("target/system-suite-proof/" + UUID.randomUUID()));
        try (var fixture = new LocalStore()) {
            var yaml = new ObjectMapper(new YAMLFactory());
            var source = yaml.readTree(AtfTestResourceReader.readResourceUtf8(
                    getClass(), AutoExConfigurationLoader.DEFAULT_RESOURCE));
            var site = (ObjectNode) source.path("automationExercise");
            site.put("baseUrl", fixture.origin());
            ((ObjectNode) site.path("browser")).put("headless", true);
            ((ObjectNode) site.path("reports")).put("uiBaseDirectory", root.relativize(directory.resolve("reports")).toString());
            Path config = directory.resolve("auto-ex.yaml");
            yaml.writeValue(config.toFile(), source);
            Path reporting = directory.resolve("atf.yaml");
            Files.writeString(reporting, "reporting:\n  console:\n    enabled: false\n");
            String oldConfig = System.getProperty(AutoExConfigurationLoader.CONFIG_PROPERTY);
            String oldReporting = System.getProperty("atf.report.config");
            String oldEnabled = System.getProperty("atf.autoex.e2e.enabled");
            try {
                System.setProperty(AutoExConfigurationLoader.CONFIG_PROPERTY, config.toString());
                System.setProperty("atf.report.config", reporting.toString());
                System.setProperty("atf.autoex.e2e.enabled", "true");
                var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(UiOnlyCase.class), selectClass(UiApiCase.class))
                        .configurationParameter(TestRunScope.CONFIGURATION_PARAMETER, "suite")
                        .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                        .configurationParameter("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$OrderAnnotation").build();
                var listener = new SummaryGeneratingListener();
                LauncherFactory.create().execute(request, listener);
                var failures = new StringWriter();
                listener.getSummary().printFailuresTo(new PrintWriter(failures));
                assertEquals(0, listener.getSummary().getContainersFailedCount(), failures.toString());
                assertEquals(2, listener.getSummary().getTestsSucceededCount(), failures.toString());
                List<Path> reports;
                try (var files = Files.walk(directory.resolve("reports"))) {
                    reports = files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".html"))
                            .filter(p -> !p.toString().contains(".atf-extent-session-")).toList();
                }
                assertEquals(1, reports.size(), "The controlled mixed suite owns one Extent artifact");
                String html = Files.readString(reports.getFirst()).replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
                assertTrue(html.contains("GET /api/productsList"), "The local catalog exchange is published in the suite artifact");
                assertTrue(html.contains("Catalog result count"), "The suite artifact retains its business comparison");
                assertTrue(html.contains(ReportEnvironmentSnapshot.MIXED_VALUE));
                for (var field : List.of(ReportEnvironmentField.PROFILE, ReportEnvironmentField.SCREENSHOT_POLICY,
                        ReportEnvironmentField.UI_EVIDENCE_LAYOUT, ReportEnvironmentField.UI_ACTION_DETAILS,
                        ReportEnvironmentField.UI_TIMEOUT, ReportEnvironmentField.API_ENDPOINT)) {
                    assertEquals(1L, Pattern.compile("<td[^>]*>\\s*" + Pattern.quote(field.label()) + "\\s*</td>")
                            .matcher(html).results().count(), "System must have one current row: " + field);
                }
                String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ");
                for (String label : List.of("GIVEN the local store is ready", "AND the local fixture is visible",
                        "WHEN the fixture state is inspected", "AND the same local document remains open",
                        "THEN the local outcome is confirmed", "AND the terminal fixture remains inspectable")) {
                    assertTrue(text.contains(label), "The effective checkpoint wording must be published: " + label);
                }
                assertFalse(text.contains("GIVEN the local fixture is visible"));
                assertFalse(text.contains("THEN the terminal fixture remains inspectable"));
                assertTrue(text.contains("Scenario correlation"));
                fixture.publishReport(reports.getFirst());
                inspectCollapsibleCatalogEvidence(fixture.reportUrl());
            } finally {
                restore(AutoExConfigurationLoader.CONFIG_PROPERTY, oldConfig);
                restore("atf.report.config", oldReporting);
                restore("atf.autoex.e2e.enabled", oldEnabled);
            }
        }
    }

    private static void inspectCollapsibleCatalogEvidence(String reportUrl) throws Exception {
        try (var viewer = PlaywrightUiSession.open(
                AutoExCatalogApiReportFixture.browserConfiguration(), AtfConsoleSink.noOp())) {
            var page = viewer.page();
            page.navigate(reportUrl);
            page.locator("#nav-test").click();
            var reportTest = page.locator("p.name:text-is('UiApiCase')");
            assertTrue(reportTest.isVisible(), "The UI/API suite scenario is listed in Extent");
            reportTest.locator("xpath=ancestor::li[contains(concat(' ', normalize-space(@class), ' '), ' test-item ')][1]")
                    .click();
            var detail = page.locator("#test-view .test-content-detail .detail-body");
            var scenario = detail.locator("a.node:has-text('localStory')").last();
            assertTrue(scenario.isVisible(), "The suite scenario is available to expand");
            scenario.click();
            var phase = detail.locator("a.node:has-text('TEST BODY')").last();
            assertTrue(phase.isVisible(), "The shared test phase appears after expanding the scenario");
            phase.click();
            var checkpoint = detail.locator("a.node:has-text('THEN the local outcome is confirmed')").last();
            assertTrue(checkpoint.isVisible(), "The final UI/API checkpoint appears in the shared phase");
            checkpoint.click();
            var validation = detail.locator("a.node:has-text('API validation')").last();
            assertTrue(validation.isVisible(), "The unified-flow suite exposes its API validation group");
            validation.click();
            var exchanges = detail.locator("a.node").all().stream()
                    .filter(node -> Pattern.matches("GET /api/productsList — Exchange \\d+", node.textContent()))
                    .toList();
            assertEquals(1, exchanges.size(), "The controlled scenario records exactly one catalog GET");
            var exchange = exchanges.getFirst();
            assertTrue(exchange.isVisible(), "The observed local catalog GET appears in the suite report");
            exchange.click();
            var exchangeCard = exchange.locator("xpath=../../..");
            var assertions = exchangeCard.locator("a.node:has-text('API assertions')");
            assertEquals(1, assertions.count(), "Only the selected catalog exchange owns the business assertion group");
            var assertionGroup = assertions.last();
            var assertionCard = assertionGroup.locator("xpath=../../..");
            var assertion = assertionCard.locator("table.helm-validation-table td:has-text('Catalog result count')");
            assertEquals(1, assertion.count());
            assertFalse(assertion.isVisible(), "The suite comparison begins collapsed");
            assertionGroup.click();
            assertTrue(assertion.isVisible(), "Expanding the suite assertion group reveals the comparison");
            var requestDetails = exchangeCard.locator("details.request-evidence summary");
            assertTrue(requestDetails.isVisible(), "The selected suite exchange exposes request details");
            requestDetails.click();
            assertTrue(exchangeCard.locator("table.helm-request-table").isVisible());
            var documentExchanges = detail.locator("a.node").all().stream()
                    .filter(node -> Pattern.matches("GET / — Exchange \\d+", node.textContent()))
                    .toList();
            assertEquals(1, documentExchanges.size(), "The separately observed document GET remains distinct");
            var documentCard = documentExchanges.getFirst().locator("xpath=../../..");
            assertEquals(0, documentCard.locator("a.node:has-text('API assertions')").count(),
                    "The unselected exchange has no copied business comparison");
        }
    }

    public abstract static class LocalCase {
        @Test void localStory(AutoExE2eRun run) {
            run.page().navigate(run.configuration().baseUrl().toString());
            var check = List.of(new AtfAssertion("Local document", "loopback fixture", run.currentUrl(),
                    run.currentUrl().startsWith("http://127.0.0.1:")));
            run.checkpoint(new LocalStep(GIVEN, "the local store is ready"), check);
            verifyCatalogApi(run);
            run.checkpoint(new LocalStep(GIVEN, "the local fixture is visible"), check);
            run.checkpoint(new LocalStep(WHEN, "the fixture state is inspected"), check);
            run.checkpoint(new LocalStep(WHEN, "the same local document remains open"), check);
            run.checkpoint(new LocalStep(THEN, "the local outcome is confirmed"), check);
            run.finalScreenshot(new LocalStep(THEN, "the terminal fixture remains inspectable"), check);
        }

        void verifyCatalogApi(AutoExE2eRun run) { }
    }

    @Order(1) @AutoExE2eTest @AtfTestChannel(TestChannel.UI)
    @AtfUiEvidence(screenshots = AtfUiScreenshotPolicy.NONE)
    public static class UiOnlyCase extends LocalCase { }

    @Order(2) @AutoExE2eTest @AtfTestChannel(TestChannel.UI_API) @AutoExNetworkDocuments({"/"})
    @AtfUiEvidence(flow = AtfUiEvidenceFlow.UNIFIED_FLOW, screenshots = AtfUiScreenshotPolicy.FINAL_ONLY,
            actionDetails = AtfUiActionDetails.SUMMARY_ONLY)
    public static class UiApiCase extends LocalCase {
        @Override void verifyCatalogApi(AutoExE2eRun run) {
            String endpoint = AutoExRoutes.PRODUCTS_API.resolve(run.configuration().baseUrl()).toString();
            var response = (java.util.Map<?, ?>) run.page().evaluate("""
                    async url => {
                      const response = await fetch(url, { headers: { Accept: 'application/json' } });
                      const body = await response.json();
                      return { status: response.status, count: Array.isArray(body.products) ? body.products.length : -1 };
                    }
                    """, endpoint);
            int status = ((Number) response.get("status")).intValue();
            int count = ((Number) response.get("count")).intValue();
            run.apiCheckpoint(AutoExPurchaseTraceStep.CATALOG_API_PRECONDITION,
                    List.of(new AtfAssertion("Catalog result count", "1", Integer.toString(count), status == 200 && count == 1)));
        }
    }

    private record LocalStep(AtfGherkinKeyword keyword, String title) implements AutoExTraceStep { }
    private static void restore(String name, String value) {
        if (value == null) System.clearProperty(name); else System.setProperty(name, value);
    }

    private static final class LocalStore implements AutoCloseable {
        private final HttpServer server;
        private volatile Path publishedReport;
        LocalStore() throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = "<html><body><h1>Local reporting fixture</h1></body></html>".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.createContext(AutoExRoutes.PRODUCTS_API.path(), exchange -> {
                byte[] body = "{\"products\":[{\"name\":\"Local catalog product\"}]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.createContext("/__extent_report", exchange -> {
                Path report = publishedReport;
                if (report == null || !Files.isRegularFile(report)) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                byte[] body = Files.readAllBytes(report);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
        }
        String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        void publishReport(Path report) { publishedReport = report.toAbsolutePath().normalize(); }
        String reportUrl() { return origin() + "/__extent_report"; }
        @Override public void close() { server.stop(0); }
    }
}

package atf.shop.tests.component.recovery;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.reporting.ShopRecoveryExtentReportProbe;
import atf.shop.tests.support.reporting.ShopRecoveryReportObservation;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.config.ShopValidationProfile;
import atf.shop.support.recovery.ShopBackendRecovery;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.testkit.assertions.RecordedAssertions;
import atf.testkit.recovery.*;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.recovery.UiBackendRecovery;
import atf.ui.recovery.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import atf.ui.playwright.PlaywrightUiSession;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.recovery.AtfUiRecovery.Mode.*;
import static atf.shop.support.recovery.ShopBackendRecovery.CATALOG_READINESS;

@ShopComponentTest
class ShopBackendRecoveryComponentTest {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String ORIGIN = "http://127.0.0.1:8123";
    private static final String BODY_CANARY = "backend-private-body-must-not-be-captured";
    @TempDir Path temp;

    private enum RecoveryOutcome {
        RECOVERED, EXHAUSTED, CAPTURE_DISABLED
    }

    private record RecoveryScenario(int responseStatus, RecoveryOutcome outcome,
                                    ShopValidationProfile validationProfile) {
        static RecoveryScenario catalog(ShopValidationProfile profile) {
            return new RecoveryScenario(200, RecoveryOutcome.RECOVERED, profile);
        }

        static RecoveryScenario exhausted() {
            return new RecoveryScenario(503, RecoveryOutcome.EXHAUSTED,
                    ShopValidationProfile.UI_ONLY);
        }

        static RecoveryScenario captureDisabled() {
            return new RecoveryScenario(200, RecoveryOutcome.CAPTURE_DISABLED,
                    ShopValidationProfile.UI_ONLY);
        }

        boolean captureBackendSignals() { return outcome != RecoveryOutcome.CAPTURE_DISABLED; }
        boolean expectedReady() { return outcome == RecoveryOutcome.RECOVERED; }
        boolean renderReport() {
            return outcome == RecoveryOutcome.EXHAUSTED
                    || expectedReady() && validationProfile == ShopValidationProfile.UI_ONLY;
        }
    }

    @Test void yamlAndAnnotationCannotExpandTheReadOnlyPolicy(RecordedAssertions checks) throws Exception {
        var packaged = ShopConfigurationLoader.load().browser().recovery();
        var method = Disabled.class.getDeclaredMethod("read");
        var plan = AtfUiRecoveryResolver.resolve(Disabled.class, method);
        checks.isFalse("Packaged recovery remains OFF", packaged.enabled());
        checks.isFalse("Passive capture requires separate opt-in", packaged.captureBackendSignals());
        checks.isTrue("Packaged operation allowlist is empty", packaged.operations().isEmpty());
        var enabled = ShopConfigurationLoader.load(yaml(true, "read-only", "catalog-readiness")).browser().recovery();
        var restricted = enabled.restrict(plan.permitsConfiguredRecovery(), plan.source());
        checks.isFalse("READ_ONLY method cannot reopen DISABLED class", restricted.enabled());
        checks.isFalse("Annotation cannot reopen YAML OFF", packaged.restrict(true, "method read").enabled());
        checks.equals("Restriction preserves common deadline", enabled.totalTimeout(), restricted.totalTimeout());
        checks.equals("Restriction preserves operation allowlist", enabled.operations(), restricted.operations());
        checks.isTrue("Effective source identifies method", restricted.source().contains("method read"));
        for (String operation : new String[]{"cart-add", "delete-account", "payment", "unknown"}) {
            assertThrows(IllegalStateException.class, () -> ShopConfigurationLoader.load(yaml(true, "read-only", operation)),
                    "Mutations and unknown operations fail before resources: " + operation);
        }
        assertThrows(IllegalStateException.class, () -> ShopConfigurationLoader.load(yaml(true, "unknown", "catalog-readiness")),
                "Unknown recovery profile fails before resources");
    }

    @ParameterizedTest(name = "Catalog recovery · {0}") @EnumSource(ShopValidationProfile.class)
    void delayedCatalogIsReobservedWithoutAnotherClickOrRequest(ShopValidationProfile validationProfile,
                                                                 RecordedAssertions checks) throws Exception {
        runBrowser(checks, RecoveryScenario.catalog(validationProfile));
    }
    @Test void exhaustedBackendKeepsOneDispatchAndAnExplicitReason(RecordedAssertions checks) throws Exception {
        runBrowser(checks, RecoveryScenario.exhausted());
    }
    @Test void missingCaptureRefusesEvenWhenRecoveryIsEnabled(RecordedAssertions checks) throws Exception {
        runBrowser(checks, RecoveryScenario.captureDisabled());
    }

    @Test void contextCannotAcquireAnotherControllerToRenewTheInvocationOwner(RecordedAssertions checks) throws Exception {
        var configuration = ShopConfigurationLoader.load(yaml(true, "read-only", "catalog-readiness"));
        var profile = atf.reporting.evidence.profile.ReportEvidenceProfile.forScreenshotPolicy(
                atf.reporting.evidence.ReportScreenshotPolicy.NONE);
        var context = atf.shop.tests.support.lifecycle.ShopE2eContext.open(configuration, profile);
        var actions = context.actions();
        try (context) {
            var first = context.backendRecovery(actions);
            checks.isTrue("One controller per context", first == context.backendRecovery(actions));
            assertThrows(IllegalStateException.class, () -> context.backendRecovery(context.actions()),
                    "A second recorder cannot establish another invocation budget");
        }
        assertThrows(IllegalStateException.class, () -> context.backendRecovery(actions),
                "A closed context cannot reopen recovery");
    }

    private void runBrowser(RecordedAssertions checks, RecoveryScenario scenario) throws Exception {
        try (var fixture = new CatalogFixture(scenario.responseStatus())) {
            var configured = ShopConfigurationLoader.load(
                    yaml(scenario.captureBackendSignals(), "read-only", "catalog-readiness", fixture.origin()));
            var configuration = configured.withValidationProfile(scenario.validationProfile());
            checks.equals("Presentation preserves recovery policy", configured.browser().recovery(), configuration.browser().recovery());
            checks.equals("Authenticated profile preserves recovery", configuration.browser().recovery(),
                    configuration.forAuthenticatedE2e(configuration.accountFile(), true, 100).browser().recovery());
            var actions = new PlaywrightActionSequence();
            var typedTraces = new ArrayList<UiRecoveryTrace>();
            try (var browser = PlaywrightUiSession.open(configuration.browser())) {
                var page = browser.page();
                page.navigate(fixture.origin());
                try (var recovery = openRecovery(page, configuration, actions,
                        scenario.renderReport() ? typedTraces : null);
                     var scope = recovery.begin(ShopBackendRecovery.CATALOG_READINESS)) {
                    // Scope starts before the one dispatch; a readiness probe never receives an action.
                    page.setDefaultTimeout(scope.remaining().toMillis());
                    new atf.shop.locators.ShopLocators().homeSearchSubmit(page).locator().click();
                    if (scenario.expectedReady()) {
                        var result = scope.await(() -> new atf.shop.locators.ShopLocators().productName(page).locator().isVisible() ? UiRecoveryTrace.Dom.READY : UiRecoveryTrace.Dom.PENDING);
                        checks.equals("Delayed catalog has recovered decision", UiRecoveryTrace.Decision.RECOVERED, result.decision());
                        checks.isTrue("First evaluation counted before reobservation", result.attempt() > 1);
                        checks.isTrue("Probe time stays inside total duration", result.probeTime().compareTo(result.totalTime()) <= 0);
                    } else {
                        var failure = assertThrows(UiRecoveryException.class,
                                () -> scope.await(() -> UiRecoveryTrace.Dom.PENDING), "No unsafe fallback after refusal or exhaustion");
                        checks.equals("Terminal recovery reason",
                                scenario.captureBackendSignals()
                                        ? UiRecoveryTrace.Reason.ATTEMPTS : UiRecoveryTrace.Reason.CAPTURE_DISABLED,
                                failure.trace().reason());
                    }
                    checks.equals("Original pointer action count", 1, ((Number) page.evaluate("window.clicks")).intValue());
                    checks.equals("Application GET dispatch count", 1, fixture.requests.get());
                    checks.isFalse("Recovery has decision evidence", actions.snapshot().isEmpty());
                    checks.isTrue("Every recovery record is INFO", actions.snapshot().stream().allMatch(row -> row.status().name().equals("INFO")));
                    checks.isTrue("Observation explicitly excludes business replay", actions.snapshot().stream()
                            .allMatch(row -> row.observed().contains("no business action replay")));
                    checks.isFalse("Backend body is excluded from evidence", actions.snapshot().toString().contains(BODY_CANARY));
                    checks.isTrue("Policy and budget are published", actions.snapshot().stream()
                            .allMatch(row -> row.expected().contains("profile=read-only") && row.expected().contains("totalTimeout=PT3S")));
                }
                if (scenario.renderReport()) {
                    var records = actions.snapshot();
                    var observation = ShopRecoveryReportObservation.create(typedTraces, records);
                    checks.equals("Observation title exposes the terminal recovery outcome",
                            "Backend recovery trace · " + scenario.outcome(), observation.title());
                    checks.equals("One typed recovery observation maps to each INFO action", typedTraces.size(), records.size());
                    checks.equals("All attempts have six explicit report fields", typedTraces.size() * 6,
                            observation.fields().size());
                    for (int index = 0; index < typedTraces.size(); index++) {
                        var trace = typedTraces.get(index);
                        var fields = observation.fields();
                        int firstField = index * 6;
                        checks.equals("Semantic recovery operation is explicit: attempt " + trace.attempt(),
                                CATALOG_READINESS, fields.get(firstField).value());
                        checks.equals("Observation event is labeled separately: attempt " + trace.attempt(),
                                "Backend observation #" + trace.attempt(), fields.get(firstField + 1).value());
                        checks.equals("Typed timings retain their measured durations: attempt " + trace.attempt(),
                                "probe=" + trace.probeTime() + "; backoff=" + trace.backoffTime()
                                        + "; total=" + trace.totalTime(), fields.get(firstField + 4).value());
                        checks.equals("Recovery trace stays informational: attempt " + trace.attempt(),
                                "INFO", fields.get(firstField + 5).value());
                    }
                    checks.equals("Report label agrees with terminal decision", scenario.outcome().name(),
                            typedTraces.getLast().decision().name());
                    checks.isFalse("Typed report projection excludes the private response body",
                            observation.fields().toString().contains(BODY_CANARY));
                    checks.observe(observation);
                    verifyRenderedRecoveryReport(checks, configuration, actions, scenario.outcome().name());
                }
            }
        }
    }

    private static UiBackendRecovery openRecovery(com.microsoft.playwright.Page page,
                                                 atf.shop.config.ShopConfiguration configuration,
                                                 PlaywrightActionSequence actions,
                                                 List<UiRecoveryTrace> typedTraces) {
        if (typedTraces == null) return ShopBackendRecovery.open(page, configuration, actions);
        var policy = configuration.browser().recovery();
        var operation = new UiRecoveryOperation(CATALOG_READINESS, UiRecoveryOperation.Safety.READ_ONLY,
                configuration.baseUrl(), configuration.api().endpoint().resolve("/products"), "GET");
        return new UiBackendRecovery(page, policy, List.of(operation), trace -> {
            typedTraces.add(trace);
            actions.information("BE observation #" + trace.attempt(), trace.operation(),
                    "profile=" + policy.profile() + "; maxAttempts=" + policy.maxAttempts()
                            + "; totalTimeout=" + policy.totalTimeout() + "; source=" + policy.source(),
                    trace.description());
        });
    }

    private void verifyRenderedRecoveryReport(RecordedAssertions checks,
                                             atf.shop.config.ShopConfiguration configuration,
                                             PlaywrightActionSequence actions, String outcome) throws Exception {
        var records = actions.snapshot();
        String stepTitle = "Backend recovery " + outcome + " after " + records.size() + " attempts";
        Path reportDirectory = Path.of("target", "shop-runtime-reports", "recovery-audit");
        Files.createDirectories(reportDirectory);
        Path reportPath = reportDirectory.resolve("shop-recovery-" + UUID.randomUUID() + ".html");
        try (var session = new ExtentReportSession(reportPath)) {
            var scenario = UiExtentReportSession.adaptScenario(
                    session.startScenario("ShopRecoveryReport", "recovery audit scenario", List.of("recovery")),
                    ReportEvidenceProfile.forScreenshotPolicy(ReportScreenshotPolicy.NONE), "recovery-audit");
            var phase = scenario.startPhase("TEST BODY");
            phase.step(AtfGherkinKeyword.THEN, stepTitle, "local catalog fixture", "",
                    List.of(new AtfAssertion("Recovery outcome", outcome,
                            outcome + " after " + records.size() + " attempts", true)),
                    ReportModelAdapters.uiActions(records), UiApiValidationEvidence.empty(),
                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(reportPath);
        checks.isFalse("Rendered report excludes the private response body", html.contains(BODY_CANARY));
        checks.isFalse("Rendered report excludes the local fixture URL", html.contains("127.0.0.1"));
        try (var viewer = PlaywrightUiSession.open(configuration.browser())) {
            ShopRecoveryExtentReportProbe.verify(viewer.page(), reportPath, stepTitle, records, checks);
        }
    }
    private Path yaml(boolean capture, String profile, String operation) throws Exception {
        return yaml(capture, profile, operation, ORIGIN);
    }
    private Path yaml(boolean capture, String profile, String operation, String origin) throws Exception {
        ObjectNode document;
        try (var input = getClass().getResourceAsStream("/atf-practice-shop.yaml")) { document = (ObjectNode) YAML.readTree(input); }
        var root = (ObjectNode) document.path("practiceShop");
        root.put("baseUrl", origin);
        ((ObjectNode) root.path("api")).put("endpoint", origin);
        root.put("headless", true);
        var recovery = root.putObject("recovery");
        recovery.put("enabled", true).put("profile", profile).put("maxAttempts", 8)
                .put("totalTimeout", "PT3S").put("backoff", "PT0.04S").put("captureBackendSignals", capture);
        recovery.putArray("operations").add(operation);
        Path file = temp.resolve(java.util.UUID.randomUUID() + ".yaml");
        YAML.writeValue(file.toFile(), document);
        return file;
    }
    private static final class CatalogFixture implements AutoCloseable {
        private final HttpServer server;
        final AtomicInteger requests = new AtomicInteger();
        CatalogFixture(int status) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/products", exchange -> {
                requests.incrementAndGet();
                byte[] body = BODY_CANARY.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.createContext("/", exchange -> {
                byte[] body = """
                    <button id="submit_search" data-test="search-submit">Read catalog once</button>
                    <div class="features_items"><div class="productinfo"><p id="ready" data-test="product-name" hidden>Catalog ready</p></div></div>
                    <script>window.clicks=0;document.querySelector('button').onclick=()=>{
                      window.clicks++;fetch('/products').then(r=>{
                        if(r.ok)setTimeout(()=>document.querySelector('#ready').hidden=false,120);
                      });
                    };</script>
                    """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
        }
        String origin() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        @Override public void close() { server.stop(0); }
    }
    @AtfUiRecovery(DISABLED)
    static class Disabled { @AtfUiRecovery(READ_ONLY) void read() { } }
}

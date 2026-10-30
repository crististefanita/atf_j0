package atf.autoex.tests.component.reporting;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.config.AutoExConfigurationLoader;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.scope.TestRunScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Real consumer extensions against a local HTTP failure, with fresh browser state per invocation. */
@AutoExComponentTest
@org.junit.jupiter.api.parallel.ResourceLock("java.lang.System.properties")
class AutoExEvidenceProfileIsolationComponentTest {
    @Test
    void failureAndSuccessKeepTheirOwnProfileAcrossIndividualAndSharedLaunches() throws Exception {
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            boolean api = exchange.getRequestURI().getPath().equals("/products");
            boolean success = exchange.getRequestURI().getRawQuery() != null;
            if (api) requests.incrementAndGet();
            byte[] body = (api ? "{\"status\":\"" + (success ? "ready" : "unavailable") + "\"}"
                    : "<html><body><h1>Profile fixture</h1></body></html>").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", api ? "application/json" : "text/html");
            exchange.sendResponseHeaders(api && !success ? 500 : 200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            verify(origin, false, UiFailure.class);
            verify(origin, false, ApiFailure.class);
            verify(origin, true, UiFailure.class, ApiSuccess.class);
            verify(origin, true, ApiFailure.class, UiSuccess.class);
            assertEquals(6, requests.get(), "one GET per invocation; no failure replay");
        } finally { server.stop(0); }
    }

    private static void verify(String origin, boolean shared, Class<?>... owners) throws Exception {
        Path root = atf.autoex.runtime.config.AutoExProjectRoot.discover();
        Path directory = Files.createDirectories(root.resolve("target/profile-isolation/" + UUID.randomUUID()));
        var yaml = new ObjectMapper(new YAMLFactory());
        var document = yaml.readTree(atf.testkit.files.AtfTestResourceReader.readResourceUtf8(
                AutoExEvidenceProfileIsolationComponentTest.class, "atf-auto-ex.yaml"));
        var consumer = (ObjectNode) document.path("automationExercise");
        consumer.put("baseUrl", origin);
        ((ObjectNode) consumer.path("reports")).put("uiBaseDirectory", root.relativize(directory.resolve("reports")).toString());
        Path configuration = directory.resolve("consumer.yaml");
        yaml.writeValue(configuration.toFile(), document);
        String previousConfig = System.getProperty("atf.autoex.config"), previousEnabled = System.getProperty("atf.autoex.e2e.enabled");
        var listener = new SummaryGeneratingListener();
        try {
            System.setProperty("atf.autoex.config", configuration.toString());
            System.setProperty("atf.autoex.e2e.enabled", "true");
            var request = LauncherDiscoveryRequestBuilder.request()
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .configurationParameter("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$OrderAnnotation");
            if (shared) request.configurationParameter(TestRunScope.CONFIGURATION_PARAMETER, "suite");
            for (var owner : owners) request.selectors(selectClass(owner));
            LauncherFactory.create().execute(request.build(), listener);
        } finally {
            restore("atf.autoex.config", previousConfig); restore("atf.autoex.e2e.enabled", previousEnabled);
        }
        var result = listener.getSummary();
        assertEquals(0, result.getContainersFailedCount(), "all selected containers complete");
        assertEquals(1, result.getTestsFailedCount(), "one controlled HTTP failure per launch");
        assertEquals(owners.length - 1, result.getTestsSucceededCount(), "all selected healthy siblings pass");
        assertTrue(result.getFailures().getFirst().getException().getMessage().contains("Profile HTTP response"),
                "the observed remote failure remains the primary cause");
        List<Path> reports;
        try (var files = Files.walk(directory)) {
            reports = files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".html"))
                    .filter(p -> !p.toString().contains(".atf-extent-session-")).toList();
        }
        assertEquals(1, reports.size(), "one HTML per individual or shared launch");
        String html = Files.readString(reports.getFirst());
        for (var owner : owners) assertTrue(html.contains(owner.getSimpleName()));
        assertTrue(html.contains("Profile HTTP response"));
        assertTrue(html.contains("500"));
        assertTrue(html.contains("data-atf-evidence-status=\"fail\""));
        assertTrue(html.contains("data:image/png;base64,"), "the failed screen remains inspectable");
        boolean api = Arrays.stream(owners).anyMatch(owner -> owner.getSimpleName().startsWith("Api"));
        assertEquals(api, html.contains("API network inspector"));
        if (shared) assertTrue(html.contains(atf.reporting.configuration.ReportEnvironmentSnapshot.MIXED_VALUE));
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
    }

    public abstract static class Case {
        @Test void observedFailureIsNotInheritedByTheNextBrowser(atf.autoex.runtime.AutoExE2eRun run) {
            if (!"about:blank".equals(run.currentUrl())) throw new AssertionError("browser was reused");
            run.navigate(run.configuration().baseUrl());
            String actual = observe(run.page(), getClass().getSimpleName().endsWith("Success"));
            run.checkpoint(new atf.autoex.support.trace.AutoExTraceStep() {
                public AtfGherkinKeyword keyword() { return AtfGherkinKeyword.THEN; }
                public String title() { return "Profile HTTP response"; }
            }, List.of(new AtfAssertion("Profile HTTP response", "200", actual, "200".equals(actual))));
        }
    }
    @atf.autoex.tests.extension.AutoExE2eTest @AtfTestChannel(TestChannel.UI)
    @org.junit.jupiter.api.Order(1) public static class UiFailure extends Case { }
    @atf.autoex.tests.extension.AutoExE2eTest @AtfTestChannel(TestChannel.UI_API)
    @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    @org.junit.jupiter.api.Order(1) public static class ApiFailure extends Case { }
    @atf.autoex.tests.extension.AutoExE2eTest @AtfTestChannel(TestChannel.UI)
    @org.junit.jupiter.api.Order(2) public static class UiSuccess extends Case { }
    @atf.autoex.tests.extension.AutoExE2eTest @AtfTestChannel(TestChannel.UI_API)
    @atf.autoex.tests.extension.AutoExNetworkDocuments({"/"})
    @org.junit.jupiter.api.Order(2) public static class ApiSuccess extends Case { }

    private static String observe(com.microsoft.playwright.Page page, boolean success) {
        return String.valueOf(page.evaluate("""
                async success => {
                    if (localStorage.getItem('atf-profile-owner')) throw new Error('browser state leaked');
                    localStorage.setItem('atf-profile-owner', 'current');
                    return (await fetch('/products' + (success ? '?ok' : ''))).status;
                }
                """, success));
    }
}

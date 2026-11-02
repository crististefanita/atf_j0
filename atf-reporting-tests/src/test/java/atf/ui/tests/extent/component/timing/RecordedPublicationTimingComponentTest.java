package atf.ui.tests.extent.component.timing;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.*;
import atf.reporting.contract.*;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.model.timing.*;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.locators.diagnostic.ReportPhaseAuditLocators;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Captured execution/transport and controlled publication clocks stay distinct without wall-clock thresholds. */
@AtfComponentTest
class RecordedPublicationTimingComponentTest {
    private static final ReportPhaseAuditLocators LOCATORS = new ReportPhaseAuditLocators();
    @TempDir Path directory;
    static Stream<Arguments> matrix() { return Stream.of(false,true).flatMap(f -> Stream.of(false,true).map(a -> Arguments.of(f,a))); }

    @ParameterizedTest @MethodSource("matrix")
    void publicationCannotBecomeCapturedExecutionOrTransport(boolean fallback, boolean aggregate) throws Exception {
        Path file = directory.resolve("captured-" + fallback + "-" + aggregate + ".html");
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var once = new AtomicBoolean();
        AtfConsoleSink console = event -> {
            if (event.type() == AtfConsoleEvent.Type.START && event.scope() == AtfConsoleEvent.Scope.REQUEST && once.compareAndSet(false, true)) {
                entered.countDown();
                try { if (!release.await(30, TimeUnit.SECONDS)) throw new AssertionError("Publication barrier was not released"); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
            }
        };
        try (var executor = Executors.newSingleThreadExecutor()) {
            var task = CompletableFuture.runAsync(() -> publish(file, fallback, aggregate, console), executor);
            try { assertTrue(entered.await(20, TimeUnit.SECONDS)); assertFalse(task.isDone(), "publication remains gated after execution capture"); }
            finally { release.countDown(); }
            task.get(30, TimeUnit.SECONDS);
        }
        if (!fallback) stampPublicationClocks(file);
        try (var playwright = Playwright.create(); var browser = playwright.chromium().launch(); var page = browser.newPage()) {
            page.setDefaultTimeout(5_000); page.setDefaultNavigationTimeout(30_000);
            page.navigate(file.toUri() + (fallback ? "" : "#test-view"));
            if (fallback) LOCATORS.testsTab(page,true).click();
            else { LOCATORS.firstOwner(page).click(); page.waitForFunction("() => document.documentElement.getAttribute('data-atf-test-view-ready') === 'true'"); }
            var scenario = LOCATORS.scenarios(page,fallback).first();
            if (!fallback) LOCATORS.header(scenario).click();
            var phase = LOCATORS.phases(scenario).first();
            if (!fallback) LOCATORS.header(phase).click();
            var metadata = phase.locator("[data-atf-execution-duration-ms]").first();
            assertEquals("11", metadata.getAttribute("data-atf-execution-duration-ms"));
            assertTrue(Long.parseLong(metadata.getAttribute("data-atf-publication-duration-ms")) >= 700);
            var phaseBadge = phase.locator(fallback ? ":scope > h3 > .badge" : ":scope > .card-header .badge-default").first();
            assertEquals("11", phaseBadge.getAttribute("data-duration-ms")); assertEquals("execution", phaseBadge.getAttribute("data-duration-source"));
            var request = LOCATORS.requests(phase,fallback).first();
            var badge = request.locator(fallback ? ":scope > .node-title > .badge" : ":scope > .card-header .badge-default").first();
            assertEquals(1, badge.count(), "the request must expose its captured transport badge");
            assertEquals("7", badge.getAttribute("data-duration-ms")); assertEquals("transport", badge.getAttribute("data-duration-source"));
            if (fallback) assertThat(request.locator(":scope > .node-title > .atf-duration-origin")).hasText("Transport");
            else assertTrue(badge.evaluate("e => getComputedStyle(e,'::after').content").toString().toLowerCase(java.util.Locale.ROOT).contains("transport"));
            if (!fallback) {
                LOCATORS.header(request).click();
                var validation = request.locator("[data-atf-node-role=validation]").first();
                var publication = validation.locator(":scope > .card-header .badge-default").first();
                assertEquals("903", publication.getAttribute("data-duration-ms")); assertEquals("publication", publication.getAttribute("data-duration-source"));
                assertTrue(publication.evaluate("e => getComputedStyle(e,'::after').content").toString().toLowerCase(java.util.Locale.ROOT).contains("publication"));
            }
            assertCaptionSpacing(phaseBadge);
            if (!fallback) assertCaptionSpacing(badge);
            assertEquals("11", phaseBadge.getAttribute("data-duration-ms"), "descendant publication/transport must not be summed into captured execution");
        }
    }

    private static void assertCaptionSpacing(Locator badge) {
        double gap = ((Number) badge.evaluate("e => parseFloat(getComputedStyle(e, '::after').marginInlineStart)")).doubleValue();
        assertTrue(gap >= 3, "the visible timing source must be separated from the duration value");
    }

    private static void publish(Path file, boolean fallback, boolean aggregate, AtfConsoleSink console) {
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file,ReportingSettings.defaults(),console)
                : new ExtentReportSession(file,ReportingSettings.defaults(),new SensitiveDataRedactor(),console)) {
            for (int index=0;index<(aggregate?2:1);index++) try (var scenario=session.startScenario("CapturedTiming","original timing "+index,List.of("component","mock"));
                    var phase=scenario.startPhase("TEST BODY")) {
                phase.executionTiming(new ReportPhaseTiming(Duration.ofMillis(11),1,
                        Optional.of(new ReportPhaseEvidenceTiming(Duration.ofMillis(3),Duration.ofMillis(700)))));
                try (var request=phase.startRequest("GET /fixture/clock","GET","/fixture/clock")) {
                    request.detail("publication barrier","authored after capture");
                    request.execution(new ReportExecutionEvidence(null,1,Duration.ofMillis(7),List.of(),List.of(),null,null));
                    try (var validation=request.startValidation("Validate original clock")) {
                        validation.assertions(List.of(new AtfAssertion("final clock comparison","ready","ready",true))); validation.complete(AtfReportStatus.PASS);
                    }
                    request.complete(AtfReportStatus.PASS);
                }
                phase.complete(AtfReportStatus.PASS); scenario.complete(AtfReportStatus.PASS);
            }
        }
    }

    private static void stampPublicationClocks(Path file) throws Exception {
        var doc=Jsoup.parse(Files.readString(file));
        for(var header:doc.select("#test-view .card-header")) {
            var title=header.selectFirst(".card-title > a.node > span");
            if(title==null)continue;
            var badge=header.selectFirst(".card-toolbar .badge-default");
            if(badge!=null&&title.text().equals("GET /fixture/clock"))badge.text("00:00:901");
            if(badge!=null&&title.text().equals("Validate original clock"))badge.text("00:00:903");
        }
        Files.writeString(file,doc.outerHtml());
    }
}

package atf.reporting.support.request;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRetryEvent;
import atf.ui.tests.extent.support.audit.api.ApiJsonEvidenceAuditor;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.api.request.ReportRequestHeaders;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.api.evidence.ReportBodyAvailability.ABSENT;
import static atf.reporting.api.evidence.ReportBodyAvailability.AVAILABLE;
import static atf.reporting.api.evidence.ReportBodyAvailability.NOT_CAPTURED;

public final class ObservedRequestFixture {
    public enum Renderer { EXTENT, FALLBACK, UI_API }
    public enum Sample {
        OBSERVED_ABSENT("", ABSENT, "{\"response\":true}", AVAILABLE),
        OBSERVED_UNCAPTURED("capture unavailable", NOT_CAPTURED, "capture unavailable", NOT_CAPTURED),
        OBSERVED_NULL("null", AVAILABLE, "null", AVAILABLE),
        OBSERVED_EXPLICIT("{\"value\":\"ok\",\"password\":\"request-secret\"}", AVAILABLE, "{\"response\":true}", AVAILABLE),
        CONFIGURED_DEFAULTS("{}", AVAILABLE, "{}", AVAILABLE),
        EMPTY_RESPONSE("", ABSENT, "", ABSENT);

        public final String body;
        public final ReportBodyAvailability availability;
        final String response;
        final ReportBodyAvailability responseAvailability;

        Sample(String body, ReportBodyAvailability availability, String response,
               ReportBodyAvailability responseAvailability) {
            this.body = body; this.availability = availability;
            this.response = response; this.responseAvailability = responseAvailability;
        }

        public ReportRequest request() {
            Map<String, String> headers = this == OBSERVED_EXPLICIT
                    ? Map.of("Accept", "text/csv", "Content-Type", "application/json; charset=UTF-8",
                            "Authorization", "Bearer header-secret")
                    : Map.of("X-Observed", "yes");
            var headerEvidence = this == CONFIGURED_DEFAULTS
                    ? ReportRequestHeaders.configured(Map.of("Accept", "application/json",
                            "Content-Type", "application/json"), headers)
                    : ReportRequestHeaders.observed(headers);
            return ReportRequest.builder("GET", "/observed/" + name().toLowerCase(java.util.Locale.ROOT))
                    .headers(headerEvidence).body(body, availability).build();
        }

        public ReportExecutionEvidence execution() {
            var request = request();
            var response = new ReportResponse(this == EMPTY_RESPONSE ? 204 : 200, this.response,
                    Map.of("Content-Type", "application/json"), Duration.ofMillis(3),
                    "https://fixture.invalid" + request.path(), responseAvailability);
            boolean retried = this == CONFIGURED_DEFAULTS;
            var history = retried ? List.of(
                    new ReportAttemptRecord(1, 503, Duration.ofMillis(2), "RETRY", "retryable fixture"),
                    new ReportAttemptRecord(2, 200, Duration.ofMillis(3), "SUCCESS", "recovered"))
                    : List.<ReportAttemptRecord>of();
            var retries = retried ? List.of(new ReportRetryEvent(
                    1, 503, "Retry-After", Duration.ofMillis(1), "retryable fixture"))
                    : List.<ReportRetryEvent>of();
            return new ReportExecutionEvidence(response, retried ? 2 : 1, Duration.ofMillis(retried ? 6 : 3), retries, history,
                    request, ReportTransportProvenance.ACCEPTANCE_MOCK);
        }
    }

    public record Published(Path path, String html, AtfReportSemanticSnapshot snapshot) { }
    private ObservedRequestFixture() { }

    public static ApiJsonEvidenceAuditor.Result inspectBody(
            Published published, Renderer renderer) {
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), 
                     new BrowserType.LaunchOptions().setHeadless(true)))) {
            try {
                var browser = browserLease.value();
                var page = BrowserResources.newPage(browser);
                page.navigate(published.path().toUri().toString() + "#test-view");
                if (renderer == Renderer.FALLBACK) {
                    page.locator("[data-tab='tests']").click();
                } else {
                    page.locator("#test-view .test-item").first().click();
                    page.getByRole(AriaRole.BUTTON,
                            new Page.GetByRoleOptions().setName("Expand all nodes").setExact(true)).click();
                }
                var metadata = page.locator("[data-atf-request-method]").first();
                var request = metadata.locator("xpath=ancestor::*[@data-atf-node-role='request'][1]");
                return new ApiJsonEvidenceAuditor().inspect(request);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }

    public static Published publish(Renderer renderer, List<Sample> samples) throws Exception {
        Path requested = TestReportPaths.report("observed-request-" + renderer + "-" + UUID.randomUUID() + ".html");
        Path actual = requested;
        AtfReportSemanticSnapshot snapshot;
        if (renderer == Renderer.UI_API) {
            try (var session = UiExtentReportSession.openConfigured(requested.getParent(),
                    "ui-observed-request-evidence.html", AtfConsoleSink.noOp())) {
                actual = session.reportPath();
                for (Sample sample : samples) {
                    var scenario = session.startScenario("ObservedRequestContract", "audit.component",
                            sample.name(), List.of("component", "controlled-evidence"));
                    var phase = scenario.startPhase("TEST BODY");
                    var execution = sample.execution();
                    int expectedStatus = sample == Sample.EMPTY_RESPONSE ? 204 : 200;
                    var api = new UiApiValidationEvidence(List.of(new AtfAssertion("Captured HTTP status",
                            Integer.toString(expectedStatus), Integer.toString(execution.response().status()),
                            expectedStatus == execution.response().status())), List.of(
                            new UiApiValidationEvidence.Exchange(execution, List.of())));
                    phase.apiTrace(THEN, "the captured request is published without invented evidence",
                            sample.request().path(), api, AtfReportStatus.PASS);
                    phase.complete(AtfReportStatus.PASS);
                    scenario.complete(AtfReportStatus.PASS);
                }
                snapshot = session.semanticSnapshot();
            }
        } else {
            try (AtfReportPorts.Session session = renderer == Renderer.EXTENT
                    ? new ExtentReportSession(requested) : new HtmlReportSession(requested)) {
                for (Sample sample : samples) {
                    var scenario = session.startScenario("ObservedRequestContract", sample.name(), List.of("component"));
                    var phase = scenario.startPhase("TEST BODY");
                    var request = sample.request();
                    try (var node = phase.startRequest("captured request", request.method(), request.path())) {
                        ApiExchangeEvidence.publish(node, sample.execution());
                        node.complete(AtfReportStatus.PASS);
                    }
                    phase.complete(AtfReportStatus.PASS);
                    scenario.complete(AtfReportStatus.PASS);
                }
                snapshot = session instanceof ExtentReportSession extent ? extent.semanticSnapshot()
                        : ((HtmlReportSession) session).semanticSnapshot();
            }
        }
        String html = Files.readString(actual).replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        return new Published(actual, html, snapshot);
    }

    public static AtfReportSemanticSnapshot.RequestSnapshot exchange(Published published) {
        var checkpoint = (AtfReportSemanticSnapshot.RequestSnapshot) published.snapshot()
                .classes().getFirst().scenarios().getFirst().phases().getFirst().nodes().getFirst();
        return checkpoint.validations().isEmpty() ? checkpoint : checkpoint.validations().getFirst().requests().getFirst();
    }
}

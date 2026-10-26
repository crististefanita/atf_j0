package atf.reporting.api.evidence;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;

/** Controlled HTTP snapshots; only the real publisher is exercised, never a remote service. */
final class ApiBodyArtifactFixture {
    private static final ReportingSettings FULL_BODY = new ReportingSettings(
            ReportingSettings.DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS, ReportingSettings.DISABLE_BODY_DISPLAY_LIMIT);
    enum Projection { EXTENT, FALLBACK, UI_API }
    record Published(String html, List<AtfConsoleEvent> console) { }

    private ApiBodyArtifactFixture() { }

    static ReportExecutionEvidence exchange(String body, String mime, ReportBodyAvailability availability) {
        var request = new ReportRequest("POST", "/body-audit", Map.of(), Map.of("Content-Type", mime),
                "none", body, null, availability);
        var response = new ReportResponse(200, body, Map.of("Content-Type", mime), Duration.ZERO,
                "https://audit.example.test/body-audit", availability);
        return new ReportExecutionEvidence(response, 1, Duration.ZERO, List.of(), List.of(),
                request, ReportTransportProvenance.ACCEPTANCE_MOCK);
    }

    static Published publish(String name, Projection projection, List<ReportExecutionEvidence> exchanges)
            throws Exception {
        Path report = TestReportPaths.report("body-" + name + "-" + projection + ".html");
        var events = new ArrayList<AtfConsoleEvent>();
        if (projection == Projection.UI_API) {
            try (var session = UiExtentReportSession.openConfigured(report.getParent(),
                    report.getFileName().toString(), events::add)) {
                report = session.reportPath();
                var scenario = session.startScenario("BodyAuditMock", "audit.component", name, List.of("mock"));
                var phase = scenario.startPhase("TEST BODY");
                phase.apiTrace(THEN, "captured body evidence preserves its contract", "/body-audit",
                        observedExchanges(exchanges), AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
        } else {
            try (AtfReportPorts.Session session = projection == Projection.EXTENT
                    ? new ExtentReportSession(report, FULL_BODY, new SensitiveDataRedactor(), events::add)
                    : new HtmlReportSession(report, FULL_BODY, events::add)) {
                var scenario = session.startScenario("BodyAuditMock", name, List.of("mock"));
                var phase = scenario.startPhase("TEST BODY");
                int index = 0;
                for (var exchange : exchanges) {
                    try (var request = phase.startRequest("POST /body-audit — sample " + ++index, "POST", "/body-audit")) {
                        ApiExchangeEvidence.publish(request, exchange);
                        request.complete(AtfReportStatus.PASS);
                    }
                }
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
        }
        return new Published(Files.readString(report), List.copyOf(events));
    }

    static List<String> bodyText(String html, String direction) {
        String markup = html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "");
        return Pattern.compile("(?s)<div class=\"" + direction
                        + "-json-body\"[^>]*>.*?<(textarea|pre)\\b[^>]*>(.*?)</\\1>")
                .matcher(markup).results().map(match -> unescape(match.group(2))).toList();
    }

    private static String unescape(String text) {
        return text.replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
}

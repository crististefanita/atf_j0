package atf.reporting.api.evidence;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiBodyMediaComponentTest {
    @TempDir Path directory;

    private enum BodyCase {
        JSON("{\"message\":\"<tag>\",\"password\":\"media-secret\"}", "application/json", "JSON"),
        JSON_WITH_HTML_MIME("{\"message\":\"<tag>\"}", "text/html", "JSON"),
        FORM("name=demo&password=media-secret&text=%3Ctag%3E", "application/x-www-form-urlencoded; charset=UTF-8", "FORM BODY"),
        TEXT("</textarea><tag>plain body</tag>", "text/plain", "BODY"),
        ABSENT("", "", ""),
        NOT_CAPTURED("capture unavailable", "", "");

        final String body;
        final String mime;
        final String suffix;
        BodyCase(String body, String mime, String suffix) {
            this.body = body; this.mime = mime; this.suffix = suffix;
        }
        ReportBodyAvailability availability() {
            return this == ABSENT ? ReportBodyAvailability.ABSENT
                    : this == NOT_CAPTURED ? ReportBodyAvailability.NOT_CAPTURED
                    : ReportBodyAvailability.AVAILABLE;
        }
    }

    private enum Projection { EXTENT, FALLBACK, UI_API }

    @ParameterizedTest
    @EnumSource(BodyCase.class)
    void preservesMediaCaptureAndEscapingInAllProjections(BodyCase sample) throws Exception {
        var request = new ReportRequest("POST", "/media", Map.of(),
                Map.of("Content-Type", sample.mime), "none", sample.body, null, sample.availability());
        var response = new ReportResponse(200, sample.body, Map.of("Content-Type", sample.mime),
                Duration.ofMillis(2), "https://api.example.test/media", sample.availability());
        var evidence = new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(),
                List.of(), request, ReportTransportProvenance.ACCEPTANCE_MOCK);
        for (Projection projection : Projection.values()) {
            String html = render(projection, evidence, false);
            assertFalse(html.contains("media-secret"), projection.name());
            String activeMarkup = html.replaceAll("(?is)<textarea\\b[^>]*>.*?</textarea>", "");
            assertFalse(activeMarkup.contains("<tag>"), "payload must not create active HTML elements");
            assertTrue(html.contains("Request details"));
            assertTrue(html.contains("Response details"));
            if (sample == BodyCase.NOT_CAPTURED || sample == BodyCase.ABSENT) {
                assertTrue(html.contains(sample == BodyCase.ABSENT ? "Absent" : "Not captured"));
                assertFalse(html.contains("REQUEST JSON"));
                assertFalse(html.contains("RESPONSE JSON"));
                assertTrue(bodyLabels(html, "request-json-body").isEmpty());
                assertTrue(bodyLabels(html, "response-json-body").isEmpty());
            } else {
                assertTrue(html.contains("REQUEST " + sample.suffix), projection.name());
                assertTrue(html.contains("RESPONSE " + sample.suffix), projection.name());
                assertEquals(List.of("REQUEST " + sample.suffix), bodyLabels(html, "request-json-body"),
                        "request body must stay in request disclosure");
                assertEquals(List.of("RESPONSE " + sample.suffix), bodyLabels(html, "response-json-body"));
                if (!sample.mime.isBlank()) assertTrue(html.contains(sample.mime.replace("&", "&amp;")));
                if (sample == BodyCase.ABSENT) assertTrue(html.contains("Absent"));
                if (sample.suffix.equals("JSON") && sample != BodyCase.ABSENT)
                    assertTrue(html.contains("\n  &quot;message&quot;") || html.contains("\n  \"message\""));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Projection.class, names = {"EXTENT", "FALLBACK"})
    void directionDoesNotDependOnTheDisplayLabel(Projection projection) throws Exception {
        String html = render(projection, null, true);
        assertEquals(List.of("Server note"), bodyLabels(html, "request-json-body"));
        assertEquals(List.of("Client note"), bodyLabels(html, "response-json-body"));
    }

    private static List<String> bodyLabels(String html, String channel) {
        return Pattern.compile("class=\"" + channel + "\"[^>]*>\\s*<div[^>]*>(.*?)</div>",
                        Pattern.DOTALL).matcher(html).results()
                .map(match -> match.group(1).replaceAll("<[^>]*>", "").trim()).toList();
    }

    private String render(Projection projection, ReportExecutionEvidence evidence, boolean customLabels)
            throws Exception {
        Path report = directory.resolve(projection + ".html");
        if (projection == Projection.UI_API) {
            try (var session = UiExtentReportSession.openConfigured(directory, "ui-media.html", AtfConsoleSink.noOp())) {
                report = session.reportPath();
                var scenario = session.startScenario("MediaContract", getClass().getPackageName(), "media", List.of());
                var phase = scenario.startPhase("TEST BODY");
                phase.apiTrace(THEN, "media exchange", "/media", observedExchanges(List.of(evidence)), AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
        } else {
            try (AtfReportPorts.Session session = projection == Projection.EXTENT
                    ? new ExtentReportSession(report) : new HtmlReportSession(report)) {
                var scenario = session.startScenario("MediaContract", "media", List.of());
                var phase = scenario.startPhase("TEST BODY");
                try (var node = phase.startRequest("POST /media", "POST", "/media")) {
                    if (customLabels) {
                        node.body(new ReportJsonBody("Server note", "request text",
                                ReportJsonBody.Direction.REQUEST, ReportJsonBody.Format.TEXT));
                        node.body(new ReportJsonBody("Client note", "response text",
                                ReportJsonBody.Direction.RESPONSE, ReportJsonBody.Format.TEXT));
                    } else ApiExchangeEvidence.publish(node, evidence);
                    node.complete(AtfReportStatus.PASS);
                }
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }
        }
        return Files.readString(report).replaceAll("(?is)<script\\b[^>]*>.*?</script>", "");
    }
}

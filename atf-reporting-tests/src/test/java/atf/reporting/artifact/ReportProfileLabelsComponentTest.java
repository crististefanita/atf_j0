package atf.reporting.artifact;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.testkit.AtfComponentTest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.reporting.artifact.ReportArtifactProfile.Renderer.FALLBACK;
import static atf.reporting.artifact.ReportArtifactProfile.Renderer.EXTENT;
import static atf.reporting.contract.AtfReportStatus.FAIL;
import static atf.reporting.contract.AtfReportStatus.PASS;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ReportProfileLabelsComponentTest {
    private static final String CANARY = "</span></script><script id='atf-profile-canary'>window.atfProfileCanary=1;</script>";
    private static final String OWNER = "Fixture & \"quoted\" " + CANARY;
    private static final String SCENARIO = "  Datele\r\nsunt\u00a0în\t release/<version>/reports/. & " + CANARY + "  ";
    private static final String VISIBLE_SCENARIO = "Datele sunt în release/<version>/reports/. & " + CANARY;
    private static final String ASSERTIONS = " Compare\n\t values <literal> ";
    private static final String COMPARISON = "  Value\r\n\t past\u00a0  preview & \"quotes\" ";
    private static final String OBSERVATION = " Observe \n   body <label> ";
    private static final String FIELD = "  capture\t  id\u00a0name ";
    private static final String BODY_LABEL = " Payload\r\n  label <name> ";
    private static final String REQUEST = "GET /fixture — Request\r\n label <query>";
    private static final String REQUEST_BODY = "Request\t JSON <body-label>";
    private static final String FULL_BODY = "\n  first\r\n\t" + "x".repeat(300) + "  & <tail> ";
    private static final String PREFIX = "y".repeat(260);

    @TempDir Path directory;

    @ParameterizedTest
    @EnumSource(ReportArtifactProfile.Renderer.class)
    void labelsAreLiteralAndComparableWhileTheBodyAndVerdictRetainTheirContracts(
            ReportArtifactProfile.Renderer renderer) throws Exception {
        Path report = publish(renderer);
        var profile = intended(renderer);
        var result = ReportArtifactInspector.inspect(report, profile);
        assertTrue(result.isCompliant(), () -> result.violations().toString());
        assertEquals(SCENARIO, profile.scenarios().getFirst().name());
        assertEquals(VISIBLE_SCENARIO, ReportArtifactProfile.visibleLabel(SCENARIO));

        var html = Jsoup.parse(Files.readString(report));
        assertNull(html.getElementById("atf-profile-canary"), "Markup in the title must not create a script element");
        assertTrue(html.select("script").stream().noneMatch(script -> script.data().contains("</script><script id='atf-profile-canary'")));
        Element heading = scenarioTitle(html, renderer);
        assertEquals(VISIBLE_SCENARIO, heading.text());
        assertTrue(heading.children().isEmpty());
        assertEquals(FULL_BODY, html.select("pre.atf-observation-body").getFirst().wholeText());
        assertEquals("Request JSON <body-label>", html.select(".helm-scroll-block-title").getFirst().text());

        Element comparison = html.select(renderer == FALLBACK ? "table.assertions tbody tr" : "table.helm-validation-table tbody tr").getFirst();
        assertEquals("Value past preview & \"quotes\"", comparison.child(0).text());
        for (int column : List.of(1, 2)) {
            assertEquals("y".repeat(239) + "…", comparison.child(column).text());
        }
        assertEquals(renderer == FALLBACK ? FAIL.displayLabel() : FAIL.marker(), comparison.child(3).text());

        heading.html("<b></b>").child(0).text(VISIBLE_SCENARIO);
        Path mutant = directory.resolve(renderer + "-marked-title.html");
        Files.writeString(mutant, html.outerHtml());
        var rejected = ReportArtifactInspector.inspect(mutant, profile);
        assertTrue(rejected.violations().contains("scenario[0] identity"), rejected.violations()::toString);
    }

    private static Element scenarioTitle(Element html, ReportArtifactProfile.Renderer renderer) {
        return html.select(renderer == FALLBACK ? "#tests .scenario-title h2" :
                "#test-view .test-contents .mt-4 > .accordion > .card > .card-header a.node > span").getFirst();
    }

    private Path publish(ReportArtifactProfile.Renderer renderer) {
        Path file = directory.resolve(renderer + ".html");
        var settings = new ReportingSettings(ReportingSettings.defaults().assertionMaxDisplayCharacters(),
                ReportingSettings.DISABLE_BODY_DISPLAY_LIMIT);
        try (AtfReportPorts.Session session = renderer == FALLBACK ? new HtmlReportSession(file, settings)
                : new ExtentReportSession(file, settings);
             var scenario = session.startScenario(OWNER, SCENARIO, List.of("component"));
             var phase = scenario.startPhase("TEST BODY")) {
            if (renderer == EXTENT) assertEquals(SCENARIO, scenario.id());
            phase.observation(new ReportObservation(OBSERVATION, List.of(new ReportField(FIELD, "capture-1")),
                    List.of(new ReportObservation.Body(BODY_LABEL, FULL_BODY, ReportObservation.Format.TEXT)), List.of()));
            try (var request = phase.startRequest(REQUEST, "GET", "/fixture")) {
                request.json(REQUEST_BODY, "{\"value\":1}");
                request.complete(PASS);
            }
            try (var step = phase.startAssertionStep(ASSERTIONS)) {
                step.assertions(List.of(new AtfAssertion(COMPARISON, PREFIX + "expected", PREFIX + "actual", false)));
                step.complete(FAIL);
            }
            phase.complete(FAIL);
            scenario.complete(FAIL);
        }
        return file;
    }

    private static ReportArtifactProfile intended(ReportArtifactProfile.Renderer renderer) {
        var observation = new ReportArtifactProfile.Observation(OBSERVATION, List.of(FIELD), List.of(BODY_LABEL));
        var request = new ReportArtifactProfile.HttpExchange(REQUEST, true, false, List.of(REQUEST_BODY), List.of(), PASS);
        var assertions = new ReportArtifactProfile.Assertions(ASSERTIONS,
                List.of(new ReportArtifactProfile.Comparison(COMPARISON, false)));
        return new ReportArtifactProfile(renderer, List.of(new ReportArtifactProfile.Scenario(OWNER, SCENARIO, FAIL,
                List.of(new ReportArtifactProfile.Phase("TEST BODY", FAIL, List.of(observation, request, assertions))))));
    }
}

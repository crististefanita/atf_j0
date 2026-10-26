package atf.reporting.artifact;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.core.http.response.ApiOperationLabel;
import atf.mock.MockRoutePaths;
import atf.reporting.extent.session.ExtentReportSession;


import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportArtifactInspectorTest {
    @Test
    void inspectsThePublishedExtentArtifactInsteadOfTheBuilder() throws Exception {
        Path report = Files.createTempDirectory("atf-inspector-").resolve("report.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", "POST user — request/response contract", List.of("api", "contract"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest(ApiOperationLabel.format(HttpMethod.POST, MockRoutePaths.USERS, null),
                    HttpMethod.POST.value(), MockRoutePaths.USERS);
            request.detail("RESPONSE DURATION (ms)", "12");
            request.detail("route", MockRoutePaths.USER_BY_ID);
            request.json("REQUEST JSON", "{\"id\":1,\"name\":\"ATF\"}");
            request.json("RESPONSE JSON", "{\"id\":1}");
            var validation = request.startValidation("Validate: user contract");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "200", "200", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        var html = Files.readString(report);
        assertTrue(html.contains("REQUEST JSON"));
        assertTrue(html.contains("\"name\" : \"ATF\"")
                || html.contains("&quot;name&quot; : &quot;ATF&quot;")
                || html.contains("\"name\":\"ATF\"")
                || html.contains("&quot;name&quot;:&quot;ATF&quot;"));
        assertTrue(html.contains("\"id\" : 1") || html.contains("&quot;id&quot; : 1")
                || html.contains("\"id\":1"));
        assertEquals(1, count(html, "function renderFails("),
                "the report runtime must publish one authoritative Fails renderer");
        assertEquals(1, count(html, "function failureContext(item)"),
                "the report runtime must publish one authoritative failure-context owner");
        assertTrue(!html.contains("failureContext=function"),
                "failure context enrichment must use helpers instead of rebinding a global function");

        var inspection = ReportArtifactInspector.inspectFresh(report, Instant.now().minusSeconds(30));
        assertTrue(inspection.isCompliant(), () -> "Report violations: " + inspection.violations());
        assertTrue(inspection.satisfies("visual color contract"));
        assertTrue(inspection.satisfies("uniform report surfaces"));
        assertTrue(inspection.satisfies("tests filter surface"));
        assertTrue(inspection.satisfies("tests filter controls"));
        assertTrue(inspection.satisfies("tests filter behavior contract"));
        assertTrue(inspection.satisfies("tests filter owner contract"));
        assertTrue(inspection.satisfies("tests filter state accessibility"));
        assertTrue(inspection.satisfies("tests filter vocabulary contract"));
        assertTrue(inspection.satisfies("tab organization contract"));
        assertTrue(inspection.satisfies("tab order contract"));
        assertTrue(inspection.satisfies("filter preserves active tab"));
        assertTrue(inspection.satisfies("scenario filtering scope"));
        assertTrue(inspection.satisfies("phase shell contract"));
        assertTrue(inspection.satisfies("phase semantic accents"));
        assertTrue(inspection.satisfies("tests navigator contract"));
        assertTrue(inspection.satisfies("compact technical trace"));
        assertTrue(inspection.satisfies("evidence visual contract"));
        assertTrue(inspection.satisfies("overview metadata"));
        assertTrue(inspection.satisfies("request-validation roles"));
        assertTrue(inspection.satisfies("runtime navigation"));
        assertTrue(inspection.satisfies("hash navigation"));
        assertTrue(inspection.satisfies("runtime bootstrap placement"));
        assertTrue(inspection.satisfies("offline tab fallback"));
        assertTrue(inspection.satisfies("overview order"));
        assertTrue(inspection.satisfies("reference overview table structure"));
        assertTrue(inspection.satisfies("scenario identity projection"));
        assertTrue(inspection.satisfies("overview shared table contract"));
        assertTrue(inspection.satisfies("category status palette"));
        assertTrue(inspection.satisfies("category surface and link contract"));
        assertTrue(inspection.satisfies("overview summary typography contract"));
        assertTrue(inspection.satisfies("overview excludes log events chart"));
        assertTrue(inspection.satisfies("failure evidence projection"));
        assertTrue(inspection.satisfies("category selection projection"));
        assertTrue(inspection.satisfies("category deep links"));
        assertTrue(inspection.satisfies("evidence cardinality contract"));
        assertTrue(inspection.satisfies("fresh artifact"));
        assertTrue(inspection.diagnostics().stream().allMatch(diagnostic -> !diagnostic.owner().isBlank()));
        assertTrue(inspection.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.owner().equals("evidence") && diagnostic.check().equals("technical trace")
                        && diagnostic.passed()));
    }

    private static int count(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}

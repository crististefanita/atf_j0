package atf.reporting.html;
import atf.testkit.AtfUnitTest;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.core.http.response.ApiOperationLabel;
import atf.mock.MockRoutePaths;
import atf.reporting.html.contract.HtmlReportStaticContract;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Component contract: semantic input → fallback HTML → static artifact audit.
 * No Playwright or external server is needed to validate the reporting module.
 */
@AtfUnitTest
class HtmlReportComponentContractTest {
    @Test
    void auditsThePublishedHtmlThroughRendererIndependentArtifactChecks() throws Exception {
        Path report = Files.createTempDirectory("atf-html-component-").resolve("report.html");
        String userPath = MockRoutePaths.userById("1");
        String requestTitle = ApiOperationLabel.format(HttpMethod.GET, userPath,
                "read user profile");
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("ComponentReportTests",
                    "API evidence component contract", List.of("api", "component"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest(
                    requestTitle, HttpMethod.GET.value(), userPath);
            request.detail("HTTP STATUS", "200");
            request.json("REQUEST JSON", "null");
            request.json("RESPONSE JSON", "{\"id\":1,\"name\":\"ATF User\"}");
            var validation = request.startValidation("Validate response contract");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "200", "200", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        var evaluation = HtmlReportStaticContract.evaluate(Files.readString(report));

        assertTrue(Files.isRegularFile(report));
        assertTrue(Files.readString(report).contains(requestTitle));
        assertTrue(evaluation.isCompliant(), () -> "Component violations: " + evaluation.violations());
        assertTrue(evaluation.satisfies("request validation roles"));
        assertTrue(evaluation.satisfies("json evidence channels"));
        assertTrue(evaluation.satisfies("runtime asset"));
    }
}

package atf.reporting.api.contract;
import atf.testkit.AtfIntegrationTest;

import atf.core.http.request.ApiQueryParameters;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.core.http.response.ApiOperationLabel;
import atf.mock.MockRoutePaths;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component-level proof that one published API artifact can be audited alone. */
@AtfIntegrationTest
class ApiReportArtifactAuditTest {
    private static final String PATH = MockRoutePaths.USERS;
    private static final String OPERATION = ApiOperationLabel.format(HttpMethod.POST, PATH, null);
    private static final String REQUEST_BODY = "{\"name\":\"ATF\"}";
    private static final String RESPONSE_BODY = "{\"id\":1,\"name\":\"ATF\"}";

    @Test
    void combinesStaticHtmlAndCatalogBackedJsonChecks() throws Exception {
        Path report = Files.createTempDirectory("atf-api-artifact-audit-").resolve("report.html");
        writeReport(report);

        ApiReportRouteContract route = new ApiReportRouteContract(
                HttpMethod.POST.value(), PATH, 201, RESPONSE_BODY,
                Map.of(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON),
                ApiReportRouteContract.ANY_AUTH_PROFILE, ApiQueryParameters.empty(), Map.of(),
                ApiReportRequestBodyMode.JSON.value(), REQUEST_BODY);

        ApiReportArtifactAudit.Result result = new ApiReportArtifactAudit(() -> List.of(route))
                .inspect(report);

        assertTrue(result.compliant(), () -> result.violations().toString());
        assertTrue(result.artifact().satisfies("request-validation roles"));
        assertTrue(result.jsonEvidence().renderedJsonDocuments() >= 2);
    }

    @Test
    void reportsMissingArtifactThroughTheSameActionableResult() throws Exception {
        Path report = Files.createTempDirectory("atf-api-artifact-missing-").resolve("missing.html");
        ApiReportArtifactAudit.Result result = new ApiReportArtifactAudit(List::of).inspect(report);

        assertFalse(result.compliant());
        assertTrue(result.violations().stream().anyMatch(violation ->
                violation.contains("API report artifact does not exist")));
    }

    private static void writeReport(Path report) throws Exception {
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", "POST user — JSON contract",
                    List.of("api", "contract"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest(OPERATION, HttpMethod.POST.value(), PATH);
            request.detail("RESPONSE DURATION (ms)", "12");
            request.json("REQUEST JSON", REQUEST_BODY);
            request.json("RESPONSE JSON", RESPONSE_BODY);
            var validation = request.startValidation("Validate: user contract");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "201", "201", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }
}

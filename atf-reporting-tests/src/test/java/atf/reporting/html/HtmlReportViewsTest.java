package atf.reporting.html;
import atf.testkit.AtfUnitTest;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiAttemptRecord;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportField;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.ui.network.ReportNetworkInspector;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class HtmlReportViewsTest {
    @Test
    void fallbackViewsShareTypedSectionIdsAndTabPanelShell() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("ShellTests", "fallback shell", List.of("api"));
        scenario.complete(AtfReportStatus.PASS);
        var snapshot = document.snapshot();

        String category = new HtmlReportCategoryView().render(snapshot);
        String fails = new HtmlReportFailsView().render(snapshot);
        String overview = new HtmlReportOverviewView().render(snapshot);

        assertAll(
                () -> assertTrue(category.contains("<section id=\"category\" class=\"tab-panel\">")),
                () -> assertTrue(category.contains("<h2>Category</h2>")),
                () -> assertTrue(fails.contains("<section id=\"fails\" class=\"tab-panel\">")),
                () -> assertTrue(fails.contains("<h2>Fails</h2>")),
                () -> assertTrue(overview.contains("<section id=\"overview\" class=\"tab-panel\">")),
                () -> assertTrue(overview.contains("<h2>Overview</h2>")));
    }

    @Test
    void projectionsReadTheSameSemanticDocumentWithoutRebuildingVerdicts() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("UsersTests", "user contract", List.of("api", "contract"));
        var phase = new HtmlReportSemanticDocument.PhaseModel("TEST BODY");
        var request = new HtmlReportSemanticDocument.RequestModel("item", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        request.status = AtfReportStatus.FAIL;
        var validation = new HtmlReportSemanticDocument.ValidationModel("Validate user");
        validation.assertions = List.of(new AtfAssertion("HTTP status", "201 Created", "200 OK", false));
        validation.status = AtfReportStatus.FAIL;
        phase.children.add(request);
        phase.children.add(validation);
        phase.status = AtfReportStatus.FAIL;
        scenario.phases.add(phase);
        scenario.status = AtfReportStatus.FAIL;

        var scenarios = document.snapshot();
        var tests = new HtmlReportTestsView().render(scenarios);
        var category = new HtmlReportCategoryView().render(scenarios);
        var fails = new HtmlReportFailsView().render(scenarios);
        var overview = new HtmlReportOverviewView().render(scenarios);

        assertAll(
                () -> assertTrue(tests.indexOf("class=\"request\"") < tests.indexOf("class=\"validation\"")),
                () -> assertTrue(tests.contains("status-fail\">Fail</span>")),
                () -> assertTrue(category.contains("class=\"tag\">api</span>")),
                () -> assertTrue(fails.contains("HTTP status") && fails.contains("200 OK")),
                () -> assertTrue(overview.contains("<strong>1</strong>")));
    }

    @Test
    void fallbackUsesTheSharedPrettyJsonProjectionForRequestAndResponseBodies() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("JsonTests", "pretty JSON evidence", List.of("api"));
        var phase = scenario.addPhase("TEST BODY");
        var request = phase.addRequest("create", "POST", "/users");
        request.addJson("REQUEST JSON", "{\"user\":{\"name\":\"Ada\",\"roles\":[\"admin\"]}}");
        request.addJson("RESPONSE JSON", "{\"created\":true,\"meta\":{\"attempt\":1}}");
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        String html = new HtmlReportTestsView().render(document.snapshot());

        assertAll(
                () -> assertTrue(html.contains("&quot;user&quot; : {")),
                () -> assertTrue(html.contains("\n    &quot;name&quot; : &quot;Ada&quot;")),
                () -> assertTrue(html.contains("&quot;roles&quot; : [\n")),
                () -> assertTrue(html.contains("&quot;meta&quot; : {")),
                () -> assertTrue(html.contains("\n    &quot;attempt&quot; : 1")));
    }

    @Test
    void fallbackUsesTheCanonicalEvidenceTableMarkupForRequestAndResponseDetails() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("EvidenceTests", "shared evidence table", List.of("api"));
        var request = scenario.addPhase("TEST BODY")
                .addRequest("create", "POST", "/users");
        request.addDetail("URL", "/users");
        request.addDetail("Content-Type", "application/json");
        request.complete(AtfReportStatus.PASS);

        String html = new HtmlReportTestsView().render(document.snapshot());

        assertAll(
                () -> assertTrue(html.contains("class=\"atf-evidence-key-column\"")),
                () -> assertTrue(html.contains("class=\"atf-evidence-value-column\"")),
                () -> assertTrue(html.contains("class=\"helm-api-classification technical\"")),
                () -> assertTrue(html.contains("<th>FIELD</th><th>VALUE</th>")),
                () -> assertTrue(html.contains("<td>URL</td><td class=\"atf-evidence-url-value\">/users</td>")));
    }

    @Test
    void fallbackOmitsAnEmptyNetworkAggregateLikeExtent() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("NetworkTests", "nested aggregate", List.of("ui", "api"));
        var phase = scenario.addPhase("TEST BODY");
        var request = phase.addRequest("API network inspector", "TRACE", "network");
        request.networkAggregate(ReportNetworkInspector.aggregate(List.of(), List.of()));
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        String html = new HtmlReportTestsView().render(document.snapshot());

        assertAll(
                () -> assertFalse(html.contains("Network aggregate")),
                () -> assertFalse(html.contains("network-aggregate-substep")),
                () -> assertFalse(html.contains("network-aggregate-sections")),
                () -> assertEquals(0, html.split("network-aggregate-section-row", -1).length - 1),
                () -> assertFalse(html.contains("<details open")));
    }

    @Test
    void executionEvidenceUsesTheSameRedactionBoundaryAsTheOtherRequestEvidence() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("RetryTests", "retry evidence", List.of("api"));
        var phase = new HtmlReportSemanticDocument.PhaseModel("TEST BODY");
        var request = new HtmlReportSemanticDocument.RequestModel("retry", "GET", ApiReportTestUrls.GENERIC_USERS);
        request.addDetail("request.header.Accept", "application/json");
        request.execution(new ApiExecutionResult(
                new ApiResponse(200, "ignored", Map.of(), Duration.ofMillis(301),
                        ApiReportTestUrls.example(ApiReportTestUrls.GENERIC_USERS
                                + "?token=raw-url-token&mode=full")),
                2,
                Duration.ofMillis(18),
                List.of(),
                List.of(
                        new ApiAttemptRecord(1, 429, Duration.ofMillis(5),
                                ApiAttemptOutcome.RETRYABLE_RESPONSE, "retry token=raw-retry-token"),
                        new ApiAttemptRecord(2, 200, Duration.ofMillis(12),
                                ApiAttemptOutcome.SUCCESS, "completed"))));
        phase.children.add(request);
        scenario.phases.add(phase);

        String html = new HtmlReportTestsView().render(document.snapshot());

        assertAll(
                () -> assertTrue(html.contains("execution-evidence")),
                () -> assertTrue(html.contains("request-evidence")),
                () -> assertTrue(html.contains("retry-history-table")),
                () -> assertEquals(2, html.split("class=\"atf-api-table-wrap\"", -1).length - 1,
                        "fallback request metadata and retry history use the same bounded table wrapper"),
                () -> assertTrue(html.contains("RESPONSE DURATION")),
                () -> assertTrue(html.contains("<th>RESPONSE DURATION</th><td>0.301 s</td>")),
                () -> assertTrue(html.contains("<td>1</td><td>429</td><td>5 ms</td><td>RETRYABLE_RESPONSE</td>")),
                () -> assertTrue(html.contains("<td>2</td><td>200</td><td>12 ms</td><td>SUCCESS</td>")),
                () -> assertTrue(html.contains("***REDACTED***")),
                () -> assertTrue(html.contains("mode=full")),
                () -> assertFalse(html.contains("raw-url-token")),
                () -> assertFalse(html.contains("raw-retry-token")));
    }

    @Test
    void fallbackPublishesPhaseFailureCauseWithTheSameRedactionBoundary() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("FailureTests", "redacted failure", List.of("api"));
        var phase = new HtmlReportSemanticDocument.PhaseModel("TEST BODY");
        phase.status = AtfReportStatus.FAIL;
        phase.details.add(new ReportField("Failure cause", "ApiValidationException: token=raw-failure-token; mode=full"));
        scenario.phases.add(phase);
        scenario.status = AtfReportStatus.FAIL;

        String html = new HtmlReportTestsView().render(document.snapshot());

        assertAll(
                () -> assertFalse(html.contains("<details class=\"step-detail phase-details\"")),
                () -> assertTrue(html.contains("<details class=\"step-detail phase-diagnostics\"")),
                () -> assertTrue(html.indexOf("phase-diagnostics") < html.indexOf("data-atf-node-role=\"phase\""),
                        "execution diagnostics belong to the scenario, outside the API phase"),
                () -> assertTrue(html.contains("Failure cause")),
                () -> assertTrue(html.contains("token=***REDACTED***")),
                () -> assertTrue(html.contains("mode=full")),
                () -> assertFalse(html.contains("raw-failure-token")));
    }
}

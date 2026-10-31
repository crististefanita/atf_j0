package atf.reporting.extent.session;

import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.support.TestReportPaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract tests for the Fails projection and its cause ownership chain. */
@AtfUnitTest
class ExtentReportFailureProjectionContractTest {
    private static final String USERS = MockRoutePaths.USERS;
    private static final String USER = MockRoutePaths.userById("1");
    private static final String USERS_OPERATION = ApiReportTestUrls.operation(HttpMethod.GET, USERS, null);
    private static final String USER_OPERATION = ApiReportTestUrls.operation(HttpMethod.GET, USER, null);

    @Test
    void lifecyclePhasesRemainOrderedWhenSomePhasesAreSkippedOrFailed() throws Exception {
        Path report = TestReportPaths.report("extent-interaction-lifecycle-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ApiLifecycleAndCombinationsTests",
                    "Users API — pass → teardown fail perspective", List.of("api", "lifecycle", "failure"));
            var body = scenario.startPhase("TEST BODY");
            body.startRequest(USERS_OPERATION, HttpMethod.GET.value(), USERS)
                    .complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            var teardown = scenario.startPhase("TEARDOWN");
            teardown.detail("FAILURE CAUSE", "teardown failure perspective");
            teardown.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.indexOf("TEST BODY") < html.indexOf("TEARDOWN")),
                () -> assertTrue(html.contains("FAILURE CAUSE")),
                () -> assertTrue(html.contains("teardown failure perspective")),
                () -> assertTrue(html.contains("status=\"fail\"")),
                () -> assertTrue(html.contains("function failureContext(item)")),
                () -> assertTrue(html.contains("data-atf-failure-phase")),
                () -> assertTrue(html.contains("class=\"atf-failure-open\"")
                                && html.contains("data-atf-failure-ordinal")
                                && html.contains("data-atf-failure-kind")
                                && html.contains("failureLink(") && html.contains("test-id"),
                        "Fails must retain typed source identity for delegated navigation and its native hash fallback")
        );
    }

    @Test
    void failsProjectionKeepsRequestOwnershipAndFailureCauseChain() throws Exception {
        Path report = TestReportPaths.report("extent-fails-request-ownership-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ApiLifecycleAndCombinationsTests",
                    "Users API — setup → test → teardown", List.of("api", "users", "lifecycle"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest(USER_OPERATION, HttpMethod.GET.value(), USER);
            request.detail("RESPONSE DURATION (ms)", "12 ms");
            request.detail("URL", ApiReportTestUrls.example(USER));
            request.json("RESPONSE JSON", "{\"id\":1}");
            var validation = request.startValidation("Contract validation");
            validation.assertions(List.of(
                    new AtfAssertion("HTTP status", "201", "200", false),
                    new AtfAssertion("Content-Type", "application/json", "application/json", true)));
            validation.complete(AtfReportStatus.FAIL);
            request.complete(AtfReportStatus.FAIL);
            body.complete(AtfReportStatus.FAIL);

            var teardown = scenario.startPhase("TEARDOWN");
            teardown.detail("FAILURE CAUSE", "teardown cleanup failed: Authorization: Bearer ***REDACTED***");
            teardown.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("function scopeFailureContextToPhase(item,context)"),
                        "Fails must resolve the request from the failing phase, not the first request in the scenario"),
                () -> assertTrue(html.contains("context.request=text(request,'.card-title a.node')"),
                        "the request shown in Fails must belong to the failed phase"),
                () -> assertTrue(html.contains("failureTransportEvidence(request,measured)")
                                && html.contains("context.duration=measured.duration||context.duration"),
                        "the duration shown in Fails must belong to the failed request"),
                () -> assertTrue(html.contains("var durationMs=atfDurationValue(rawDuration)")
                                && html.contains("duration=durationMs===null?rawDuration:atfFormatDuration(durationMs)"),
                        "execution evidence duration must use the shared duration parser and formatter"),
                () -> assertTrue(html.contains("Scenario diagnostics: ") && html.contains("PRIMARY:"),
                        "an API assertion must retain the scenario diagnostics alongside its own cause"),
                () -> assertTrue(html.contains("PRIMARY: ") && html.contains("SECONDARY: "),
                        "the Fails projection must preserve the ordered cause chain")
        );
    }
}

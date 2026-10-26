package atf.api.reporting.extent.session;

import atf.api.testkit.AtfUnitTest;
import atf.api.core.http.response.ApiExecutionResult;
import atf.api.core.http.response.ApiResponse;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfReportStatus;
import atf.api.core.http.request.HttpMethod;
import atf.api.mock.MockRoutePaths;
import atf.api.reporting.support.ApiReportTestUrls;
import atf.api.reporting.support.TestReportPaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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
                                && html.contains("atfOpenTest(id,scenarioIndex)")
                                 && (html.contains("onclick=\"if(typeof atfOpenTest===")
                                 || html.contains("onclick=\\\"if(typeof atfOpenTest==="))
                                && html.contains("href=\"#test-id="),
                        "Fails must expose an executable deep-link and a native hash fallback")
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
                () -> assertTrue(html.contains("if(!ctx.message||ctx.message.indexOf('PRIMARY:')<0)"),
                        "an assertion row must not overwrite the primary/secondary cause chain"),
                () -> assertTrue(html.contains("PRIMARY: ") && html.contains("SECONDARY: "),
                        "the Fails projection must preserve the ordered cause chain")
        );
    }
}

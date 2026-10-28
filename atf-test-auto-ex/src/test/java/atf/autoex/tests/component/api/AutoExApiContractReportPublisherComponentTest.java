package atf.autoex.tests.component.api;

import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransportProvenance;
import atf.core.http.request.HttpHeader;
import atf.autoex.api.AutoExApiRoute;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionModel;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.api.AutoExApiRequestFactory;
import atf.autoex.tests.api.AutoExApiContractReportExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static atf.core.http.request.HttpMethod.POST;
import static atf.core.http.request.MediaTypes.APPLICATION_FORM_URLENCODED;
import static atf.autoex.tests.support.AutoExApiFixture.BASE_URI;
import static atf.reporting.ui.contract.ReportUiContract.Phase.TEST_BODY;
import static atf.autoex.api.AutoExApiOutcome.USER_EXISTS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects AutoEx responseCode/message mapping and the consumer's published-data boundary. */
@AutoExComponentTest
class AutoExApiContractReportPublisherComponentTest {
    private static final String REPORT_DIRECTORY = "custom/api-reports";
    private static final String EXPECTED_MESSAGE = USER_EXISTS.message();
    private static final String SECRET = "synthetic-report-password";
    private static final String RESPONSE_BODY = "{\"responseCode\":200,\"message\":\"User exists!\"}";

    @Test
    void publisherForwardsTheAutoExContractAndDoesNotExposeFormCredentials(@TempDir Path directory)
            throws Exception {
        Path report = directory.resolve("api-contract.html");
        var execution = execution(200, RESPONSE_BODY);
        try (var session = new HtmlReportSession(report)) {
            publish(session, execution);
            var request = publishedRequest(session);
            var validation = (ReportExecutionModel.Validation) request.children().getFirst();
            assertAll(
                    () -> assertEquals("API 7 valid login", request.title()),
                    () -> assertEquals(POST.value(), request.method()),
                    () -> assertEquals(AutoExApiRoute.VERIFY_LOGIN.path(), request.path()),
                    () -> assertEquals(APPLICATION_FORM_URLENCODED,
                            request.execution().request().headers().get(HttpHeader.CONTENT_TYPE.value())),
                    () -> assertTrue(request.execution().response().headers().values()
                            .contains("text/html; charset=utf-8")),
                    () -> assertEquals("Validate documented JSON response contract", validation.title()),
                    () -> assertEquals(List.of("JSON responseCode", "JSON message", "HTTP transport status"),
                            validation.assertions().stream().map(AtfAssertion::name).toList()),
                    () -> assertEquals("200", validation.assertions().getFirst().actual()),
                    () -> assertEquals(EXPECTED_MESSAGE, validation.assertions().get(1).actual()),
                    () -> assertEquals(AtfReportStatus.PASS, validation.status()));
        }
        Path extentReport = directory.resolve("extent-contract.html");
        try (var session = new ExtentReportSession(extentReport)) {
            publish(session, execution);
        }
        for (Path published : List.of(report, extentReport)) {
            assertTrue(Files.readString(published).contains("***REDACTED***"));
            assertFalse(Files.readString(published).contains(SECRET),
                    "the AutoEx request factory and publisher must not expose the form password");
        }
    }

    @ParameterizedTest
    @MethodSource("invalidContracts")
    void transportSuccessCannotHideAnInvalidAutoExContractOrAdditionalAssertion(
            int httpStatus, String responseBody, @TempDir Path directory) {
        try (var session = new HtmlReportSession(directory.resolve("invalid-contract.html"))) {
            publish(session, execution(httpStatus, responseBody));
            var request = publishedRequest(session);
            var validation = (ReportExecutionModel.Validation) request.children().getFirst();
            assertEquals(AtfReportStatus.FAIL, request.status());
            assertEquals(AtfReportStatus.FAIL, validation.status());
            assertTrue(validation.assertions().stream().anyMatch(assertion -> !assertion.passed()));
        }
    }

    static Stream<Arguments> invalidContracts() {
        return Stream.of(
                Arguments.of(200, "{\"responseCode\":401,\"message\":\"User not found!\"}"),
                Arguments.of(200, "{\"responseCode\":200,\"message\":\"Unexpected message\"}"),
                Arguments.of(200, "<html>upstream error</html>"),
                Arguments.of(503, RESPONSE_BODY));
    }

    @Test
    void reportDirectoryUsesTheAutoExDefaultOrConfiguredProjectRelativeOverride(@TempDir Path projectRoot) {
        assertAll(
                () -> assertEquals(projectRoot.resolve("artifacts/reports/api/auto-ex"),
                        AutoExApiContractReportExtension.resolveDirectory(projectRoot, null)),
                () -> assertEquals(projectRoot.resolve(REPORT_DIRECTORY),
                        AutoExApiContractReportExtension.resolveDirectory(projectRoot, REPORT_DIRECTORY)),
                () -> assertTrue(AutoExApiContractReportExtension.resolveDirectory(
                        projectRoot, REPORT_DIRECTORY).startsWith(projectRoot)));
    }

    @Test
    void executionFailureForwardsTheActualCauseChainAndSuppressionsWithoutLeakingSecrets(
            @TempDir Path directory) throws Exception {
        Path report = directory.resolve("api-contract-failure.html");
        String secret = "synthetic-api-token-should-not-leak";
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("AutoExApiReadOnlyLiveContractTest",
                    "read-only API evidence", List.of("api", "auto-ex", "component"));
            var phase = scenario.startPhase(TEST_BODY.displayName());
            var failure = new IllegalStateException("request failed", new java.io.IOException(
                    "authorization: Bearer " + secret));
            failure.addSuppressed(new IllegalArgumentException("cleanup failure"));
            AutoExApiContractReportExtension.recordExecutionFailure(phase, failure);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            var fields = session.executionModel().scenarios().getFirst().phases().getFirst().details();
            assertEquals(1, fields.size());
            assertEquals("JUnit failure", fields.getFirst().name());
            String message = fields.getFirst().value();
            assertAll(
                    () -> assertTrue(message.contains("IllegalStateException: request failed")),
                    () -> assertTrue(message.contains("IOException: authorization: Bearer")),
                    () -> assertTrue(message.contains("IllegalArgumentException: cleanup failure")));
        }
        assertFalse(Files.readString(report).contains(secret), "the published diagnostic must redact credentials");
    }

    @Test
    void reportAdapterClosesBothHandlesAndPreservesTheFirstFailure() {
        var calls = new java.util.ArrayList<String>();
        var closeFailure = new AssertionError("phase close failed");
        var scenarioFailure = new IllegalStateException("scenario close failed");
        var phase = (AtfReportPorts.Phase) java.lang.reflect.Proxy.newProxyInstance(
                AtfReportPorts.Phase.class.getClassLoader(), new Class<?>[]{AtfReportPorts.Phase.class},
                (proxy, method, args) -> {
                    calls.add("phase." + method.getName());
                    if (method.getName().equals("close")) throw closeFailure;
                    return null;
                });
        var scenario = (AtfReportPorts.Scenario) java.lang.reflect.Proxy.newProxyInstance(
                AtfReportPorts.Scenario.class.getClassLoader(), new Class<?>[]{AtfReportPorts.Scenario.class},
                (proxy, method, args) -> {
                    calls.add("scenario." + method.getName()
                            + (method.getName().equals("complete") ? "." + args[0] : ""));
                    if (method.getName().equals("close")) throw scenarioFailure;
                    return null;
                });
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class,
                () -> AutoExApiContractReportExtension.finishRun(scenario, phase, null));
        org.junit.jupiter.api.Assertions.assertSame(closeFailure, thrown);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Throwable[]{scenarioFailure}, thrown.getSuppressed());
        assertEquals(List.of("phase.complete", "phase.close", "scenario.complete.FAIL", "scenario.close"), calls);

        calls.clear();
        var primary = new IllegalArgumentException("actual business failure");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> AutoExApiContractReportExtension.finishRun(scenario, phase, primary));
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new Throwable[]{closeFailure, scenarioFailure}, primary.getSuppressed());
        assertTrue(calls.contains("phase.diagnostic"));
        assertTrue(calls.contains("scenario.close"));
    }

    private static ApiExecutionResult execution(int status, String body) {
        var request = new AutoExApiRequestFactory().verifyLogin("report@example.test", SECRET);
        var response = new ApiResponse(status, body,
                Map.of("content-type", "text/html; charset=utf-8"), Duration.ofMillis(4),
                BASE_URI.resolve(AutoExApiRoute.VERIFY_LOGIN.path()).toString());
        return new ApiExecutionResult(response, 1, Duration.ofMillis(4), List.of(), List.of(),
                request, ApiTransportProvenance.ACCEPTANCE_MOCK);
    }

    @Test
    void diagnosticsBoundFailureGraphsAndLongMessagesWithoutLooping(@TempDir Path directory) {
        var failure = new IllegalStateException("root");
        var cause = new IllegalArgumentException("cause");
        failure.initCause(cause);
        cause.initCause(failure);
        failure.addSuppressed(new UnsupportedOperationException("suppressed-one"));
        failure.addSuppressed(new ArithmeticException("suppressed-two"));
        failure.addSuppressed(new IndexOutOfBoundsException("outside-bound"));

        String boundedGraph = diagnostic(directory.resolve("graph.html"), failure);
        assertAll(
                () -> assertTrue(boundedGraph.contains("IllegalStateException: root")),
                () -> assertTrue(boundedGraph.contains("IllegalArgumentException: cause")),
                () -> assertTrue(boundedGraph.contains("UnsupportedOperationException: suppressed-one")),
                () -> assertTrue(boundedGraph.contains("ArithmeticException: suppressed-two")),
                () -> assertFalse(boundedGraph.contains("outside-bound")));
        String boundedMessage = diagnostic(directory.resolve("long-message.html"),
                new IllegalStateException("detail ".repeat(400)));
        assertEquals(1_000, boundedMessage.length());
    }

    private static String diagnostic(Path report, Throwable failure) {
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("AutoExDiagnostics", "bounded failure graph", List.of("component"));
            var phase = scenario.startPhase(TEST_BODY.displayName());
            AutoExApiContractReportExtension.recordExecutionFailure(phase, failure);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            return session.executionModel().scenarios().getFirst().phases().getFirst().details().getFirst().value();
        }
    }

    private static void publish(AtfReportPorts.Session session, ApiExecutionResult execution) {
        var scenario = session.startScenario("AutoExApiLiveContractTest", "verify login report contract",
                List.of("api", "auto-ex", "component"));
        var phase = scenario.startPhase(TEST_BODY.displayName());
        int status = execution.response().status();
        AutoExApiContractReportExtension.publish(phase, "API 7 valid login", execution, 200,
                EXPECTED_MESSAGE, List.of(new AtfAssertion("HTTP transport status", "200",
                        Integer.toString(status), status == 200)));
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);
    }

    private static ReportExecutionModel.Request publishedRequest(HtmlReportSession session) {
        return (ReportExecutionModel.Request) session.executionModel().scenarios().getFirst()
                .phases().getFirst().children().getFirst();
    }
}

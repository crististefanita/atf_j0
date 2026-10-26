package atf.console;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.contract.AtfReportStatus;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class AtfConsoleReporterTest {
    @Test
    void defaultPolicyPrintsDetailAndCodeEvents() {
        var buffer = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(buffer), ConsoleSettings.defaults());
        reporter.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "status", "200"));
        reporter.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST, "response.body", "{\"ok\":true}"));
        var output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("status"));
        assertTrue(output.contains("200"));
        assertTrue(output.contains("response.body"));
        assertTrue(output.contains("ok"));
    }

    @Test
    void rendersNestedLifecycleWithStatusAndMilliseconds() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));

        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.SCENARIO, "PostsApiTests"));
        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.PHASE, "TEST BODY"));
        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST,
                "GET /public/v2/posts"));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                "GET /public/v2/posts", AtfReportStatus.PASS, 16));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.PHASE,
                "TEST BODY", AtfReportStatus.PASS, 18));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.SCENARIO,
                "PostsApiTests", AtfReportStatus.PASS, 20));

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(output.contains("[ATF] ▶ PostsApiTests")),
                () -> assertTrue(output.contains("[ATF]     ▶ GET /public/v2/posts")),
                () -> assertTrue(output.contains("[ATF]   ✅ PASS: TEST BODY (18 ms)")),
                () -> assertTrue(output.contains("[ATF] ✅ PASS: PostsApiTests (20 ms)")),
                () -> assertFalse(output.contains("null")));
    }

    @Test
    void rendersPublishedReportPathAsAnAbsoluteTerminalArtifactLine() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false), true);
        Path report = Path.of("target", "reports", "extent.html").toAbsolutePath().normalize();

        reporter.publish(AtfConsoleEvent.reportPublished(report));

        assertTrue(bytes.toString(StandardCharsets.UTF_8)
                .contains("[ATF] Extent report: " + report));
    }

    @Test
    void publishedReportPathIsOptInAndDoesNotLeakIntoOrdinaryConsoleRuns() {
        Path report = Path.of("target", "reports", "extent.html").toAbsolutePath().normalize();
        var defaultOutput = new ByteArrayOutputStream();
        var defaultReporter = new AtfConsoleReporter(new PrintStream(defaultOutput, true,
                StandardCharsets.UTF_8), new ConsoleSettings(true, false));
        defaultReporter.publish(AtfConsoleEvent.reportPublished(report));

        var optedInOutput = new ByteArrayOutputStream();
        var optedInReporter = new AtfConsoleReporter(new PrintStream(optedInOutput, true,
                StandardCharsets.UTF_8), new ConsoleSettings(true, false), true);
        optedInReporter.publish(AtfConsoleEvent.reportPublished(report));

        assertAll(
                () -> assertFalse(defaultOutput.toString(StandardCharsets.UTF_8).contains("Extent report:")),
                () -> assertTrue(optedInOutput.toString(StandardCharsets.UTF_8)
                        .contains("Extent report: " + report)));
    }

    @Test
    void switchesToSecondsOnlyAboveTheThreeHundredMillisecondThreshold() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));

        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                "short", AtfReportStatus.PASS, 300));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                "long", AtfReportStatus.PASS, 301));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                "second", AtfReportStatus.PASS, 1_000));

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(output.contains("Call completed (300 ms)")),
                () -> assertTrue(output.contains("Call completed (0.301 s)")),
                () -> assertTrue(output.contains("Call completed (1 s)")));
    }

    @Test
    void explicitDetailOptOutAndConsoleDisableAreRespected() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));
        reporter.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "Authorization", "Bearer ***REDACTED***"));
        String compact = bytes.toString(StandardCharsets.UTF_8);
        assertFalse(compact.contains("Authorization"));

        var disabled = new ByteArrayOutputStream();
        var silent = new AtfConsoleReporter(new PrintStream(disabled, true, StandardCharsets.UTF_8),
                new ConsoleSettings(false, true));
        silent.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.SCENARIO, "silent"));
        assertTrue(disabled.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    void ownsOneLineDiagnosticsForLaunchersAndOtherConsoleClients() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));

        reporter.info("debug target ready");

        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("[ATF] debug target ready"));
    }

    @Test
    void rendersRedactedDetailsWhenEnabled() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, true));
        reporter.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                "Authorization", "Bearer ***REDACTED***"));

        assertTrue(bytes.toString(StandardCharsets.UTF_8)
                .contains("• Authorization=Bearer ***REDACTED***"));
    }

    @Test
    void rendersPrettyBodyAsAnIndentedDiagnosticBlockWhenDetailsAreEnabled() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, true));

        reporter.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST,
                "response.body", "{\n  \"id\": 1\n}"));

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(output.contains("• response.body")),
                () -> assertTrue(output.contains("  {")),
                () -> assertTrue(output.contains("    \"id\": 1")),
                () -> assertFalse(output.contains("null")));
    }

    @Test
    void normalizesReportTypographyForConsoleOutput() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));

        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.SCENARIO,
                "Users API \u2014 setup \u2192 test"));

        assertTrue(bytes.toString(StandardCharsets.UTF_8)
                .contains("[ATF] ▶ Users API - setup -> test"));
    }

    @Test
    void formatsSingleLineBodyAndValidationLikeReferenceConsoleFlow() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, true));

        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST,
                "POST /public/v2/users"));
        reporter.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "method", "POST"));
        reporter.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST,
                "request.body", "{\"name\":\"ATF user\"}"));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                "POST /public/v2/users", AtfReportStatus.PASS, 12));
        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.VALIDATION,
                "Validate status: expect 201"));
        reporter.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.VALIDATION, "actual", "201"));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.VALIDATION,
                "Validate status: expect 201", AtfReportStatus.PASS, 1));

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("▶ POST /public/v2/users"));
        assertTrue(output.contains("• method=POST"));
        assertTrue(output.contains("• request.body={\"name\":\"ATF user\"}"));
        assertTrue(output.contains("✅ PASS: Call completed"));
        assertTrue(output.contains("▶ Validate status: expect 201"));
        assertTrue(output.contains("• actual=201"));
        assertTrue(output.contains("✅ PASS: Validated"));
    }

    @Test
    void rendersUiActionIdentityAsOperationArrowTarget() {
        var bytes = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));

        reporter.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.UI_ACTION,
                "Click -> #nav-test"));
        reporter.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.UI_ACTION,
                "Click -> #nav-test", AtfReportStatus.PASS, 3));

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(output.contains("▶ Click -> #nav-test")),
                () -> assertTrue(output.contains("✅ PASS: UI action completed (3 ms)")));
    }
}

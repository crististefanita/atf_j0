package atf.console.junit;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;

import atf.console.AtfConsoleReporter;
import atf.console.ConsoleSettings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class AtfConsoleTestLifecycleTest {
    @Test
    void directlyExecutedTestPublishesConfiguredScenarioAndPhase() {
        var output = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new ConsoleSettings(true, false));
        var ticks = new long[] {0, 8_000_000};
        var tickIndex = new int[] {0};
        AtfConsoleTestLifecycle lifecycle = new AtfConsoleTestLifecycle(
                reporter, "UsersApiTests", "getsUserCollectionAndValidatesItsContract",
                () -> ticks[tickIndex[0]++]);

        lifecycle.start();
        lifecycle.complete(AtfReportStatus.PASS);

        String text = output.toString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(text.contains("[ATF] ▶ UsersApiTests")),
                () -> assertTrue(text.contains("TEST BODY - getsUserCollectionAndValidatesItsContract")),
                () -> assertTrue(text.contains("✅ PASS: UsersApiTests (8 ms)")),
                () -> assertFalse(text.contains("null")));
    }

    @Test
    void disabledConsoleRemainsSilentForDirectTests() {
        var output = new ByteArrayOutputStream();
        var reporter = new AtfConsoleReporter(
                new PrintStream(output, true, StandardCharsets.UTF_8),
                ConsoleSettings.disabled());
        AtfConsoleTestLifecycle lifecycle = new AtfConsoleTestLifecycle(
                reporter, "UsersApiTests", "test", () -> 0);

        lifecycle.start();
        lifecycle.complete(AtfReportStatus.PASS);

        assertTrue(output.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    void completionIsIdempotentAndNeverPublishesASecondVerdict() {
        var events = new java.util.ArrayList<AtfConsoleEvent>();
        AtfConsoleSink sink = events::add;
        AtfConsoleTestLifecycle lifecycle = new AtfConsoleTestLifecycle(sink, "UsersApiTests", "test", () -> 0);

        lifecycle.start();
        lifecycle.complete(AtfReportStatus.PASS);
        lifecycle.complete(AtfReportStatus.FAIL);

        assertTrue(events.stream().filter(event -> event.type() == AtfConsoleEvent.Type.COMPLETE).count() == 2);
    }

    @Test
    void validationWarningIsReflectedInTheDirectConsoleVerdict() {
        var events = new java.util.ArrayList<AtfConsoleEvent>();
        var lifecycle = new AtfConsoleTestLifecycle(events::add, "UsersApiTests", "warning", () -> 0);

        lifecycle.start();
        lifecycle.recordStatus(AtfReportStatus.WARNING);
        lifecycle.complete(AtfReportStatus.PASS);

        assertTrue(events.stream().anyMatch(event ->
                event.type() == AtfConsoleEvent.Type.COMPLETE
                        && event.status() == AtfReportStatus.WARNING));
    }
}

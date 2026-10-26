package atf.console.junit;

import atf.testkit.AtfIntegrationTest;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Exercises the real Jupiter extension callbacks, rather than only its lifecycle helper. */
@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock("atf.report.config")
@AtfIntegrationTest
class AtfConsoleTestExtensionIntegrationTest {
    @TempDir Path temporary;

    @Test
    void jupiterOutcomesDriveConsoleLifecycleAndRestoreTheThreadContext() throws Exception {
        Path config = enabledConsoleConfig();

        CapturedRun successful = run(SuccessfulFixture.class, config);
        successful.results().testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
        assertTrue(successful.output().contains("PASS: SuccessfulFixture"));
        assertTrue(successful.output().contains("fixture=visible"),
                "the active policy and sink must be available to test-owned API/UI adapters");

        CapturedRun aborted = run(AbortedFixture.class, config);
        aborted.results().testEvents().assertStatistics(stats -> stats.started(1).aborted(1));
        assertTrue(aborted.output().contains("SKIP: AbortedFixture"));
        assertEqualsOccurrences(aborted.output(), "> AbortedFixture", 1);

        CapturedRun disabled = run(DisabledFixture.class, config);
        disabled.results().testEvents().assertStatistics(stats -> stats.skipped(1));
        assertTrue(disabled.output().isBlank(), "disabled tests must not fabricate a lifecycle");

        CapturedRun parameterized = run(ParameterizedFixture.class, config);
        parameterized.results().testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
        assertEqualsOccurrences(parameterized.output(), "> ParameterizedFixture", 2);
        assertEqualsOccurrences(parameterized.output(), "PASS: ParameterizedFixture", 2);

        CapturedRun dynamic = run(DynamicFixture.class, config);
        assertTrue(dynamic.output().contains("FAIL: TEST BODY - dynamic fail"),
                "dynamic invocation console output was: " + dynamic.output());
        dynamic.results().testEvents().assertStatistics(stats ->
                stats.started(3).succeeded(1).aborted(1).failed(1));
        assertEqualsOccurrences(dynamic.output(), "> DynamicFixture", 3);
        assertTrue(dynamic.output().contains("PASS: TEST BODY - dynamic pass"));
        assertTrue(dynamic.output().contains("SKIP: TEST BODY - dynamic abort"));
        assertTrue(dynamic.output().contains("FAIL: TEST BODY - dynamic fail"));
        assertTrue(dynamic.output().contains("fixture=dynamic-visible"),
                "dynamic children must inherit the owner's annotation policy and bound sink");

        CapturedRun teardownFailure = run(TeardownFailureFixture.class, config);
        teardownFailure.results().testEvents().assertStatistics(stats -> stats.started(1).failed(1));
        assertTrue(teardownFailure.output().contains("FAIL: TeardownFailureFixture"));

        ConcurrentFixture.LIFECYCLES.clear();
        CapturedRun concurrent = run(ConcurrentFixture.class, config, true);
        concurrent.results().testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
        assertEqualsOccurrences(concurrent.output(), "thread=one", 1);
        assertEqualsOccurrences(concurrent.output(), "thread=two", 1);
        org.junit.jupiter.api.Assertions.assertEquals(2, ConcurrentFixture.LIFECYCLES.size(),
                "parallel invocations must not share a thread-local test lifecycle");

        CapturedRun pathEnabled = run(ReportPathEnabledFixture.class, config);
        assertTrue(pathEnabled.output().contains("Extent report: "));
        CapturedRun pathDisabled = run(ReportPathDisabledFixture.class, config);
        org.junit.jupiter.api.Assertions.assertFalse(pathDisabled.output().contains("Extent report:"));

        assertNull(AtfConsoleTestContext.current(), "the extension must clear its thread-bound lifecycle");
    }

    private Path enabledConsoleConfig() throws Exception {
        Path config = temporary.resolve("atf-console.yaml");
        Files.writeString(config, """
                reporting:
                  console:
                    enabled: true
                    detailsEnabled: false
                """);
        return config;
    }

    private static CapturedRun run(Class<?> fixture, Path config) {
        return run(fixture, config, false);
    }

    private static CapturedRun run(Class<?> fixture, Path config, boolean parallel) {
        String property = "atf.report.config";
        String previousProperty = System.getProperty(property);
        PrintStream previousOut = System.out;
        var captured = new ByteArrayOutputStream();
        try (var output = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setProperty(property, config.toString());
            System.setOut(output);
            var engine = EngineTestKit.engine("junit-jupiter").selectors(selectClass(fixture));
            if (parallel) engine.configurationParameter("junit.jupiter.execution.parallel.enabled", "true");
            EngineExecutionResults results = engine.execute();
            output.flush();
            return new CapturedRun(captured.toString(StandardCharsets.UTF_8), results);
        } finally {
            System.setOut(previousOut);
            if (previousProperty == null) System.clearProperty(property);
            else System.setProperty(property, previousProperty);
        }
    }

    private static void assertEqualsOccurrences(String text, String expected, int count) {
        long actual = text.lines().filter(line -> line.contains(expected)).count();
        org.junit.jupiter.api.Assertions.assertEquals(count, actual, expected);
    }

    private static void assertEqualsOccurrences(String text, String expected, int count, String reason) {
        long actual = text.lines().filter(line -> line.contains(expected)).count();
        org.junit.jupiter.api.Assertions.assertEquals(count, actual, reason + ": " + expected);
    }

    private record CapturedRun(String output, EngineExecutionResults results) { }

    @ExtendWith(AtfConsoleTestExtension.class)
    @AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
    static class SuccessfulFixture {
        @Test
        void emitsDetailFromTheBoundConsoleSink() {
            AtfConsoleTestContext.configuredSink().publish(
                    AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "fixture", "visible"));
        }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    static class AbortedFixture {
        @Test
        void abortsExplicitly() { Assumptions.abort("intentional lifecycle probe"); }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    static class DisabledFixture {
        @Disabled("disabled lifecycle probe")
        @Test
        void doesNotStart() { throw new AssertionError("disabled test executed"); }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    static class ParameterizedFixture {
        @ParameterizedTest
        @ValueSource(ints = {1, 2})
        void startsAndCompletesOncePerInvocation(int value) { assertTrue(value > 0); }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    @AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
    static class DynamicFixture {
        @TestFactory
        Stream<org.junit.jupiter.api.DynamicTest> createsDynamicChildren() {
            return Stream.of(
                    AtfConsoleTestContext.dynamicTest(DynamicFixture.class, "dynamic pass", () -> {
                        AtfConsoleTestContext.configuredSink().publish(
                                AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST,
                                        "fixture", "dynamic-visible"));
                        assertTrue(true);
                    }),
                    AtfConsoleTestContext.dynamicTest(DynamicFixture.class, "dynamic abort",
                            () -> Assumptions.abort("intentional dynamic abort")),
                    AtfConsoleTestContext.dynamicTest(DynamicFixture.class, "dynamic fail",
                            () -> { throw new AssertionError("intentional dynamic failure"); }));
        }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    static class TeardownFailureFixture {
        @Test void bodyPasses() { assertTrue(true); }
        @AfterEach void teardownFails() { throw new IllegalStateException("intentional teardown failure"); }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    @AtfReportPath
    static class ReportPathEnabledFixture {
        @Test void publishesOnlyWhenOptedIn() {
            AtfConsoleTestContext.configuredSink().publish(AtfConsoleEvent.reportPublished(
                    Path.of("reports", "report.html").toAbsolutePath().normalize()));
        }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    static class ReportPathDisabledFixture {
        @Test void leavesPathOutputOffWithoutAnnotation() {
            AtfConsoleTestContext.configuredSink().publish(AtfConsoleEvent.reportPublished(
                    Path.of("reports", "report.html").toAbsolutePath().normalize()));
        }
    }

    @ExtendWith(AtfConsoleTestExtension.class)
    @AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
    @Execution(ExecutionMode.CONCURRENT)
    static class ConcurrentFixture {
        private static final CyclicBarrier BOTH_TEST_THREADS = new CyclicBarrier(2);
        private static final Set<AtfConsoleTestLifecycle> LIFECYCLES = ConcurrentHashMap.newKeySet();

        @Test void firstConcurrentInvocation() throws Exception { emitAfterBarrier("one"); }
        @Test void secondConcurrentInvocation() throws Exception { emitAfterBarrier("two"); }

        private static void emitAfterBarrier(String id) throws Exception {
            AtfConsoleTestLifecycle lifecycle = AtfConsoleTestContext.current();
            if (lifecycle == null) throw new AssertionError("test thread has no console lifecycle");
            LIFECYCLES.add(lifecycle);
            BOTH_TEST_THREADS.await(5, TimeUnit.SECONDS);
            lifecycle.console().publish(
                    AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "thread", id));
        }
    }
}

package atf.testkit.assertions;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@AtfComponentTest
class ComponentPhaseTimingExtensionComponentTest {
    private static final ThreadLocal<Probe> ACTIVE = new ThreadLocal<>();
    private static final class Probe {
        final AtomicLong ticks = new AtomicLong();
        final List<ComponentAssertionAudit.Result> results = new ArrayList<>();
        final RuntimeException primary = new IllegalStateException("controlled execution");
        boolean publicationFails;
        TestExecutionSummary summary;
        void advance(int millis) { ticks.addAndGet(Duration.ofMillis(millis).toNanos()); }
    }
    @Test void repeatedCallbacksAndTemplatesExcludeAcquisitionPublicationAndClose() {
        var probe = launch(false, TemplateCase.class);
        AuditedAssertions.assertEquals(2, probe.summary.getTestsSucceededCount());
        AuditedAssertions.assertEquals(2, probe.results.size());
        for (var result : probe.results) {
            AuditedAssertions.assertEquals(List.of(12L, 20L, 11L), result.phases().stream()
                    .map(p -> p.timing().orElseThrow().elapsed().toMillis()).toList());
            AuditedAssertions.assertEquals(List.of(2, 1, 2), result.phases().stream()
                    .map(p -> p.timing().orElseThrow().intervals()).toList());
        }
        AuditedAssertions.assertEquals(12_086, probe.ticks.get() / 1_000_000L);
    }
    @Test void bodyFailureKeepsTimingAndTheOriginalErrorWhenPublicationAlsoFails() {
        var probe = launch(true, FailureCase.class);
        AuditedAssertions.assertEquals(1, probe.summary.getTestsFailedCount());
        AuditedAssertions.assertSame(probe.primary, probe.summary.getFailures().getFirst().getException());
        AuditedAssertions.assertEquals(Duration.ofMillis(30), probe.results.getFirst().phases().getFirst().timing().orElseThrow().elapsed());
        AuditedAssertions.assertEquals("publication", probe.primary.getSuppressed()[0].getMessage());
    }
    @Test void classSetupFailureMeasuresCallbacksBeforeOpeningTheDiagnosticPublisher() {
        var probe = launch(false, ClassFailureCase.class);
        AuditedAssertions.assertEquals(1, probe.summary.getContainersFailedCount());
        AuditedAssertions.assertSame(probe.primary, probe.summary.getFailures().getFirst().getException());
        var phase = probe.results.getFirst().phases().getFirst();
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Phase.SETUP, phase.phase());
        AuditedAssertions.assertEquals(Duration.ofMillis(700), phase.timing().orElseThrow().elapsed());
        AuditedAssertions.assertTrue(phase.checks().isEmpty());
    }
    @Test void abortedBodyAndIndependentLaunchesRetainTheirOwnIntervals() {
        var abort = launch(false, AbortCase.class);
        AuditedAssertions.assertEquals(1, abort.summary.getTestsAbortedCount());
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Outcome.ABORTED, abort.results.getFirst().outcome());
        AuditedAssertions.assertEquals(Duration.ofMillis(40), abort.results.getFirst().phases().getFirst().timing().orElseThrow().elapsed());
        var second = launch(false, TemplateCase.class);
        AuditedAssertions.assertEquals(2, second.results.size());
        AuditedAssertions.assertEquals(Duration.ofMillis(20), second.results.getLast().phases().get(1).timing().orElseThrow().elapsed());
    }
    @Test void failedMethodLifecycleClosesOnlyTheCallbacksThatActuallyRan() {
        var setup = launch(false, SetupFailureCase.class);
        AuditedAssertions.assertSame(setup.primary, setup.summary.getFailures().getFirst().getException());
        AuditedAssertions.assertEquals(1, setup.results.getFirst().phases().size());
        AuditedAssertions.assertEquals(Duration.ofMillis(9), setup.results.getFirst().phases().getFirst().timing().orElseThrow().elapsed());
        var teardown = launch(false, TeardownFailureCase.class);
        AuditedAssertions.assertSame(teardown.primary, teardown.summary.getFailures().getFirst().getException());
        AuditedAssertions.assertEquals(List.of(20L, 11L), teardown.results.getFirst().phases().stream()
                .map(phase -> phase.timing().orElseThrow().elapsed().toMillis()).toList());
    }
    @Test void classTeardownFailureKeepsItsOwnTimingAfterTheSuccessfulBody() {
        var probe = launch(false, ClassTeardownFailureCase.class);
        AuditedAssertions.assertEquals(1, probe.summary.getContainersFailedCount());
        AuditedAssertions.assertEquals(1, probe.summary.getTestsSucceededCount());
        AuditedAssertions.assertSame(probe.primary, probe.summary.getFailures().getFirst().getException());
        var phase = probe.results.getLast().phases().getFirst();
        AuditedAssertions.assertEquals(ComponentAssertionAudit.Phase.TEARDOWN, phase.phase());
        AuditedAssertions.assertEquals(Duration.ofMillis(80), phase.timing().orElseThrow().elapsed());
        AuditedAssertions.assertTrue(phase.checks().isEmpty());
    }
    private static Probe launch(boolean fails, Class<?> fixture) {
        var probe = new Probe(); probe.publicationFails = fails; ACTIVE.set(probe);
        try {
            var listener = new SummaryGeneratingListener();
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(selectClass(fixture))
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").build(), listener);
            probe.summary = listener.getSummary(); return probe;
        } finally { ACTIVE.remove(); }
    }
    public static class Adapter implements ComponentAuditAdapter {
        private final ComponentAuditExtension audit = new ComponentAuditExtension(Adapter.class, context -> {
            var probe = ACTIVE.get(); probe.advance(1_000);
            return new ComponentAuditExtension.Publication() {
                public AssertionValueDisplay display() { return new AssertionValueDisplay(240, (name, text) -> text); }
                public void publish(ComponentAssertionAudit.Result result) {
                    probe.results.add(result); probe.advance(2_000);
                    if (probe.publicationFails) throw new IllegalStateException("publication");
                }
                public void close() { probe.advance(3_000); }
            };
        }, () -> ACTIVE.get().ticks.get());
        public ComponentAuditExtension auditExtension() { return audit; }
    }
    public static class Parent {
        @BeforeEach void parentSetup() { ACTIVE.get().advance(5); }
        @AfterEach void parentTeardown() { ACTIVE.get().advance(4); }
    }
    @ExtendWith(Adapter.class) public static class TemplateCase extends Parent {
        @BeforeEach void setup() { ACTIVE.get().advance(7); }
        @AfterEach void teardown() { ACTIVE.get().advance(7); }
        @RepeatedTest(2) void body() {
            ACTIVE.get().advance(20); AuditedAssertions.assertTrue(true);
        }
    }
    @ExtendWith(Adapter.class) public static class FailureCase {
        @Test void body() { ACTIVE.get().advance(30); throw ACTIVE.get().primary; }
    }
    @ExtendWith(Adapter.class) public static class ClassFailureCase {
        @BeforeAll static void setup() { ACTIVE.get().advance(700); throw ACTIVE.get().primary; }
        @Test void unreachable() { throw new AssertionError("unreachable"); }
    }
    @ExtendWith(Adapter.class) public static class AbortCase {
        @Test void body() { ACTIVE.get().advance(40); Assumptions.abort("controlled abort"); }
    }
    @ExtendWith(Adapter.class) public static class SetupFailureCase {
        @BeforeEach void setup() { ACTIVE.get().advance(9); throw ACTIVE.get().primary; }
        @Test void unreachable() { throw new AssertionError("unreachable"); }
    }
    @ExtendWith(Adapter.class) public static class TeardownFailureCase {
        @Test void body() { ACTIVE.get().advance(20); AuditedAssertions.assertTrue(true); }
        @AfterEach void teardown() { ACTIVE.get().advance(11); throw ACTIVE.get().primary; }
    }
    @ExtendWith(Adapter.class) public static class ClassTeardownFailureCase {
        @Test void body() { ACTIVE.get().advance(20); AuditedAssertions.assertTrue(true); }
        @AfterAll static void teardown() { ACTIVE.get().advance(80); throw ACTIVE.get().primary; }
    }
}

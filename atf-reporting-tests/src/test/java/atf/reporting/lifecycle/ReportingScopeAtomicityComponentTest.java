package atf.reporting.lifecycle;

import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.semantic.AtfReportSemanticRecorder;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static atf.reporting.contract.AtfReportStatus.FAIL;
import static atf.reporting.contract.AtfReportStatus.PASS;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ReportingScopeAtomicityComponentTest {
    private static final AtfAssertion GOOD = new AtfAssertion("Stable audited comparison", "ready", "ready", true);
    private static final AtfAssertion BAD = new AtfAssertion("Independent failed comparison", "ready", "invalid", false);
    private static final AtfUiActionRecord ACTION = new AtfUiActionRecord(1, "Observe", "local fixture", "ready", "ready", 1, PASS);

    @ParameterizedTest(name = "{0} · late request {1}")
    @MethodSource("lateRequests")
    void rejectedRequestMutationsRetainSnapshotAndPublishThePreviousValidState(
            ScopeAtomicityFixture.Backend backend, RequestMutation mutation) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "late-" + mutation)) {
            var validation = f.request.startValidation("Original validation");
            validation.assertions(List.of(GOOD));
            validation.complete(PASS);
            f.request.complete(PASS);
            var before = f.snapshot();

            assertThrows(IllegalStateException.class, () -> mutation.apply(f.request));
            assertEquals(before, f.snapshot(), "Rejected mutation must change neither graph nor values");
            f.finishParents();
            assertEquals(PASS, f.status());
            assertTrue(Files.isRegularFile(f.file));
            assertFalse(Files.readString(f.file).contains("REJECTED_EVIDENCE"));
        }
    }

    static Stream<Arguments> lateRequests() {
        return backends().flatMap(backend -> Stream.of(RequestMutation.values()).map(op -> Arguments.of(backend, op)));
    }

    enum RequestMutation {
        DETAIL(r -> r.detail("REJECTED_EVIDENCE", "late")),
        JSON(r -> r.json("REJECTED_EVIDENCE", "{}")),
        BODY(r -> r.body(new ReportJsonBody("REJECTED_EVIDENCE", "{}"))),
        EXECUTION(r -> r.execution((ReportExecutionEvidence) null)),
        ACTION(r -> r.uiActions(List.of(ReportingScopeAtomicityComponentTest.ACTION))),
        SCREENSHOT(r -> r.screenshot("REJECTED_EVIDENCE", ReportTestScreenshots.ONE_PIXEL_PNG)),
        AGGREGATE(r -> r.networkAggregate(ReportNetworkInspector.aggregate(List.of(), List.of()))),
        MODE(r -> r.uiActionDetailsMode(ReportUiActionDetailsMode.SUMMARY_ONLY)),
        CHILD(r -> r.startValidation("REJECTED_EVIDENCE")),
        COMPLETE(r -> r.complete(FAIL));
        final Consumer<AtfReportPorts.Request> action;
        RequestMutation(Consumer<AtfReportPorts.Request> action) { this.action = action; }
        void apply(AtfReportPorts.Request request) { action.accept(request); }
    }

    @ParameterizedTest(name = "{0} · open child of {1}")
    @MethodSource("openParents")
    void aParentRefusalDoesNotCommitAndCanRecoverAfterItsChildCompletes(
            ScopeAtomicityFixture.Backend backend, Parent parent) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "open-" + parent)) {
            var validation = f.request.startValidation("Independent validation");
            validation.assertions(List.of(BAD));
            var nested = parent == Parent.VALIDATION ? validation.startRequest("Nested request", "GET", "/nested") : null;
            var before = f.snapshot();
            Runnable invalid = switch (parent) {
                case SCENARIO -> () -> f.scenario.complete(PASS);
                case PHASE -> () -> f.phase.complete(PASS);
                case REQUEST -> () -> f.request.complete(PASS);
                case VALIDATION -> () -> validation.complete(PASS);
            };
            assertThrows(IllegalStateException.class, invalid::run);
            assertEquals(before, f.snapshot(), "Refusal must not change status, children, timing or counters");
            if (nested != null) nested.complete(PASS);
            validation.complete(FAIL);
            f.finishRequest(PASS);
            assertEquals(FAIL, f.status(), "Child FAIL remains authoritative after a rejected caller PASS");
            assertTrue(Files.isRegularFile(f.file));
        }
    }

    enum Parent { SCENARIO, PHASE, REQUEST, VALIDATION }
    static Stream<Arguments> openParents() {
        return backends().flatMap(backend -> Stream.of(Parent.values()).map(parent -> Arguments.of(backend, parent)));
    }

    @ParameterizedTest(name = "{0} · invalid payload")
    @EnumSource(ScopeAtomicityFixture.Backend.class)
    void invalidPayloadDoesNotErasePreviousRowsOrIncreaseEvidenceCounts(ScopeAtomicityFixture.Backend backend) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "invalid-payload")) {
            var validation = f.request.startValidation("Validated inputs");
            validation.assertions(List.of(GOOD));
            var before = f.snapshot();
            var nullRow = Arrays.asList(GOOD, (AtfAssertion) null);
            assertThrows(NullPointerException.class, () -> validation.assertions(nullRow));
            assertThrows(NullPointerException.class, () -> validation.uiActions(Arrays.asList(ACTION, null)));
            assertThrows(IllegalArgumentException.class, () -> validation.screenshot("invalid", ""));
            assertThrows(NullPointerException.class, () -> f.request.body(null));
            assertThrows(NullPointerException.class, () -> f.request.uiActions(Arrays.asList(ACTION, null)));
            assertThrows(IllegalArgumentException.class, () -> f.request.screenshot("invalid", ""));
            assertEquals(before, f.snapshot());
            validation.complete(PASS);
            f.finishRequest(PASS);
            assertEquals(PASS, f.status());
        }
    }

    @Test
    void directSemanticRecorderRejectsInvalidRowsBeforeChangingItsReadModel() {
        var closed = new AtomicBoolean();
        var recorder = new AtfReportSemanticRecorder(() -> {
            if (closed.get()) throw new IllegalStateException("Session closed");
        });
        var scenario = recorder.startScenario("Semantic atomicity", "", "invalid rows", List.of());
        var phase = scenario.startPhase("TEST BODY");
        var request = phase.startRequest("request", "GET", "/");
        var validation = request.startValidation("validation");
        validation.assertions(List.of(GOOD));
        var before = recorder.snapshot();
        assertThrows(NullPointerException.class, () -> validation.assertions(Arrays.asList(GOOD, null)));
        assertEquals(before, recorder.snapshot());
        validation.complete(PASS); request.complete(PASS); phase.complete(PASS); scenario.complete(PASS);
        closed.set(true);
        assertThrows(IllegalStateException.class, () -> recorder.startScenario("late", "", "late", List.of()));
    }

    @ParameterizedTest(name = "{0} · stable caller collection")
    @EnumSource(ScopeAtomicityFixture.Backend.class)
    void validationCapturesOneStableCopyBeforeUpdatingItsTwoPopulations(ScopeAtomicityFixture.Backend backend) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "caller-collection")) {
            var reads = new AtomicInteger();
            var callerRows = new AbstractCollection<AtfAssertion>() {
                @Override public int size() { return 1; }
                @Override public Iterator<AtfAssertion> iterator() {
                    if (reads.incrementAndGet() > 1) throw new IllegalStateException("Caller collection changed");
                    return List.of(GOOD).iterator();
                }
            };
            var validation = f.request.startValidation("One immutable input");
            assertDoesNotThrow(() -> validation.assertions(callerRows));
            assertEquals(1, reads.get(), "Renderer and recorder must receive the same captured rows");
            validation.complete(PASS); f.finishRequest(PASS);
            assertEquals(PASS, f.status());
        }
    }

    @ParameterizedTest(name = "{0} · close is not complete")
    @EnumSource(ScopeAtomicityFixture.Backend.class)
    void closeIsIdempotentRejectsFurtherWritesAndCompletesAnUnfinishedLeafAsFail(ScopeAtomicityFixture.Backend backend) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "closed-before-complete")) {
            var validation = f.request.startValidation("Unfinished validation");
            validation.close(); validation.close();
            var before = f.snapshot();
            assertThrows(IllegalStateException.class, () -> validation.complete(PASS));
            assertThrows(IllegalStateException.class, () -> validation.assertions(List.of(GOOD)));
            assertEquals(before, f.snapshot());
            f.finishRequest(PASS);
            assertEquals(FAIL, f.status());
        }
    }

    @ParameterizedTest(name = "{0} · parent complete output {1}")
    @MethodSource("completionErrors")
    void aDiagnosticOutputFailureCannotDuplicateCompletionOrBlockTheParent(
            ScopeAtomicityFixture.Backend backend, AtfConsoleEvent.Scope scope) throws Exception {
        var armed = new AtomicBoolean();
        var failures = new AtomicInteger();
        var primary = new IllegalStateException("Controlled output failure");
        try (var f = new ScopeAtomicityFixture(backend, "console-" + scope, event -> {
            if (armed.get() && event.type() == AtfConsoleEvent.Type.COMPLETE && event.scope() == scope) {
                armed.set(false); failures.incrementAndGet(); throw primary;
            }
        }, false)) {
            var validation = f.request.startValidation("Valid comparison");
            validation.assertions(List.of(GOOD));
            if (scope == AtfConsoleEvent.Scope.VALIDATION) {
                armed.set(true);
                assertDoesNotThrow(() -> validation.complete(PASS));
                assertThrows(IllegalStateException.class, () -> validation.complete(FAIL));
            } else validation.complete(PASS);
            if (scope == AtfConsoleEvent.Scope.REQUEST) {
                armed.set(true);
                assertDoesNotThrow(() -> f.request.complete(PASS));
                assertThrows(IllegalStateException.class, () -> f.request.complete(FAIL));
            } else f.request.complete(PASS);
            f.finishParents();
            assertEquals(1, failures.get(), "Finalization callback and console completion happen once");
            assertEquals(PASS, f.status());
        }
    }

    static Stream<Arguments> completionErrors() {
        return backends().flatMap(backend -> Stream.of(AtfConsoleEvent.Scope.VALIDATION, AtfConsoleEvent.Scope.REQUEST)
                .map(scope -> Arguments.of(backend, scope)));
    }

    @ParameterizedTest(name = "{0} · failed start {1}")
    @MethodSource("startErrors")
    void aDiagnosticOutputFailureDuringStartStillReturnsACompletableChild(
            ScopeAtomicityFixture.Backend backend, boolean assertionStep) throws Exception {
        var armed = new AtomicBoolean();
        var primary = new IllegalStateException("Controlled start failure");
        try (var f = new ScopeAtomicityFixture(backend, "start-" + assertionStep, event -> {
            if (armed.get() && event.type() == AtfConsoleEvent.Type.START) throw primary;
        }, false)) {
            armed.set(true);
            if (assertionStep) {
                var child = f.phase.startAssertionStep("Accepted child despite diagnostic failure");
                child.assertions(List.of(GOOD)); child.complete(PASS);
            } else {
                var child = f.request.startValidation("Accepted child despite diagnostic failure");
                child.assertions(List.of(GOOD)); child.complete(PASS);
            }
            armed.set(false);
            f.finishRequest(PASS);
            assertEquals(PASS, f.status());
        }
    }
    static Stream<Arguments> startErrors() {
        return backends().flatMap(backend -> Stream.of(false, true).map(value -> Arguments.of(backend, value)));
    }

    @ParameterizedTest(name = "{0} · aggregate replacement and details")
    @EnumSource(ScopeAtomicityFixture.Backend.class)
    void aggregateReplacementKeepsOneCanonicalMarkerAndAllowsLaterDetails(ScopeAtomicityFixture.Backend backend) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "aggregate-replacement")) {
            f.request.detail("before", "one");
            f.request.networkAggregate(aggregate(1));
            f.request.detail("after", "two");
            var before = f.snapshot();
            f.request.networkAggregate(null);
            f.request.networkAggregate(aggregate(2));
            assertEquals(before, f.snapshot(), "Aggregate replacement is one evidence slot, not an appended detail");
            var request = (AtfReportSemanticSnapshot.RequestSnapshot) f.snapshot().classes().getFirst()
                    .scenarios().getFirst().phases().getFirst().nodes().getFirst();
            assertEquals(List.of("before", "after", "network.aggregate"), request.detailNames());
            f.finishRequest(PASS);
            assertTrue(Files.isRegularFile(f.file), "Fallback must pass its independent semantic guard");
            assertTrue(Files.readString(f.file).contains("Network aggregate · 2 exchanges"), "Title must describe the latest aggregate");
            assertFalse(Files.readString(f.file).contains("Network aggregate · 1 exchanges"), "Replacement must not retain a stale counter");
        }
    }

    private static ReportNetworkInspector.JourneyAggregate aggregate(int observations) {
        return new ReportNetworkInspector.JourneyAggregate(
                List.of(new ReportNetworkInspector.AuthenticationObservation("none", "controlled", 200, observations)),
                List.of(), new ReportNetworkInspector.TransportCounters(observations, observations, 0, observations, 0, 0, observations, 0),
                List.of(), List.of());
    }

    @ParameterizedTest(name = "{0} · session publication failure")
    @EnumSource(ScopeAtomicityFixture.Backend.class)
    void publicationFailurePreservesCauseAndClosesTheLogicalSession(ScopeAtomicityFixture.Backend backend) throws Exception {
        try (var f = new ScopeAtomicityFixture(backend, "blocked-publication", event -> { }, true)) {
            f.request.complete(PASS); f.phase.complete(PASS); f.scenario.complete(PASS);
            var before = f.snapshot();
            var failure = assertThrows(IllegalStateException.class, f.session::close);
            assertNotNull(failure.getCause(), "Publication retains its IO cause");
            assertEquals(before, f.snapshot());
            assertFalse(Files.isRegularFile(f.file));
            assertThrows(IllegalStateException.class, () -> f.session.startScenario("late", "late", List.of()));
            assertDoesNotThrow(f.session::close, "A second close does not attempt another publication");
        }
    }

    private static Stream<ScopeAtomicityFixture.Backend> backends() {
        return Stream.of(ScopeAtomicityFixture.Backend.values());
    }
}


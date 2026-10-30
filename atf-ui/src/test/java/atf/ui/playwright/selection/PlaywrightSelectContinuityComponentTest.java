package atf.ui.playwright.selection;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.support.selection.SelectContinuityFixture;
import atf.ui.tests.support.selection.SelectContinuityBrowser;
import com.microsoft.playwright.TimeoutError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class PlaywrightSelectContinuityComponentTest {
    private SelectContinuityBrowser browser;
    @BeforeAll void openBrowser() { browser = new SelectContinuityBrowser(); }
    @AfterAll void closeBrowser() { if (browser != null) browser.close(); }
    private SelectContinuityFixture fixture(String mode) throws Exception { return fixture(mode, "top", false); }
    private SelectContinuityFixture fixture(String mode, String frame, boolean sensitive) throws Exception { return browser.open(mode, frame, sensitive); }
    @ParameterizedTest(name = "single native selection · {0}")
    @ValueSource(strings = {"replacement", "hidden-replacement", "option-replacement", "fieldset-replacement", "aria-replacement", "repeated-replacement"})
    void replacementIsObservedBeforeTheOnlyNativeActivation(String mode) throws Exception {
        try (var fixture = fixture(mode)) {
            fixture.select();
            assertEquals("blue", fixture.value(), "The current control actually holds the configured selection");
            assertEquals(1, fixture.nativeCalls(), "Readiness precedes the first and only native selectOption invocation");
            assertEquals(1, fixture.inputs(), "Exactly one input event is dispatched");
            assertEquals(1, fixture.changes(), "Exactly one change event is dispatched");
            assertEquals(0, fixture.oldActivations(), "The detached or disabled old control is never activated");
            assertRecord(fixture, "PASS");
        }
    }

    @ParameterizedTest @CsvSource({"replacement,same", "replacement,cross", "option-replacement,cross"})
    void frameLocatorsKeepTheirOwnDocumentAndOneActivation(String mode, String frame) throws Exception {
        try (var fixture = fixture(mode, frame, false)) {
            fixture.select();
            assertEquals("blue", fixture.value()); assertEquals(1, fixture.nativeCalls());
            assertEquals(1, fixture.inputs()); assertEquals(1, fixture.changes()); assertEquals(0, fixture.oldActivations());
            assertRecord(fixture, "PASS");
        }
    }

    @Test void navigationBeforeActivationResolvesTheCurrentDocument() throws Exception {
        try (var fixture = fixture("document")) {
            fixture.select();
            assertTrue(fixture.url().contains("/ready")); assertEquals("blue", fixture.value());
            assertEquals(1, fixture.nativeCalls()); assertEquals(1, fixture.changes()); assertEquals(0, fixture.oldActivations());
            assertRecord(fixture, "PASS");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "ambiguous-replacement", "stays-disabled"})
    void unavailableOrAmbiguousTargetsFailBeforeAnyNativeMutation(String mode) throws Exception {
        try (var fixture = fixture(mode)) {
            long start = System.nanoTime();
            assertThrows(RuntimeException.class, fixture::select);
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 1600, "One 700ms action budget is never renewed");
            assertEquals(0, fixture.nativeCalls()); assertEquals(0, fixture.inputs()); assertEquals(0, fixture.changes());
            assertEquals(0, fixture.oldActivations()); assertRecord(fixture, "FAIL");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"same", "cross"})
    void aDisappearingFrameNeverDispatchesOrReceivesAnotherBudget(String frame) throws Exception {
        try (var fixture = fixture("frame-removed", frame, false)) {
            long start = System.nanoTime();
            assertThrows(RuntimeException.class, fixture::select);
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 1600, "A detached frame cannot start another timeout");
            assertEquals(0, fixture.nativeCalls()); assertEquals(0, fixture.inputs()); assertEquals(0, fixture.changes());
            assertRecord(fixture, "FAIL");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"label-attribute", "multi"})
    void nativeLabelMatchingAndMultipleSelectSemanticsRemainAuthoritative(String mode) throws Exception {
        try (var fixture = fixture(mode)) {
            fixture.select();
            assertEquals("blue", fixture.value()); assertEquals(1, fixture.nativeCalls()); assertEquals(1, fixture.changes());
            assertRecord(fixture, "PASS");
        }
    }

    @Test void nativeSelectionReceivesOnlyWhatRemainsAfterReadiness() throws Exception {
        try (var fixture = fixture("late-ready")) {
            fixture.select();
            assertNotNull(fixture.nativeTimeout());
            assertTrue(fixture.nativeTimeout() > 0 && fixture.nativeTimeout() < 350, "Readiness consumes the original budget before native dispatch");
            assertEquals("blue", fixture.value()); assertEquals(1, fixture.nativeCalls()); assertEquals(1, fixture.changes());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"empty", "native-detach"})
    void anEmptyNativeResultNeverAuthorizesASecondAttempt(String mode) throws Exception {
        try (var fixture = fixture(mode)) {
            var failure = assertThrows(IllegalStateException.class, fixture::select);
            assertTrue(failure.getMessage().contains("no confirmed selected value"));
            assertEquals(1, fixture.nativeCalls()); assertEquals(0, fixture.inputs()); assertEquals(0, fixture.changes());
            assertRecord(fixture, "FAIL");
        }
    }

    @Test void uncertaintyAfterANativeChangeRetainsFailureAndNeverReplays() throws Exception {
        try (var fixture = fixture("unconfirmed")) {
            var failure = assertThrows(TimeoutError.class, fixture::select);
            assertSame(fixture.unconfirmed(), failure.getCause(), "The original native timeout remains the cause");
            assertEquals(1, fixture.nativeCalls()); assertEquals(1, fixture.inputs()); assertEquals(1, fixture.changes());
            assertRecord(fixture, "FAIL");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"replacement", "unconfirmed"})
    void sensitiveLabelsRemainPrivateInRecordsTraceAndFailure(String mode) throws Exception {
        try (var fixture = fixture(mode, "top", true)) {
            if (mode.equals("unconfirmed")) {
                var failure = assertThrows(RuntimeException.class, fixture::select);
                assertFalse(failure.getMessage().contains(SelectContinuityFixture.PRIVATE_LABEL));
                assertRecord(fixture, "FAIL");
            } else { fixture.select(); assertEquals("blue", fixture.value()); assertRecord(fixture, "PASS"); }
            assertEquals(1, fixture.nativeCalls()); assertEquals(1, fixture.changes());
            assertFalse(fixture.records().toString().contains(SelectContinuityFixture.PRIVATE_LABEL));
            assertFalse(fixture.trace().toString().contains(SelectContinuityFixture.PRIVATE_LABEL));
            assertTrue(fixture.records().getFirst().expected().contains("[REDACTED]"));
        }
    }

    private static void assertRecord(SelectContinuityFixture fixture, String status) {
        assertEquals(1, fixture.records().size(), "Observation and native activation have one action record and no replay rows");
        var record = fixture.records().getFirst(); assertEquals("Select option", record.operation());
        assertEquals("#target", record.target()); assertEquals(status, record.status().name());
        assertTrue(record.durationMillis() > 0, "The full action, including readiness, has measured timing");
    }
}

package atf.reporting.browser.contract;

import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import atf.reporting.browser.process.BrowserPhaseDeadline;
import atf.reporting.browser.process.BrowserPhaseChannel.Frame;
import atf.testkit.AtfComponentTest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
@org.junit.jupiter.api.Tag("report-browser")
class BrowserPhaseDeadlineContractTest {
    private static final BrowserProbeBudget BUDGET = new BrowserProbeBudget(Duration.ofNanos(5), Duration.ofNanos(5), Duration.ofNanos(5));

    @Test void aNewCleanupScopeKeepsItsOwnDeadlineWhenTheIntermediateVerifyWasNotObserved() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        deadline.observe("phase-00000001-CLEANUP"); now.set(4);
        deadline.observe("phase-00000003-CLEANUP"); now.set(6);
        assertFalse(deadline.expired(BUDGET)); now.set(9); assertTrue(deadline.expired(BUDGET));
    }

    @Test void rereadingTheSameMarkerCannotRenewAHungCleanup() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        deadline.observe("phase-00000001-CLEANUP"); now.set(4); deadline.observe("phase-00000001-CLEANUP");
        now.set(5); assertTrue(deadline.expired(BUDGET));
    }

    @Test void terminalMarkersCannotGrantAnotherCleanupWindow() {
        for (String terminal : java.util.List.of("DONE", "FAILED")) {
            var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
            deadline.observe("phase-00000001-CLEANUP"); now.set(4); deadline.observe("phase-00000002-" + terminal);
            now.set(5); assertTrue(deadline.expired(BUDGET)); assertEquals("CLEANUP", deadline.phase());
        }
    }

    @Test void aVerificationScopeDoesNotConsumeTheFollowingSetupDeadline() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        deadline.observe("phase-00000001-VERIFY"); now.set(4); deadline.observe("phase-00000002-SETUP");
        now.set(8); assertFalse(deadline.expired(BUDGET)); now.set(9); assertTrue(deadline.expired(BUDGET));
    }

    @Test void firstSetupMarkerCannotRenewTheProcessStartupBudget() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        now.set(4); deadline.observe(new Frame("phase-00000001-SETUP", 100), BUDGET);
        now.set(5); assertTrue(deadline.expired(BUDGET));
    }

    @Test void aCompletedSetupWithinItsBudgetSurvivesDelayedObservation() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        deadline.observe(new Frame("phase-00000001-SETUP", 100), BUDGET);
        now.set(5); assertTrue(deadline.expired(BUDGET));
        deadline.observe(new Frame("phase-00000002-VERIFY", 104), BUDGET);
        assertEquals("VERIFY", deadline.phase()); assertFalse(deadline.expired(BUDGET));
        now.set(10); assertTrue(deadline.expired(BUDGET));
    }

    @Test void aLateSetupCompletionCannotBeHiddenByLaterMarkers() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        deadline.observe(new Frame("phase-00000001-SETUP", 100), BUDGET);
        deadline.observe(new Frame("phase-00000002-VERIFY", 105), BUDGET);
        deadline.observe(new Frame("phase-00000003-DONE", 106), BUDGET);
        assertTrue(deadline.expired(BUDGET)); assertEquals("SETUP", deadline.phase());
    }

    @Test void discoveryAndTheEntryIntoExecutionShareTheExistingVerificationWindow() {
        var now = new AtomicLong(); var deadline = new BrowserPhaseDeadline(now::get);
        deadline.observe("phase-00000001-VERIFY");
        now.set(2); deadline.observe("phase-00000002-DISCOVERY");
        assertEquals("DISCOVERY", deadline.phase());
        now.set(4); deadline.observe("phase-00000003-VERIFY");
        assertFalse(deadline.expired(BUDGET));
        now.set(5); assertTrue(deadline.expired(BUDGET));
    }
}

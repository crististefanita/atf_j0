package atf.reporting.browser.contract;

import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import atf.reporting.browser.process.BrowserPhaseDeadline;
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
}

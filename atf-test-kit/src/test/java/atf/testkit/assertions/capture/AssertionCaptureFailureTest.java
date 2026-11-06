package atf.testkit.assertions.capture;

import atf.testkit.AtfUnitTest;
import atf.testkit.assertions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AssertionCaptureFailureTest {
    @Test void caughtProjectionFailureCannotTurnAnAuditedBodyIntoPass() {
        var capture = new IllegalStateException("Capture refused");
        var audit = audit(capture); audit.start(ComponentAssertionAudit.Phase.TEST_BODY);
        var rows = audit.recorder(ComponentAssertionAudit.Phase.TEST_BODY);
        rows.equals("good", 1, 1);
        assertSame(capture, assertThrows(IllegalStateException.class, () -> rows.equals("bad", 1, 1)));
        assertFailure(audit.finish(null), capture);
    }

    @Test void originalComparisonFailureSurvivesACaughtEvidenceFailure() {
        var capture = new IllegalStateException("Capture refused");
        var audit = audit(capture); audit.start(ComponentAssertionAudit.Phase.TEST_BODY);
        var rows = audit.recorder(ComponentAssertionAudit.Phase.TEST_BODY);
        rows.equals("good", 1, 1);
        var original = assertThrows(org.opentest4j.AssertionFailedError.class, () -> rows.equals("bad", 1, 2));
        assertSame(capture, original.getSuppressed()[0]);
        assertFailure(audit.finish(null), original);
    }

    @Test void detachedProjectionFailureRemainsVisibleInTheBoundAudit() {
        var capture = new IllegalStateException("Capture refused");
        var audit = audit(null); audit.start(ComponentAssertionAudit.Phase.TEST_BODY);
        var bound = audit.recorder(ComponentAssertionAudit.Phase.TEST_BODY);
        var detached = new RecordedAssertions(new AssertionValueDisplay(240, (field, value) -> value, ignored -> { throw capture; }));
        try (var scope = AuditedAssertionScope.open(getClass().getName(), "detachedProjectionFailureRemainsVisibleInTheBoundAudit", bound)) {
            bound.equals("good", 1, 1);
            assertSame(capture, assertThrows(IllegalStateException.class, () -> detached.equals("bad", 1, 1)));
        }
        assertFailure(audit.finish(null), capture);
    }

    private static ComponentAssertionAudit audit(RuntimeException capture) {
        return new ComponentAssertionAudit(new AssertionValueDisplay(240, (field, value) -> value, projection -> {
            if (capture != null && projection.name().equals("bad")) throw capture;
        }));
    }

    private static void assertFailure(ComponentAssertionAudit.Result result, Throwable original) {
        assertEquals(ComponentAssertionAudit.Outcome.FAIL, result.outcome());
        assertSame(original, result.failure());
        assertEquals(ComponentAssertionAudit.Outcome.FAIL, result.phases().getFirst().outcome());
    }
}

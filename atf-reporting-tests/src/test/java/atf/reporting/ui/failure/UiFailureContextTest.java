package atf.reporting.ui.failure;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.*;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UiFailureContextTest {
    private static AtfUiActionRecord action(int sequence, AtfReportStatus status, AtfUiEvidenceSource source) {
        return new AtfUiActionRecord(sequence, "Click", "#submit_search", "navigation completes", "original observation",
                1234, status, source);
    }
    @Test void longExceptionCannotDisplaceTheCapturedActionAndOriginalEvidenceIsUnmodified() {
        var failed = action(22, AtfReportStatus.FAIL, AtfUiEvidenceSource.ACTION);
        var failure = new IllegalStateException("stack prefix ".repeat(100));
        var context = new UiFailureContext(failure, List.of(action(21, AtfReportStatus.PASS, AtfUiEvidenceSource.ACTION), failed));
        assertEquals("IllegalStateException; first failed evidence: Click #22 -> #submit_search; elapsed=1234 ms; expected=navigation completes", context.summary());
        assertEquals(context.summary(), context.projectedActions().get(1).observed());
        assertEquals("original observation", failed.observed());
        assertEquals("stack prefix ".repeat(100), failure.getMessage());
        assertEquals(0, failure.getSuppressed().length);
        assertEquals(2, context.diagnostics().size());
    }
    @Test void causeAndSuppressedGraphIsFiniteAndPreservesRelationshipsAndOriginalIdentity() {
        var primary = new IllegalStateException("primary");
        var cause = new IllegalArgumentException("cause");
        primary.initCause(cause); cause.initCause(primary);
        var cleanup = new UnsupportedOperationException("cleanup");
        primary.addSuppressed(cleanup); cleanup.addSuppressed(cause);
        var diagnostics = new UiFailureContext(primary, List.of()).diagnostics();
        assertEquals(5, diagnostics.size());
        assertEquals("Primary exception", diagnostics.get(0).relation());
        assertEquals("Caused by diagnostic #1", diagnostics.get(1).relation());
        assertEquals("Suppressed by diagnostic #1", diagnostics.get(2).relation());
        assertTrue(diagnostics.get(3).text().startsWith("Reference to diagnostic #"));
        assertSame(primary, cause.getCause()); assertSame(cleanup, primary.getSuppressed()[0]);
    }
    @Test void passingActionsCannotBeInventedAsTheCauseOfAnIndependentFailure() {
        var passed = action(1, AtfReportStatus.PASS, AtfUiEvidenceSource.ACTION);
        var context = new UiFailureContext(new IllegalStateException("independent failure"), List.of(passed));
        assertTrue(context.summary().contains("failed UI action not recorded"));
        assertFalse(context.summary().contains("Click"));
        assertSame(passed, context.projectedActions().getFirst());
    }
    @Test void absentThrowableAndFailedObservationRemainExplicitWithoutInventingNetworkFacts() {
        var context = new UiFailureContext(null, List.of(action(4, AtfReportStatus.FAIL, AtfUiEvidenceSource.OBSERVE)));
        assertTrue(context.summary().startsWith("No exception supplied; failed UI observation:"));
        assertTrue(context.summary().contains("source=observation"));
        assertFalse(context.summary().contains("HTTP"));
        assertEquals("Recorded observe #4", context.diagnostics().getFirst().relation());
        assertThrows(IllegalArgumentException.class, () -> new UiFailureContext(null, List.of()));
        assertTrue(new UiFailureContext(null, List.of(action(1, AtfReportStatus.FAIL, AtfUiEvidenceSource.ACTION)))
                .summary().startsWith("No exception supplied; failed UI action: Click"));
    }
    @Test void firstSequenceWinsWithoutAttributingOneThrowableToEveryFailedAction() {
        var later = action(9, AtfReportStatus.FAIL, AtfUiEvidenceSource.ACTION);
        var earlier = action(3, AtfReportStatus.FAIL, AtfUiEvidenceSource.ACTION);
        var context = new UiFailureContext(new IllegalArgumentException(), List.of(later, earlier));
        assertTrue(context.summary().contains("#3 ->"));
        assertSame(later, context.projectedActions().getFirst());
        assertTrue(context.diagnostics().getFirst().text().contains("message not recorded"));
    }
}

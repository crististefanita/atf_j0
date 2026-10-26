package atf.api.reporting.ui.failure;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.ui.failure.UiCheckpointFailure;
import atf.api.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class UiCheckpointFailureTest {
    private static AtfAssertion failed() { return new AtfAssertion("notice", "visible", "absent", false); }
    private static Set<AtfAssertion> identitySet(AtfAssertion... assertions) {
        Set<AtfAssertion> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(List.of(assertions));
        return result;
    }

    @Test void recognizesOnlyTheExactReportedInstancesIncludingWrappedPropagation() {
        var original = failed();
        var error = new UiCheckpointFailure("same message", List.of(original));
        var reported = identitySet(original);
        assertTrue(UiCheckpointFailure.isReported(error, reported));
        assertTrue(UiCheckpointFailure.isReported(new RuntimeException(error), reported));
        assertFalse(UiCheckpointFailure.isReported(new AssertionError("same message"), reported));
        assertFalse(UiCheckpointFailure.isReported(new UiCheckpointFailure("same message", List.of(failed())), reported));
        assertFalse(UiCheckpointFailure.isReported(error, identitySet()));
    }

    @Test void requiresEveryFailedAssertionAndDoesNotBorrowPassingOnes() {
        var first = failed();
        var second = failed();
        var error = new UiCheckpointFailure("two causes", List.of(first, second,
                new AtfAssertion("passing", "ok", "ok", true)));
        assertFalse(UiCheckpointFailure.isReported(error, identitySet(first)));
        assertTrue(UiCheckpointFailure.isReported(error, identitySet(first, second)));
        assertThrows(IllegalArgumentException.class, () -> new UiCheckpointFailure("no failure", List.of()));
    }

    @Test void retainsIndependentSuppressedFailuresAndTerminatesCyclicCauseChains() {
        var original = failed();
        var error = new UiCheckpointFailure("checkpoint", List.of(original));
        var wrapper = new RuntimeException(error);
        wrapper.addSuppressed(new IllegalStateException("cleanup"));
        assertFalse(UiCheckpointFailure.isReported(wrapper, identitySet(original)));
        error.addSuppressed(new IllegalStateException("capture"));
        assertFalse(UiCheckpointFailure.isReported(error, identitySet(original)));
        var caused = new UiCheckpointFailure("checkpoint", List.of(original));
        caused.initCause(new IllegalStateException("independent cause"));
        assertFalse(UiCheckpointFailure.isReported(caused, identitySet(original)));
        var cycle = new RuntimeException();
        var other = new RuntimeException(cycle);
        cycle.initCause(other);
        assertFalse(UiCheckpointFailure.isReported(cycle, identitySet(original)));
    }
}

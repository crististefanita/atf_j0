package atf.ui.config;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class UiRetryPolicyTest {
    @Test
    void defaultsAreFailClosedAndDoNotReplayAnyAction() {
        UiRetryPolicy policy = UiRetryPolicy.none();

        assertFalse(policy.enabled());
        assertFalse(policy.allows("Wait for visible", 1));
    }

    @Test
    void allowsOnlyExplicitReadOnlyOperationsWithinTheAttemptBudget() {
        UiRetryPolicy policy = new UiRetryPolicy(3, Duration.ZERO,
                EnumSet.of(UiRetryOperation.WAIT_FOR_VISIBLE), "report stabilization");

        assertTrue(policy.allows("Wait for visible", 1));
        assertTrue(policy.allows("Wait for visible", 2));
        assertFalse(policy.allows("Wait for visible", 3));
        assertFalse(policy.allows("Click", 1));
    }

    @Test
    void rejectsNegativeBackoffAndUnknownOperationNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new UiRetryPolicy(2, Duration.ofSeconds(-1),
                        EnumSet.of(UiRetryOperation.NAVIGATE), "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> UiRetryOperation.from("Click"));
    }
}

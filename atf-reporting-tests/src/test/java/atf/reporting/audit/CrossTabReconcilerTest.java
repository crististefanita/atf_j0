package atf.reporting.audit;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class CrossTabReconcilerTest {
    private static final String USERS_OWNER = "UsersApiTests";
    private static final String POSTS_OWNER = "PostsApiTests";
    private final CrossTabReconciler reconciler = new CrossTabReconciler();

    @Test
    void acceptsEquivalentOwnerSetsAndProjectedCounts() {
        ValidationResult result = reconciler.reconcile(
                List.of(USERS_OWNER, POSTS_OWNER),
                List.of(POSTS_OWNER, USERS_OWNER), 2, 4);

        assertTrue(result.valid(), result::failureSummary);
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void reportsEveryProjectionDrift() {
        ValidationResult result = reconciler.reconcile(
                List.of(USERS_OWNER, POSTS_OWNER),
                List.of(USERS_OWNER, "UnexpectedApiTests"), 3, 0);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(value -> value.startsWith("Owners:")));
        assertTrue(result.violations().stream().anyMatch(value -> value.startsWith("Overview classes:")));
        assertTrue(result.violations().stream().anyMatch(value -> value.startsWith("Category links:")));
    }
}

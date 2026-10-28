package atf.ui.config.recovery;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UiRecoveryConfigurationTest {
    private static final Set<String> READS = Set.of("catalog-readiness");
    @Test void absentConfigurationIsOffAndDeclarationCannotEnableIt() {
        var off = UiRecoveryConfigurationParser.parse(Map.of(), READS);
        assertFalse(off.enabled()); assertEquals(1, off.maxAttempts());
        assertFalse(off.restrict(true, "@AtfUiRecovery READ_ONLY").enabled());
        assertFalse(off.captureBackendSignals());
        assertEquals("YAML", off.restrict(true, "YAML").source());
    }
    @Test void validPolicyIsImmutableAndAnAnnotationOnlyRestrictsItsExistingCeilings() {
        var operations = new ArrayList<>(List.of("catalog-readiness"));
        var policy = UiRecoveryConfigurationParser.parse(Map.of("enabled", true, "operations", operations,
                "maxAttempts", 3, "totalTimeout", "PT2S", "backoff", "PT0.01S", "captureBackendSignals", true), READS);
        operations.clear(); assertEquals(READS, policy.operations());
        var disabled = policy.restrict(false, "method disabled");
        assertFalse(disabled.enabled()); assertEquals(3, disabled.maxAttempts());
        assertEquals(Duration.ofSeconds(2), disabled.totalTimeout());
        assertEquals(READS, disabled.operations());
        assertThrows(UnsupportedOperationException.class, () -> policy.operations().add("cart"));
    }
    @Test void unknownKeysProfilesOperationsAndMutationsAreRejectedEvenWhenDisabled() {
        for (var values : List.of(Map.of("profile", "all"), Map.of("maxAttempt", 2),
                Map.of("operations", List.of("cart-add")), Map.of("operations", List.of("catalog-readiness", "catalog-readiness")))) {
            assertThrows(IllegalArgumentException.class, () -> UiRecoveryConfigurationParser.parse(values, READS));
        }
    }
    @Test void malformedAndInconsistentBudgetsNeverBecomeDefaults() {
        for (var values : List.of(Map.of("enabled", "false"), Map.of("maxAttempts", 0), Map.of("maxAttempts", 1.5),
                Map.of("operations", "catalog-readiness"), Map.of("totalTimeout", "PT0S"), Map.of("backoff", "PT-1S"),
                Map.of("totalTimeout", "PT1S", "backoff", "PT1S"), Map.of("totalTimeout", 5), Map.of("enabled", true))) {
            assertThrows(RuntimeException.class, () -> UiRecoveryConfigurationParser.parse(values, READS));
        }
    }
}

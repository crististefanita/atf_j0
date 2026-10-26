package atf.debugui;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class DebugUiMemoryPolicyTest {
    @Test
    void acceptsAHeapWithinTheParallelSessionLimit() {
        assertDoesNotThrow(() -> DebugUiMemoryPolicy.enforce(
                128L * 1024L * 1024L, DebugUiMemoryPolicy.DEFAULT_MAX_HEAP_BYTES));
    }

    @Test
    void rejectsAnUnboundedHeapBeforeStartingTheServer() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DebugUiMemoryPolicy.enforce(
                        8L * 1024L * 1024L * 1024L, DebugUiMemoryPolicy.DEFAULT_MAX_HEAP_BYTES));

        assertTrue(failure.getMessage().contains("-Xmx128m"));
    }
}

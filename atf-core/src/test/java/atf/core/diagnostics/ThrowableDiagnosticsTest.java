package atf.core.diagnostics;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ThrowableDiagnosticsTest {
    @Test
    void keepsTypeAndCompactsTheCauseMessage() {
        assertEquals("unavailable: IllegalStateException — selector did not resolve for the configured product",
                ThrowableDiagnostics.unavailable(new IllegalStateException(
                        "selector did not resolve\nfor the configured product")));
    }

    @Test
    void boundsDiagnosticsAtTheConfiguredLength() {
        String diagnostic = ThrowableDiagnostics.unavailable(
                new IllegalArgumentException("x".repeat(500)), 80);

        assertEquals(80, diagnostic.length());
        assertTrue(diagnostic.endsWith("…"));
        assertThrows(IllegalArgumentException.class,
                () -> ThrowableDiagnostics.unavailable(new RuntimeException(), 1));
    }
}

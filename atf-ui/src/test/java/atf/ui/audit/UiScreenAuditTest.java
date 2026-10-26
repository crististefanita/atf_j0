package atf.ui.audit;

import atf.core.ui.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.UiScreenValidation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiScreenAuditTest {
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void requiresCapturedPngRatherThanABase64StringOrCaptureFlag() {
        UiScreenAudit.requireScreenshot(new UiScreenValidation.Evidence("final", PNG, true));
        for (var evidence : java.util.List.of(
                new UiScreenValidation.Evidence("final", "", false),
                new UiScreenValidation.Evidence("final", "bm90LWEtUE5H", true),
                new UiScreenValidation.Evidence("final", PNG, false))) {
            assertThrows(IllegalStateException.class, () -> UiScreenAudit.requireScreenshot(evidence));
        }
    }

    @Test
    void recordsTheCallerVerdictCountAndUrlWithoutEvaluatingAssertions() {
        var actions = new PlaywrightActionSequence();
        UiScreenAudit.record(actions, "screen", " https://example.test/final ", 2,
                new UiScreenValidation.Evidence("screen", "", false), false);
        var row = actions.snapshot().getFirst();
        assertEquals(AtfUiEvidenceSource.OBSERVE, row.source());
        assertEquals("Audit screen contract", row.operation());
        assertTrue(row.observed().contains("status=FAIL; assertions=2"));
        assertTrue(row.observed().contains("url=https://example.test/final"));
    }

    @Test
    void drainsEarlierActionsThenExactlyOneTerminalAudit() {
        var actions = priorActions();
        var evidence = UiScreenAudit.drain(actions, "final", "/confirmation", 1,
                new UiScreenValidation.Evidence("final", PNG, true), true, true);
        assertEquals(2, evidence.size());
        assertEquals("Prior UI evidence", evidence.getFirst().operation());
        assertEquals("Audit screen contract", evidence.getLast().operation());
        assertTrue(evidence.getLast().observed().contains("screenshot=valid PNG"));
        assertTrue(actions.snapshot().isEmpty());
    }

    @Test
    void disabledOrMalformedCaptureRemainsExplicitlyNonVisual() {
        for (String payload : java.util.List.of("", "not-a-png")) {
            var actions = priorActions();
            var evidence = UiScreenAudit.drain(actions, "final", "/confirmation", 1,
                    new UiScreenValidation.Evidence("final", payload, true), true, false);
            assertEquals(2, evidence.size());
            assertTrue(evidence.getLast().observed().contains("screenshot=not captured"));
            assertFalse(evidence.getLast().observed().contains("valid PNG"));
            assertTrue(actions.snapshot().isEmpty());
        }
    }

    @Test
    void failedRequiredCaptureDoesNotConsumeEarlierEvidence() {
        var actions = priorActions();
        assertThrows(IllegalStateException.class, () -> UiScreenAudit.drain(actions, "final", null, 1,
                new UiScreenValidation.Evidence("final", "", false), true, true));
        assertEquals(1, actions.snapshot().size());
        UiScreenAudit.record(actions, "final", null, 0,
                new UiScreenValidation.Evidence("final", "", false), true);
        assertTrue(actions.snapshot().getLast().observed().contains("url=unavailable"));
        assertThrows(IllegalArgumentException.class, () -> UiScreenAudit.record(actions, "final", "", -1,
                new UiScreenValidation.Evidence("final", "", false), true));
    }

    private static PlaywrightActionSequence priorActions() {
        var actions = new PlaywrightActionSequence();
        actions.observation("Prior UI evidence", "checkpoint", "ready", "ready");
        return actions;
    }
}

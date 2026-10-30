package atf.ui.playwright.recovery;

import atf.core.reporting.contract.AtfReportStatus;
import atf.core.ui.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import atf.ui.config.recovery.UiRecoveryConfiguration;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.recovery.UiRecoveryOperation;
import atf.ui.recovery.UiRecoveryTrace;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiRecoveryRecordingComponentTest {
    private static final URI ORIGIN = URI.create("https://recording.test");
    private static Page readOnlyPage() {
        return (Page) Proxy.newProxyInstance(Page.class.getClassLoader(), new Class<?>[]{Page.class}, (proxy, method, args) -> switch(method.getName()) {
            case "url" -> ORIGIN + "/home";
            case "mainFrame" -> null;
            case "isClosed" -> false;
            default -> throw new AssertionError("No browser dispatch or listener acquisition is allowed for disabled capture: " + method.getName());
        });
    }
    @Test void sharedRecordingPreservesPolicySourceAndAnObservationOnlyVerdict() {
        var actions = new PlaywrightActionSequence();
        var policy = UiRecoveryConfiguration.disabled().restrict(false, "class probe");
        var operation = new UiRecoveryOperation("catalog", UiRecoveryOperation.Safety.READ_ONLY, ORIGIN, ORIGIN.resolve("/read"), "GET");
        try (var recovery = UiBackendRecovery.recording(readOnlyPage(), policy, List.of(operation), actions);
             var scope = recovery.begin("catalog")) {
            assertEquals(UiRecoveryTrace.Decision.INITIAL_READY, scope.await(() -> UiRecoveryTrace.Dom.READY).decision());
        }
        assertEquals(1, actions.snapshot().size()); var row = actions.snapshot().getFirst();
        assertEquals("BE observation #1", row.operation()); assertEquals("catalog", row.target());
        assertEquals("profile=read-only; maxAttempts=1; totalTimeout=PT5S; source=YAML; class probe", row.expected());
        assertTrue(row.observed().startsWith("INITIAL_READY; READY; backend signal not captured"));
        assertEquals(AtfReportStatus.INFO, row.status()); assertEquals(AtfUiEvidenceSource.OBSERVE, row.source());
    }
    @Test void aProbeFailureIsRecordedWithoutReplacingItsOriginalCauseOrPublishingPass() {
        var actions = new PlaywrightActionSequence(); var original = new AssertionError("controlled probe failure");
        var operation = new UiRecoveryOperation("catalog", UiRecoveryOperation.Safety.READ_ONLY, ORIGIN, ORIGIN.resolve("/read"), "GET");
        try (var recovery = UiBackendRecovery.recording(readOnlyPage(), UiRecoveryConfiguration.disabled(), List.of(operation), actions);
             var scope = recovery.begin("catalog")) {
            assertSame(original, assertThrows(AssertionError.class, () -> scope.await(() -> { throw original; })));
        }
        assertEquals(1, actions.snapshot().size());
        assertTrue(actions.snapshot().getFirst().observed().startsWith("REFUSED; PROBE_FAILURE;"));
        assertEquals(AtfReportStatus.INFO, actions.snapshot().getFirst().status());
        assertEquals(0, original.getSuppressed().length);
    }
}

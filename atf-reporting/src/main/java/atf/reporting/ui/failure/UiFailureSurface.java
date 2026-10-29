package atf.reporting.ui.failure;

import java.util.List;

/** Failure evidence channels that must remain independently actionable. */
public enum UiFailureSurface {
    SETUP(
            "Setup failure — input availability",
            List.of("Setup evidence", "Setup check", "Setup check failed")),
    PLAYWRIGHT_ACTION(
            "Playwright action failure — timeout",
            List.of("Failure evidence", "TimeoutError", "Wait for visible #1",
                    "button[data-test='login-submit']", "elapsed=20000 ms",
                    "expected=login submit becomes visible")),
    AUDIT_OBSERVATION(
            "Audit observation failure — rendered state",
            List.of("UI validation", "Audit observation", "Audit observation failed")),
    ASSERTION(
            "Assertion failure — expected versus actual",
            List.of("UI validation", "Status badge", "Status badge expected"));

    private final String scenarioTitle;
    private final List<String> failsDetailMarkers;

    UiFailureSurface(String scenarioTitle, List<String> failsDetailMarkers) {
        this.scenarioTitle = scenarioTitle;
        this.failsDetailMarkers = List.copyOf(failsDetailMarkers);
    }

    public String scenarioTitle() {
        return scenarioTitle;
    }

    public List<String> failsDetailMarkers() {
        return failsDetailMarkers;
    }
}

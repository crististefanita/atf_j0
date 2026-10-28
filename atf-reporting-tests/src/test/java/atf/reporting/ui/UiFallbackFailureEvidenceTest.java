package atf.reporting.ui;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class UiFallbackFailureEvidenceTest {
    @Test
    void keepsTimeoutCauseActionableRedactedAndScopedToItsScenario(@TempDir Path directory)
            throws Exception {
        Path report = directory.resolve("ui-fallback-failure.html");
        String failedScenarioId;
        try (var session = new HtmlReportSession(report)) {
            var failed = session.startScenario("UiTest", "UI fallback timeout",
                    List.of("ui", "failure"));
            failedScenarioId = failed.id();
            var phase = failed.startPhase("TEST BODY");
            new UiEvidencePublisher(phase).publishFailure("login action timed out",
                    "button[data-test='login-submit']", List.of(
                            new AtfUiActionRecord(1, "Wait for visible",
                                    "button[data-test='login-submit']",
                                    "login submit becomes visible", "Timeout 20000ms exceeded",
                                    20_000, AtfReportStatus.FAIL)),
                    new TimeoutError("Timeout 20000ms exceeded waiting for "
                            + "button[data-test='login-submit']; password=raw-ui-password; "
                            + "Authorization: Bearer raw-ui-token"), null);
            phase.complete(AtfReportStatus.FAIL);
            failed.complete(AtfReportStatus.FAIL);

            var sibling = session.startScenario("UiTest", "Unrelated passing sibling",
                    List.of("ui", "pass"));
            sibling.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            sibling.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int failureCause = html.indexOf("TimeoutError: Timeout 20000ms exceeded");
        String failedScenario = articleContaining(html, "TimeoutError: Timeout 20000ms exceeded", 0);
        int failedScenarioEnd = html.indexOf("</article>", failureCause);
        String siblingScenario = articleContaining(html, "Unrelated passing sibling", failedScenarioEnd + 1);
        int failsTab = html.indexOf("id=\"fails\"");
        assertAll(
                () -> assertTrue(failureCause >= 0, "the original timeout class and duration are missing"),
                () -> assertTrue(failedScenario.contains("login-submit"),
                        "the failing locator must remain actionable"),
                () -> assertTrue(failedScenario.contains("TimeoutError: Timeout 20000ms exceeded"),
                        "the diagnostic must remain under the failing scenario"),
                () -> assertTrue(html.contains("***REDACTED***"),
                        "sensitive values in the exception message must be redacted"),
                () -> assertFalse(html.contains("raw-ui-password")),
                () -> assertFalse(html.contains("raw-ui-token")),
                () -> assertFalse(html.contains("<details class=\"step-detail request-evidence\""),
                        "fallback UI failure evidence must not create an HTTP request disclosure"),
                () -> assertFalse(html.contains("<details class=\"step-detail response-evidence\""),
                        "fallback UI failure evidence must not create an HTTP response disclosure"),
                () -> assertTrue(failsTab >= 0 && html.substring(failsTab)
                                .contains("href=\"#" + failedScenarioId + "\""),
                        "fallback Fails entry must link back to the owning scenario"),
                () -> assertTrue(failsTab >= 0 && html.substring(failsTab).contains("Wait for visible #1")
                                && html.substring(failsTab).contains("login-submit")
                                && html.substring(failsTab).contains("elapsed=20000 ms"),
                        "fallback Fails projection must lead with captured action context; original timeout remains in diagnostics"),
                () -> assertTrue(siblingScenario.contains("Unrelated passing sibling"),
                        "the second scenario must be independently rendered"),
                () -> assertFalse(siblingScenario.contains("TimeoutError:"),
                        "failure diagnostics must not leak into a sibling scenario"));
    }

    private static String articleContaining(String html, String marker, int fromIndex) {
        int markerIndex = html.indexOf(marker, Math.max(0, fromIndex));
        if (markerIndex < 0) return "";
        int start = html.lastIndexOf("<article", markerIndex);
        int end = html.indexOf("</article>", markerIndex);
        return start < 0 || end < start ? "" : html.substring(start, end);
    }

    private static final class TimeoutError extends RuntimeException {
        private TimeoutError(String message) { super(message); }
    }
}

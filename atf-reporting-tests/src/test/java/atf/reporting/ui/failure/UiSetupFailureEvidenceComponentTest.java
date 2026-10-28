package atf.reporting.ui.failure;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class UiSetupFailureEvidenceComponentTest {
    private static final String REPORT_FILE = "ui-setup-failure-component.html";
    private static final String FIXTURE_SECRET = "private-password-value";
    private static final String LOCATOR = "[data-test='login-submit']";

    @Test
    void setupFailureReportShowsMockedPlaywrightTimeoutAndRedactsCredentialText() throws Exception {
        Path outputDirectory = Files.createTempDirectory("ui-setup-failure-report-");
        Path reportPath;
        try (var report = UiExtentReportSession.openConfigured(
                UiExtentReportOptions.builder(outputDirectory)
                        .fileName(REPORT_FILE)
                        .console(AtfConsoleSink.noOp())
                        .build())) {
            reportPath = report.reportPath();
            var scenario = report.startScenario("UiSetupContract",
                    "atf.tests.fixture", "authenticated purchase setup", List.of("ui", "e2e"));
            var setup = scenario.startPhase("SETUP");

            setup.failureEvidence("account setup failure", "https://ui.example.test/login",
                    List.of(new AtfUiActionRecord(1, "Wait for visible", LOCATOR,
                            "state=Visible", "TimeoutError: Timeout 20000ms exceeded", 20_000,
                            AtfReportStatus.FAIL)),
                    new TimeoutError("Timeout 20000ms exceeded. waiting for locator(\""
                            + LOCATOR + "\"); password=" + FIXTURE_SECRET), "");

            setup.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(reportPath);
        assertTrue(html.contains("SETUP"), "the failure remains attached to the setup phase");
        assertTrue(html.contains("Failure evidence"), "the report exposes the structured failure section");
        assertTrue(html.contains("TimeoutError"), "the exception type is visible");
        assertTrue(html.contains("Timeout 20000ms exceeded"), "the timeout reason is visible");
        assertTrue(html.contains("login-submit"), "the awaited locator is visible for diagnosis");
        assertTrue(html.contains("Wait for visible"), "the action that actually failed is retained");
        assertTrue(html.contains("20,000 ms") || html.contains("20000 ms"),
                "the captured action duration remains auditable");
        assertTrue(html.contains("***REDACTED***"), "credential-like data is redacted");
        assertFalse(html.contains(FIXTURE_SECRET), "raw credential text never reaches the report");
        assertTrue(html.contains("FAIL"), "the report keeps the actual failed outcome");
    }

    @Test
    void longNativeStackCannotHideTheFailedActionFromTheBoundedCause() throws Exception {
        Path directory = Files.createTempDirectory("ui-failure-context-");
        Path file;
        try (var report = UiExtentReportSession.openConfigured(directory, "context.html", AtfConsoleSink.noOp())) {
            file = report.reportPath();
            var scenario = report.startScenario("ContextFixture", "fixture", "failed action", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            phase.failureEvidence("controlled failure", "#submit", List.of(
                    new AtfUiActionRecord(1, "Click", "#submit", "result ready", "timeout", 20, AtfReportStatus.FAIL)),
                    new IllegalStateException("native error\n" + "stack-noise-".repeat(150)), "");
            phase.complete(AtfReportStatus.FAIL); scenario.complete(AtfReportStatus.FAIL);
        }
        String html = Files.readString(file);
        assertTrue(html.contains("Click #1 -&gt; #submit; elapsed=20 ms; expected=result ready"));
        assertTrue(html.contains("IllegalStateException: native error"), "original native message remains in a separate diagnostic");
        assertTrue(html.contains("Primary exception"));
        assertFalse(html.contains("stack-noise-".repeat(150)));
        assertTrue(html.contains("ContextFixture"));
    }

    private static final class TimeoutError extends RuntimeException {
        private TimeoutError(String message) { super(message); }
    }
}

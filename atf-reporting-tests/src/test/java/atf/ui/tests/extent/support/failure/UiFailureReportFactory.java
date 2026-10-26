package atf.ui.tests.extent.support.failure;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.config.environment.UiReportEnvironmentSnapshotFactory;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;

/** Creates a real Extent artifact containing one intentional failure per UI channel. */
public final class UiFailureReportFactory {
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
    private static final String TEST_CLASS = "UiFailureReportFixture";
    private static final String TEST_PACKAGE = "atf.ui.tests.extent.support.failure";
    private static final List<String> TAGS = List.of("ui", "extent", "failure-contract", "fixture");

    private UiFailureReportFactory() { }

    public static Path create(UiConfiguration configuration) {
        String fileName = configuration.reserveTimestampedReportPath(
                "ui-failure-contract-input", Clock.systemDefaultZone()).getFileName().toString();
        UiExtentReportSession report = UiExtentReportSession.openConfiguredResolved(
                configuration.reportDirectory(), fileName, AtfConsoleSink.noOp(),
                UiReportEnvironmentSnapshotFactory.fixture(
                        "UI failure contract", "failure evidence channels"));
        try {
            setupFailure(report);
            actionFailure(report);
            observationFailure(report);
            assertionFailure(report);
        } finally {
            report.close();
        }
        return report.reportPath();
    }

    private static void setupFailure(UiExtentReportSession report) {
        var scenario = report.startScenario(TEST_CLASS, TEST_PACKAGE,
                "Setup failure — input availability", TAGS);
        var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
        setup.setup(GIVEN, "the configured report input is available",
                "report input", List.of(new AtfAssertion("Input report", "available", "missing", false)),
                AtfReportStatus.FAIL);
        setup.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private static void actionFailure(UiExtentReportSession report) {
        var scenario = report.startScenario(TEST_CLASS, TEST_PACKAGE,
                "Playwright action failure — timeout", TAGS);
        passSetup(scenario);
        var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        body.failureEvidence("Playwright action timed out",
                "button[data-test='login-submit']",
                List.of(new AtfUiActionRecord(1, "Wait for visible",
                        "button[data-test='login-submit']",
                        "login submit becomes visible", "Timeout 20000ms exceeded", 20_000,
                        AtfReportStatus.FAIL, AtfUiEvidenceSource.ACTION)),
                new TimeoutError("Timeout 20000ms exceeded waiting for "
                        + "button[data-test='login-submit']; password=raw-ui-password; "
                        + "Authorization: Bearer raw-ui-token"), ONE_PIXEL_PNG);
        body.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private static void observationFailure(UiExtentReportSession report) {
        var scenario = report.startScenario(TEST_CLASS, TEST_PACKAGE,
                "Audit observation failure — rendered state", TAGS);
        passSetup(scenario);
        var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        body.screenValidation("Audit observed an invalid visual state", "#failure-audit",
                ONE_PIXEL_PNG, List.of(), List.of(new AtfUiActionRecord(
                        1, "Audit observation", "#failure-audit", "badge is visible",
                        "badge is missing", 0, AtfReportStatus.FAIL, AtfUiEvidenceSource.OBSERVE)),
                AtfReportStatus.FAIL);
        body.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private static void assertionFailure(UiExtentReportSession report) {
        var scenario = report.startScenario(TEST_CLASS, TEST_PACKAGE,
                "Assertion failure — expected versus actual", TAGS);
        passSetup(scenario);
        var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
        body.screenValidation("Assertion contract is not satisfied", "#failure-assertion",
                ONE_PIXEL_PNG, List.of(new AtfAssertion("Status badge", "Fail", "Pass", false)),
                List.of(), AtfReportStatus.FAIL);
        body.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);
    }

    private static void passSetup(UiExtentReportSession.UiScenario scenario) {
        var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
        setup.setup(GIVEN, "the failure fixture is available", "fixture",
                List.of(new AtfAssertion("Fixture", "available", "available", true)),
                AtfReportStatus.PASS);
        setup.complete(AtfReportStatus.PASS);
    }

    private static final class TimeoutError extends RuntimeException {
        private TimeoutError(String message) { super(message); }
    }
}

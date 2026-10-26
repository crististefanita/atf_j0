package atf.ui.tests.extent.support.uireport.browser;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.config.environment.UiReportEnvironmentSnapshotFactory;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** Produces a compact, valid UI report input when no previous artifact is selected. */
public final class UiReportContractInputFactory {
    private static final String OWNER = "UiReportContractInput";
    private static final String PACKAGE = "atf.ui.tests.extent.support.uireport.browser";
    private static final String SCREENSHOT_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    private UiReportContractInputFactory() { }

    public static Path create(UiConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        String fileName = configuration.reserveTimestampedReportPath(
                "ui-contract-input", Clock.systemDefaultZone()).getFileName().toString();
        UiExtentReportOptions options = UiExtentReportOptions.builder(configuration.reportDirectory())
                .fileName(fileName)
                .console(AtfConsoleSink.noOp())
                .environment(UiReportEnvironmentSnapshotFactory.fixture(
                        "UI Extent contract", "default IntelliJ input fixture"))
                .build();
        try (UiExtentReportSession report = UiExtentReportSession.openConfigured(options)) {
            var scenario = report.startScenario(OWNER, PACKAGE,
                    "UI report contract evidence is available", List.of("ui", "extent", "contract"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.setup(AtfGherkinKeyword.GIVEN, "a valid UI report evidence fixture is published",
                    "generated UI report", List.of(new AtfAssertion(
                            "UI report fixture", "available", "available", true)), AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);

            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.uiValidation("The report exposes replayable UI evidence", "#test-view",
                    SCREENSHOT_PNG,
                    List.of(new AtfAssertion("Evidence surface", "visible", "visible", true)),
                    List.of(
                            new AtfUiActionRecord(1, "Open report", "#test-view",
                                    "Tests tab is selected", "Tests tab is selected", 1,
                                    AtfReportStatus.PASS, AtfUiEvidenceSource.ACTION),
                            new AtfUiActionRecord(2, "Audit observation", "#test-view",
                                    "screen evidence is present", "screen evidence is present", 0,
                                    AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            return report.reportPath();
        }
    }
}

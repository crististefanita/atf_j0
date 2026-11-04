package atf.ui.tests.extent.support.inspection.provenance;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Interleaved captured inputs; the report groups ACTION, OBSERVE and comparisons without inventing assertions. */
public final class UiFailureCatalogueFixture {
    public static final List<String> CAUSES = List.of("dispatch rejected", "captured dialog absent", "wrong captured total");
    private UiFailureCatalogueFixture() { }

    public static Path publish(boolean requestOwner) throws Exception {
        return publish(true, requestOwner);
    }

    public static Path publish(boolean fallback, boolean requestOwner) throws Exception {
        Path directory = Path.of(System.getProperty("atf.test.reportDirectory", System.getProperty("java.io.tmpdir")));
        Files.createDirectories(directory);
        Path report = Files.createTempDirectory(directory, "ui-failure-catalogue-")
                .resolve((fallback ? "fallback-" : "extent-") + requestOwner + ".html");
        var rows = List.of(
                new AtfUiActionRecord(3, "Inspect", "dialog=Receipt", "present", CAUSES.get(1), 5,
                        AtfReportStatus.FAIL, AtfUiEvidenceSource.OBSERVE),
                new AtfUiActionRecord(1, "Activate", "button=Start", "accepted", "accepted", 7, AtfReportStatus.PASS),
                new AtfUiActionRecord(2, "Activate", "button=Continue", "accepted", CAUSES.get(0), 11, AtfReportStatus.FAIL));
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(report) : new ExtentReportSession(report)) {
            var scenario = session.startScenario("UiCatalogue", "Mixed captured UI failures", List.of("component", "mock"));
            try (var phase = scenario.startPhase("TEST BODY")) {
                try (var request = phase.startRequest("Captured UI flow", "UI", "device://captured")) {
                    if (requestOwner) {
                        request.uiActions(rows);
                        request.screenshot("Whole captured display", ReportTestScreenshots.ONE_PIXEL_PNG);
                    }
                    try (var validation = request.startValidation("Captured UI checkpoint")) {
                        if (!requestOwner) {
                            validation.uiActions(rows);
                            validation.screenshot("Whole captured display", ReportTestScreenshots.ONE_PIXEL_PNG);
                        }
                        comparisons(validation);
                    }
                    request.complete(AtfReportStatus.FAIL);
                }
                phase.complete(AtfReportStatus.FAIL);
            }
            scenario.complete(AtfReportStatus.FAIL);
            var sibling = session.startScenario("UiCatalogue", "Unrelated passing sibling", List.of("component", "mock"));
            sibling.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            sibling.complete(AtfReportStatus.PASS);
        }
        return report;
    }

    private static void comparisons(AtfReportPorts.Validation validation) {
        validation.evidenceOwner(ReportEvidenceOwner.UI);
        validation.assertions(List.of(new AtfAssertion("Receipt total", "100", CAUSES.get(2), false),
                new AtfAssertion("Captured account", "guest", "guest", true)));
        validation.complete(AtfReportStatus.FAIL);
    }
}

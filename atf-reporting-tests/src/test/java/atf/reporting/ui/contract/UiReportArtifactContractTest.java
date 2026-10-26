package atf.reporting.ui.contract;
import atf.testkit.AtfUnitTest;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.markup.AtfReportLabels;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static atf.reporting.contract.AtfGherkinKeyword.GIVEN;

/** Component coverage for the same UI evidence surfaces exercised by E2E smoke. */
@AtfUnitTest
class UiReportArtifactContractTest {
    @Test
    void generatedUiArtifactPreservesTheFullDisplayContractWithoutARealBrowser() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-artifact-contract-");
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "ui-contract.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiReportComponentTests", "atf.reporting.ui",
                    "GIVEN the generated report is available WHEN the UI evidence is published THEN every display surface remains auditable",
                    List.of("ui", "component"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.setup(GIVEN, "the generated report is available",
                    "ui-contract.html", List.of(new AtfAssertion(
                            "Input report", "published HTML", "ui-contract.html", true)),
                    AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.uiValidation("report is visible", "#test-view",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Tests tab", "visible", "visible", true)),
                    List.of(
                            new AtfUiActionRecord(1, "Click", "#tests",
                                    "Tests tab opens", "opened", 4, AtfReportStatus.PASS),
                            new AtfUiActionRecord(2, "Get text", "h1",
                                    "report title is visible", AtfReportLabels.REPORT_TITLE, 2,
                                    AtfReportStatus.PASS),
                            new AtfUiActionRecord(3, "Observe", "#test-view",
                                    "Tests surface remains visible", "visible", 0,
                                    AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        var evaluation = UiReportArtifactContract.evaluate(Files.readString(report));

        assertTrue(evaluation.isCompliant(), () -> "UI artifact violations: "
                + evaluation.violations());
        assertTrue(evaluation.satisfies("setup evidence is separate"));
        assertTrue(evaluation.satisfies("UI validation sibling layout"));
        assertTrue(evaluation.satisfies("legacy validation title absent"));
        assertTrue(evaluation.satisfies("Playwright action table"));
        assertTrue(evaluation.satisfies("inline screenshot link"));
        assertTrue(evaluation.satisfies("long proposition wrapping"));
    }

    @Test
    void visualCheckpointsUseOnlyTheCanonicalUiValidationTitle() throws Exception {
        Path root = Files.createTempDirectory("atf-ui-validation-title-");
        Path report;
        try (var session = UiExtentReportSession.openConfigured(root, "ui-title.html",
                AtfConsoleSink.noOp())) {
            report = session.reportPath();
            var scenario = session.startScenario("UiValidationTitle", "atf.reporting.ui",
                    "visual assertion title contract", List.of("ui", "component"));
            var setup = scenario.startPhase(ReportUiContract.Phase.SETUP.displayName());
            setup.setup(GIVEN, "the report fixture is available", "ui-title.html",
                    List.of(new AtfAssertion("Report", "published", "published", true)),
                    AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);
            var body = scenario.startPhase("TEST BODY");
            body.uiValidation("page is ready", "#test-view",
                    ReportTestScreenshots.ONE_PIXEL_PNG,
                    List.of(new AtfAssertion("Page", "ready", "ready", true)),
                    List.of(
                            new AtfUiActionRecord(1, "Click", "#tests", "Tests tab opens",
                                    "opened", 4, AtfReportStatus.PASS),
                            new AtfUiActionRecord(2, "Get text", "h1", "report title is visible",
                                    AtfReportLabels.REPORT_TITLE, 2, AtfReportStatus.PASS),
                            new AtfUiActionRecord(3, "Observe", "#test-view",
                                    "Tests surface remains visible", "visible", 0,
                                    AtfReportStatus.PASS, AtfUiEvidenceSource.OBSERVE)),
                    AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertTrue(html.contains("<span>UI validation</span>"));
        assertFalse(html.contains("<span>Screen validation</span>"));
        var evaluation = UiReportArtifactContract.evaluate(html);
        assertTrue(evaluation.isCompliant(), () -> "UI artifact violations: "
                + evaluation.violations());
    }

    @Test
    void reportsMissingDisplaySurfacesAsActionableChecks() {
        var evaluation = UiReportArtifactContract.evaluate("<!doctype html><body></body>");

        assertFalse(evaluation.isCompliant());
        assertTrue(evaluation.violations().contains("four navigation tabs"));
        assertTrue(evaluation.violations().contains("inline plus glyph"));
    }
}

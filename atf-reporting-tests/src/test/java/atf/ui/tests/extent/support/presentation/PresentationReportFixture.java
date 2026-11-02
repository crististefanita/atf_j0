package atf.ui.tests.extent.support.presentation;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import java.nio.file.Path;
import java.util.List;

/** Authored renderer facts and mock comparisons; no application adapter or transport is required. */
public final class PresentationReportFixture {
    public static final String CLASS_NAME = "atf.fixture.reporting." + "CompleteConsumerClassName".repeat(7);
    public static final String TITLE = "Original scenario with a complete, deliberately long title " + "auditable words ".repeat(14);
    private PresentationReportFixture() { }

    public static Path publish(Path file, boolean fallback, boolean aggregate) {
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file, ReportingSettings.defaults())) {
            int count = aggregate ? 3 : 1;
            for (int index = 0; index < count; index++) {
                var status = List.of(AtfReportStatus.PASS, AtfReportStatus.FAIL, AtfReportStatus.SKIP).get(index);
                try (var scenario = session.startScenario(CLASS_NAME, TITLE + index, List.of("component", "mock"))) {
                    try (var phase = scenario.startPhase("TEST BODY")) {
                        if (status != AtfReportStatus.SKIP) {
                            try (var step = phase.startAssertionStep("Original comparisons")) {
                                step.assertions(List.of(new AtfAssertion("Final original comparison", "expected-" + index,
                                        index == 0 ? "expected-0" : "different-1", index == 0)));
                                step.complete(status);
                            }
                        }
                        phase.complete(status);
                    }
                    scenario.complete(status);
                }
            }
        }
        return file;
    }
}

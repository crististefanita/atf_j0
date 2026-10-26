package atf.ui.tests.extent.support.configurable;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.UiExtentReportOptions;
import atf.ui.tests.extent.support.config.environment.UiReportEnvironmentSnapshotFactory;


import java.nio.file.Files;
import java.nio.file.Path;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;

/** Publishes and cleans the standalone reports used by category cardinality tests. */
public final class CategoryCardinalityReportFixtures {
    private CategoryCardinalityReportFixtures() {
    }

    public static Path publish(Path root, CategoryCardinalityFixture fixture) {
        var options = UiExtentReportOptions.builder(root)
                .fileName("categories-" + fixture.name().toLowerCase() + ".html")
                .console(AtfConsoleSink.noOp())
                .environment(UiReportEnvironmentSnapshotFactory.fixture(
                        "Category cardinality", "generated category fixture"))
                .build();
        try (UiExtentReportSession session = UiExtentReportSession.openConfigured(options)) {
            var scenario = session.startScenario("CategoryCardinalityFixture",
                    "atf.ui.tests.extent.e2e.configurable", "generated category fixture",
                    fixture.tags());
            var body = scenario.startPhase(ReportUiContract.Phase.TEST_BODY.displayName());
            body.step(THEN,
                    "generated category fixture is valid",
                    ReportUiContract.Tab.CATEGORIES.hash(), null, AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            return session.reportPath();
        }
    }

    public static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            });
        }
    }
}

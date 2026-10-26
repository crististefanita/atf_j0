package atf.ui.tests.extent.support.config.output;

import atf.testkit.AtfUnitTest;
import atf.testkit.output.UiReportSubdirectory;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static atf.testkit.output.ReportOutputDirectory.resolve;

@AtfUnitTest
class UiReportSubdirectoryTest {
    private static final Path UI_REPORTS = Path.of("C:/atf/artifacts/reports/ui");

    @UiReportSubdirectory("e2e")
    private static final class E2eReportOwner { }

    private static final class DefaultReportOwner { }

    @ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
    private static final class ProjectReportOwner { }

    @AtfTestChannel(TestChannel.API)
    private static final class UnsupportedApiOwner { }

    @Test
    void reportingUiCompositionRejectsAnApiOnlyOwnerBeforeLoadingTheBrowserFixture() {
        assertThrows(IllegalStateException.class,
                () -> ReportingTestResources.uiConfiguration(UnsupportedApiOwner.class));
    }

    @Test
    void reportingCompositionUsesTheProjectRootForTheNewAnnotation() {
        Path standardUiOutput = ReportingTestResources.uiConfiguration().reportDirectory();
        assertEquals(standardUiOutput.resolve("e2e"),
                ReportingTestResources.uiConfiguration(ProjectReportOwner.class).reportDirectory());
    }

    @Test
    void annotatedSuiteReportUsesRelativeE2eDirectory() {
        assertEquals(UI_REPORTS.resolve("e2e").toAbsolutePath().normalize(),
                resolve(UI_REPORTS.getParent().getParent().getParent(), UI_REPORTS, E2eReportOwner.class));
    }

    @Test
    void unannotatedReportsKeepTheExistingUiDirectory() {
        assertEquals(UI_REPORTS.toAbsolutePath().normalize(),
                resolve(UI_REPORTS.getParent().getParent().getParent(), UI_REPORTS, DefaultReportOwner.class));
        assertEquals(UI_REPORTS.toAbsolutePath().normalize(),
                resolve(UI_REPORTS.getParent().getParent().getParent(), UI_REPORTS, null));
    }

    @Test
    void annotationCannotEscapeTheUiReportDirectory() {
        assertThrows(IllegalStateException.class,
                () -> resolve(UI_REPORTS.getParent().getParent().getParent(), UI_REPORTS, TraversalReportOwner.class));
    }

    @Test
    void annotationCannotSelectAnAbsoluteDirectory() {
        assertThrows(IllegalStateException.class, () -> resolve(UI_REPORTS.getParent().getParent().getParent(), UI_REPORTS, AbsoluteReportOwner.class));
    }

    @Test
    void annotationCannotSelectABlankDirectory() {
        assertThrows(IllegalStateException.class, () -> resolve(UI_REPORTS.getParent().getParent().getParent(), UI_REPORTS, BlankReportOwner.class));
    }

    @UiReportSubdirectory("../api")
    private static final class TraversalReportOwner { }

    @UiReportSubdirectory("C:/outside-ui")
    private static final class AbsoluteReportOwner { }

    @UiReportSubdirectory("  ")
    private static final class BlankReportOwner { }
}

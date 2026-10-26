package atf.ui.tests.extent.component.support;

import atf.testkit.AtfComponentTest;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Reproduces IntelliJ's arbitrary working-directory boundary without launching a browser. */
@AtfComponentTest
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ReportingTestResourcesIdeLaunchTest {
    private static final String WORKING_DIRECTORY_PROPERTY = "user.dir";
    private static final String PROJECT_RELATIVE_REPORT_DIRECTORY = "artifacts/reports/ui/ide-smoke";

    @Test
    void resourceDiscoveryKeepsTheReportInTheReactorWhenTheIdeWorkingDirectoryIsExternal(
            @TempDir Path externalWorkingDirectory) {
        String originalWorkingDirectory = System.getProperty(WORKING_DIRECTORY_PROPERTY);
        try {
            System.setProperty(WORKING_DIRECTORY_PROPERTY, externalWorkingDirectory.toString());

            Path reportDirectory = ReportingTestResources.uiConfiguration(IdeRunOwner.class)
                    .reportDirectory();
            Path reactorRoot = reactorRootFromReportingResource();

            assertEquals(externalWorkingDirectory.toAbsolutePath().normalize(),
                    Path.of(System.getProperty(WORKING_DIRECTORY_PROPERTY)).toAbsolutePath().normalize());
            assertEquals(reactorRoot.resolve(PROJECT_RELATIVE_REPORT_DIRECTORY), reportDirectory);
            assertFalse(reportDirectory.startsWith(externalWorkingDirectory),
                    "an IDE working directory outside the checkout must not capture report output");
        } finally {
            restoreWorkingDirectory(originalWorkingDirectory);
        }
    }

    private static Path reactorRootFromReportingResource() {
        Path current = ReportingTestResources.resourcePath(ReportingTestResources.UI_RESOURCE).getParent();
        while (current != null && !Files.isRegularFile(current.resolve("atf-reporting/pom.xml"))) {
            current = current.getParent();
        }
        assertNotNull(current, "reporting test resource must have the ATF reactor as an ancestor");
        return current.toAbsolutePath().normalize();
    }

    private static void restoreWorkingDirectory(String original) {
        if (original == null) System.clearProperty(WORKING_DIRECTORY_PROPERTY);
        else System.setProperty(WORKING_DIRECTORY_PROPERTY, original);
    }

    @ProjectRelativeUiReportDirectory(PROJECT_RELATIVE_REPORT_DIRECTORY)
    private static final class IdeRunOwner { }
}

package atf.ui.tests.extent.component.architecture;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the API/report composition root from drifting back into UI flows. */
@AtfComponentTest
class ApiReportCompositionBoundaryArchitectureTest {
    @Test
    void compositionRootIsTheOnlySetupFacadeAndDoesNotOwnBrowserCode() throws Exception {
        Path source = sourceRoot().resolve(Path.of("support", "api", "ApiReportAuditSetup.java"));
        String text = Files.readString(source);
        assertTrue(!Files.exists(sourceRoot().resolve(Path.of("support", "api", "ApiReportSetupFlow.java"))));
        assertTrue(text.contains("class ApiReportAuditSetup"));
        assertTrue(!text.contains("import com.microsoft.playwright"));
        assertTrue(!text.contains("page.locator("));
    }

    private static Path sourceRoot() throws Exception {
        return Path.of(ApiReportCompositionBoundaryArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent").normalize();
    }
}

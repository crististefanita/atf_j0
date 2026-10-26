package atf.ui.tests.extent.component.architecture;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiReportDurationCompositionArchitectureTest {
    @Test
    void durationFacadeDoesNotOwnBrowserObservationPolicyOrGeometry() throws Exception {
        Path sourceRoot = sourceRoot();
        String source = Files.readString(sourceRoot.resolve(
                "support/inspection/duration/ApiReportDurationInspector.java"));

        assertFalse(source.contains("page.evaluate"), "DOM observation belongs to auditors");
        assertFalse(source.contains("setViewportSize"), "viewport replay belongs to geometry auditor");
        assertFalse(source.contains("querySelector"), "DOM selectors belong to observation owners");
        assertTrue(source.contains("ApiReportDurationDomInspector"));
        assertTrue(source.contains("ApiReportDurationResponsiveInspector"));
        assertTrue(Files.size(sourceRoot.resolve(
                "support/inspection/duration/ApiReportDurationInspector.java")) < 8_000,
                "duration facade must remain orchestration-only");
    }

    private static Path sourceRoot() throws Exception {
        return Path.of(ApiReportDurationCompositionArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent").normalize();
    }
}

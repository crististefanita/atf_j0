package atf.ui.tests.extent.component.section.architecture;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents the retired DOM-inspector/validator seam from being silently restored. */
@AtfComponentTest
class DomInspectorMigrationBoundaryTest {
    @Test
    void migratedExtractorsAndPureValidatorsAreTheActiveBoundary() {
        Path root = projectRoot();
        Path test = root.resolve("atf-reporting-tests/src/test/java/atf/ui/tests/extent");
        Path main = root.resolve("atf-reporting/src/main/java/atf/ui/tests/extent");
        Path reporting = root.resolve("atf-reporting/src/main/java/atf/reporting/ui/audit/section");
        assertTrue(Files.isRegularFile(test.resolve("support/section/extract/EvidenceSectionExtractor.java")));
        assertTrue(Files.isRegularFile(test.resolve("support/section/extract/ApiReportStructureExtractor.java")));
        assertTrue(Files.isRegularFile(test.resolve("support/section/extract/FilterSectionExtractor.java")));
        assertTrue(Files.isRegularFile(reporting.resolve("JsonEvidenceValidator.java")));
        assertTrue(Files.isRegularFile(reporting.resolve("VisualEvidenceValidator.java")));
        assertFalse(Files.exists(main.resolve("flows/inspection/ApiReportRequestEvidenceInspector.java")));
        assertFalse(Files.exists(main.resolve("flows/inspection/ApiReportStructureInspector.java")));
        assertFalse(Files.exists(main.resolve("flows/inspection/ApiReportFilterInspector.java")));
        assertFalse(Files.exists(main.resolve("audit/CategoryScreenValidator.java")));
        assertFalse(Files.exists(main.resolve("audit/EvidenceScreenValidator.java")));
    }

    private static Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("atf-tests"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("ATF project root not found");
    }
}

package atf.ui.tests.extent.component.section.architecture;
import atf.testkit.AtfComponentTest;
import atf.reporting.ui.audit.section.JsonEvidenceValidator;
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
        ClassLoader library = JsonEvidenceValidator.class.getClassLoader();
        assertTrue(Files.isRegularFile(test.resolve("support/section/extract/EvidenceSectionExtractor.java")));
        assertTrue(Files.isRegularFile(test.resolve("support/section/extract/ApiReportStructureExtractor.java")));
        assertTrue(Files.isRegularFile(test.resolve("support/section/extract/FilterSectionExtractor.java")));
        assertTrue(library.getResource("atf/reporting/ui/audit/section/JsonEvidenceValidator.class") != null);
        assertTrue(library.getResource("atf/reporting/ui/audit/section/VisualEvidenceValidator.class") != null);
        for (String retired : java.util.List.of("flows/inspection/ApiReportRequestEvidenceInspector",
                "flows/inspection/ApiReportStructureInspector", "flows/inspection/ApiReportFilterInspector",
                "audit/CategoryScreenValidator", "audit/EvidenceScreenValidator")) {
            assertFalse(library.getResource("atf/ui/tests/extent/" + retired + ".class") != null,
                    "Retired production boundary returned: " + retired);
        }
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

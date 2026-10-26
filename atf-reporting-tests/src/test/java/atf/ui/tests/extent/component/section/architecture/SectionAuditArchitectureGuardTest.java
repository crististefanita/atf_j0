package atf.ui.tests.extent.component.section.architecture;
import atf.core.files.AtfPathLocator;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps annotation-driven composition roots free of browser implementation details. */
@AtfComponentTest
class SectionAuditArchitectureGuardTest {
    private static final Path TEST_SOURCE_ROOT = AtfPathLocator.locateAtfTestSourceRoot(
            Path.of(System.getProperty("user.dir", ".")), Path.of("atf", "ui", "tests"));

    @Test
    void generatedAnnotationTestDependsOnlyOnCompositionApi() throws Exception {
        Path source = TEST_SOURCE_ROOT.resolve(
                "atf/ui/tests/extent/e2e/section/SelectedApiTestsReportUiTest.java");
        assertTrue(Files.isRegularFile(source), "generated section test source must be present");
        String text = Files.readString(source);
        assertFalse(text.contains("ReportUiLocators"));
        assertFalse(text.contains("com.microsoft.playwright.Page"));
        assertFalse(text.contains("com.microsoft.playwright.Locator"));
        assertFalse(text.contains(".locator("));
        assertTrue(text.contains("@UiExtentSectionAudit"));
        assertTrue(text.contains("SectionAuditDynamicEngine"));
        assertTrue(Files.isRegularFile(TEST_SOURCE_ROOT.resolve(
                "atf/ui/tests/extent/support/section/runtime/SectionAuditDynamicEngine.java")));
        assertFalse(Files.exists(TEST_SOURCE_ROOT.resolve(
                "atf/ui/tests/extent/section/SectionAuditDynamicEngine.java")));
    }

    @Test
    void sectionModulesReturnRendererNeutralResultsAndDoNotAssertWithJUnit() throws Exception {
        Path root = TEST_SOURCE_ROOT.resolve("atf/ui/tests/extent/support/section/module");
        try (var files = Files.list(root)) {
            files.filter(path -> path.getFileName().toString().endsWith("SectionAuditModule.java"))
                    .forEach(path -> {
                        try {
                            String text = Files.readString(path);
                            assertTrue(text.contains("SectionAuditResult audit("), path.toString());
                            assertFalse(text.contains("org.junit.jupiter.api.Assertions"), path.toString());
                            assertFalse(text.contains("assertTrue("), path.toString());
                            assertFalse(text.contains("auditResult("), path.toString());
                            assertFalse(text.contains("capture("), path.toString());
                            assertFalse(text.contains("ReportAuditChecks.status"), path.toString());
                        } catch (Exception error) {
                            throw new AssertionError(error);
                        }
                    });
        }
    }

    @Test
    void rendererPublicationIsOwnedByThePublisherAdapter() throws Exception {
        String publisher = Files.readString(TEST_SOURCE_ROOT.resolve(
                "atf/ui/tests/extent/support/section/publish/ExtentSectionAuditPublisher.java"));
        assertTrue(publisher.contains("auditResult("));
        assertTrue(publisher.contains("SectionAuditResult result = module.audit"));
        assertTrue(publisher.contains("audit.screen("));
    }

    @Test
    void legacyGlobalRuleVocabularyIsGone() throws Exception {
        Path root = TEST_SOURCE_ROOT.resolve("atf/ui/tests/extent");
        try (var files = Files.walk(root)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString()
                    .equals("UiReportAuditRule.java")));
        }
        assertFalse(Files.exists(TEST_SOURCE_ROOT.resolve(
                "atf/ui/tests/extent/engine/UiReportAuditDynamicEngine.java")),
                "the retired rule engine must not compete with SectionAuditDynamicEngine");
    }

}

package atf.ui.tests.extent.component.architecture;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source boundary for the declarative API-to-UI test path. */
@AtfComponentTest
class ApiReportUiArchitectureTest {
    @Test
    void selectedApiScenarioContainsIntentOnlyAndNoPlaywrightSelectors() throws Exception {
        Path source = Path.of(ApiReportUiArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent/e2e/section/SelectedApiTestsReportUiTest.java")
                .normalize();
        String text = Files.readString(source);

        assertTrue(text.contains("@ApiScenarioSetup"));
        assertTrue(text.contains("@ApiTestRef(owner = UsersApiScenarios.class"));
        assertTrue(text.contains("@ApiTestRef(owner = PostsApiScenarios.class"));
        assertTrue(text.contains("@ApiTestRef(owner = LifecycleApiScenarios.class"));
        assertTrue(text.contains("method = PostsApiScenarios.CREATE_POST"));
        assertTrue(text.contains("method = LifecycleApiScenarios.LIFECYCLE_MATRIX"));
        assertTrue(!text.contains("families ="), "the smoke root must not select whole families");
        assertTrue(text.contains("@UiExtentSectionAudit"));
        assertTrue(text.contains("SectionAuditDynamicEngine"));
        assertTrue(!text.contains(".locator("), "UI test intent must not build selectors");
        assertTrue(!text.contains("getByRole("), "UI test intent must not build role selectors");
        assertTrue(!text.contains("getByText("), "UI test intent must not build text selectors");
    }

    @Test
    void duplicateExploratoryCompositionRootsRemainRetired() throws Exception {
        Path root = Path.of(ApiReportUiArchitectureTest.class
                .getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("../../src/test/java/atf/ui/tests/extent")
                .normalize();

        assertTrue(!Files.exists(root.resolve("exploratory/ApiExtentReportExploratoryTest.java")));
        assertTrue(!Files.exists(root.resolve("exploratory/ExistingExtentReportExploratoryTest.java")));
        assertTrue(!Files.exists(root.resolve("exploratory/ExistingExtentCategoriesExploratoryTest.java")));
    }
}

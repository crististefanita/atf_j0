package atf.reporting.extent.architecture;

import atf.core.config.module.AtfModuleIds;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents the Extent contract suite from regressing into one mixed-responsibility class. */
@AtfUnitTest
class ExtentReportContractCohesionArchitectureTest {
    private static final String TEST_JAVA = "src/test/java";
    private static final String EXTENT_SESSION_PACKAGE = "atf/reporting/extent/session";
    private static final Path INTERACTION = Path.of(
            TEST_JAVA, EXTENT_SESSION_PACKAGE, "ExtentReportInteractionContractTest.java");
    private static final Path FAILURE = Path.of(
            TEST_JAVA, EXTENT_SESSION_PACKAGE, "ExtentReportFailureProjectionContractTest.java");
    private static final Path NAVIGATION = Path.of(
            TEST_JAVA, EXTENT_SESSION_PACKAGE, "ExtentReportNavigationContractTest.java");

    @Test
    void extentInteractionContractsHaveIndependentSemanticOwners() throws Exception {
        Path root = locateProjectRoot();
        Path reporting = root.resolve(AtfModuleIds.REPORTING_TESTS);
        String interaction = read(reporting.resolve(INTERACTION));
        String failure = read(reporting.resolve(FAILURE));
        String navigation = read(reporting.resolve(NAVIGATION));

        assertTrue(interaction.contains("testTreePublishesCollapsibleScenarioPhaseAndEvidenceContracts"));
        assertTrue(interaction.contains("failedTransportRequestKeepsEvidenceZonesClosedAndSemanticallyDistinct"));
        assertFalse(interaction.contains("failsProjectionKeepsRequestOwnershipAndFailureCauseChain"));
        assertFalse(interaction.contains("testsTabPublishesIndependentNavigationAndFilterContracts"));

        assertTrue(failure.contains("lifecyclePhasesRemainOrderedWhenSomePhasesAreSkippedOrFailed"));
        assertTrue(failure.contains("failsProjectionKeepsRequestOwnershipAndFailureCauseChain"));
        assertFalse(failure.contains("testsTabPublishesIndependentNavigationAndFilterContracts"));

        assertTrue(navigation.contains("testsTabPublishesIndependentNavigationAndFilterContracts"));
        assertTrue(navigation.contains("testsFiltersPreserveOwnerScopeAndTabIdentity"));
        assertTrue(navigation.contains("categoriesUseIndependentProjectionAndExactScenarioDeepLinks"));
        assertFalse(navigation.contains("failsProjectionKeepsRequestOwnershipAndFailureCauseChain"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "Missing semantic contract owner: " + path);
        return Files.readString(path);
    }

    private static Path locateProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(AtfModuleIds.CORE))
                    && Files.isRegularFile(current.resolve("pom.xml"))) return current;
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
    }
}

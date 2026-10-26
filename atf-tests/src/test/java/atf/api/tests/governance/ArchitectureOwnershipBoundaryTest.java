package atf.api.tests.governance;

import atf.core.config.module.AtfModuleIds;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.api.tests.governance.AtfGovernanceTestSupport.assertNoPackagePrefix;
import static atf.api.tests.governance.AtfGovernanceTestSupport.containsJavaFileNamed;
import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards whole-ATF ownership and keeps reporting composition in its consumer module. */
@AtfComponentTest
public class ArchitectureOwnershipBoundaryTest {
    private static final String MAIN_JAVA = "src/main/java";
    private static final String TEST_JAVA = "src/test/java";
    private static final String TESTS_GOVERNANCE_PACKAGE = "atf/api/tests/governance";
    private static final String REPORTING_TESTS_PACKAGE = "atf/api/tests";
    private static final String REPORTING_UI_TESTS_PACKAGE = "atf/ui/tests/extent";

    @Test
    void atfTestsOwnOnlyWholeProjectGovernance() throws Exception {
        Path root = locateProjectRoot();
        Path testModule = root.resolve(AtfModuleIds.TESTS);
        Path reportingTests = modulePath(root, AtfModuleIds.REPORTING_TESTS, TEST_JAVA);

        assertAll(
                () -> assertFalse(containsJavaFileNamed(testModule.resolve("src/main"), ".java"),
                        "atf-tests must not own production Java code"),
                () -> requireFile(testModule.resolve(TEST_JAVA).resolve(TESTS_GOVERNANCE_PACKAGE)
                                .resolve("ArchitectureModuleBoundaryTest.java"),
                        "whole-ATF module governance belongs to atf-tests"),
                () -> assertFalse(Files.exists(reportingTests.resolve(REPORTING_TESTS_PACKAGE)
                                .resolve("architecture/ArchitectureModuleBoundaryTest.java")),
                        "whole-ATF governance must not remain in atf-reporting"),
                () -> assertFalse(containsJavaFileNamed(testModule.resolve(TEST_JAVA), "UsersApiTests.java"),
                        "atf-tests must not own executable API tests"),
                () -> assertFalse(containsJavaFileNamed(testModule.resolve(TEST_JAVA), "SelectedApiTestsReportUiTest.java"),
                        "atf-tests must not own reporting UI E2E tests"),
                () -> requireFile(reportingTests.resolve("atf/api/tests/api/users/UsersApiScenarios.java"),
                        "UsersApiScenarios belongs to atf-reporting test scope"),
                () -> requireFile(reportingTests.resolve("atf/api/tests/api/posts/PostsApiScenarios.java"),
                        "PostsApiScenarios belongs to atf-reporting test scope"),
                () -> requireFile(modulePath(root, AtfModuleIds.REPORTING_TESTS, TEST_JAVA,
                                "atf/reporting/api/acceptance/ApiAcceptancePublisher.java"),
                        "API acceptance publisher belongs to the reporting test composition boundary"),
                () -> requireFile(modulePath(root, AtfModuleIds.REPORTING_TESTS, TEST_JAVA,
                                "atf/reporting/api/acceptance/ApiAcceptancePlanBuilder.java"),
                        "API acceptance plan builder belongs to the reporting test composition boundary"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/locators/ReportUiLocators.java"),
                        "report locators belong to reporting test support"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/audit/ReportAuditContext.java"),
                        "report audit context belongs to reporting test support"),
                () -> requireFile(modulePath(root, AtfModuleIds.REPORTING, MAIN_JAVA,
                                "atf/reporting/ui/UiAuditLifecycle.java"),
                        "UI audit lifecycle belongs to reporting infrastructure"),
                () -> assertFalse(Files.exists(testModule.resolve("src/main/atf/ui/tests/extent/support/lifecycle/UiAuditLifecycle.java")),
                        "atf-tests must not own reporting lifecycle infrastructure"),
                () -> requireFile(reportingTests.resolve("atf/api/tests/support/setup/ApiScenarioSetupCatalog.java"),
                        "scenario-coupled API setup catalog belongs to reporting test support"),
                () -> requireFile(reportingTests.resolve("atf/api/tests/api/users/UsersApiTests.java"),
                        "UsersApiTests belongs to reporting test scope"),
                () -> requireFile(reportingTests.resolve("atf/api/tests/api/posts/PostsApiTests.java"),
                        "PostsApiTests belongs to reporting test scope"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/section/runtime/SectionAuditDynamicEngine.java"),
                        "JUnit dynamic-node construction belongs to reporting test support"),
                () -> assertFalse(Files.isRegularFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("section/SectionAuditDynamicEngine.java")),
                        "test orchestration must not be mixed with the section model package"),
                () -> requireFile(modulePath(root, AtfModuleIds.CORE, MAIN_JAVA,
                                "atf/core/config/source/ApiBootstrapConfigurationLoader.java"),
                        "generic YAML API configuration loader belongs to atf-core"),
                () -> requireFile(modulePath(root, AtfModuleIds.CORE, MAIN_JAVA,
                                "atf/core/testing/http/RecordingApiTransport.java"),
                        "reusable request capture belongs to atf-core"),
                () -> assertFalse(Files.exists(testModule.resolve("src/main/atf/api/tests/support/setup/ApiBootstrapConfigurationLoader.java")),
                        "configuration loader must remain in atf-core"),
                () -> assertFalse(Files.exists(reportingTests.resolve("atf/api/tests/support/junit/RecordingApiTransport.java")),
                        "request capture must remain in atf-core"),
                () -> requireFile(modulePath(root, AtfModuleIds.REPORTING, MAIN_JAVA,
                                "atf/reporting/ui/contract/UiReportArtifactContract.java"),
                        "static UI artifact contract belongs to atf-reporting"),
                () -> requireFile(modulePath(root, AtfModuleIds.REPORTING_TESTS, TEST_JAVA,
                                "atf/reporting/ui/contract/UiReportArtifactContractTest.java"),
                        "static UI artifact coverage belongs to reporting tests"),
                () -> assertFalse(Files.exists(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("section/scenarios/StaticScreenValidationReportUiTest.java")),
                        "duplicate static Screen validation E2E must not return"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("e2e/uireport/StaticUiExtentReportContractTest.java"),
                        "one configurable browser boundary belongs to reporting test scope"),
                () -> requireFile(modulePath(root, AtfModuleIds.REPORTING_TESTS, TEST_JAVA,
                                "atf/reporting/support/MockApiReportContractFixtures.java"),
                        "reporting mock contract fixture data belongs to reporting test scope"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/configurable/CategoryCardinalityFixture.java"),
                        "category cardinality data belongs to an explicit fixtures namespace"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/configurable/CategoryCardinalityReportFixtures.java"),
                        "category fixture publishing belongs to an explicit fixtures namespace"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/sanity/UiThenDisclosureReportFixtures.java"),
                        "sanity report inputs belong to an explicit fixtures namespace"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/api/ApiReportSetupDeclarations.java"),
                        "annotation-only setup declarations belong to an explicit fixtures namespace"),
                () -> requireFile(reportingTests.resolve(REPORTING_UI_TESTS_PACKAGE)
                                .resolve("support/section/fixtures/MissingApiSetupDeclaration.java"),
                        "invalid source declarations belong to an explicit fixtures namespace"),
                () -> requireFile(modulePath(root, AtfModuleIds.MOCK, TEST_JAVA,
                                "atf/api/mock/fixtures/MockTransportTestFixtures.java"),
                        "mock transport vocabulary belongs to an explicit test fixtures namespace"),
                () -> assertFalse(Files.isRegularFile(modulePath(root, AtfModuleIds.MOCK, TEST_JAVA,
                                "atf/api/mock/MockTransportTestFixtures.java")),
                        "mock transport fixtures must not be mixed with production-package tests"));
    }

    @Test
    void uiConfigurationOwnershipKeepsAtfUiGeneric() throws Exception {
        Path root = locateProjectRoot();
        Path genericUiConfig = modulePath(root, AtfModuleIds.UI, MAIN_JAVA, "atf/ui/config");
        Path reportingUiConfig = modulePath(root, AtfModuleIds.REPORTING_TESTS, TEST_JAVA,
                "atf/ui/tests/extent/support/config");
        assertAll(
                () -> requireFile(genericUiConfig.resolve("UiBrowserConfiguration.java"),
                        "browser policy belongs to generic atf-ui runtime"),
                () -> requireFile(genericUiConfig.resolve("UiScreenValidationConfiguration.java"),
                        "screen capture policy belongs to generic atf-ui runtime"),
                () -> forbidFile(genericUiConfig.resolve("UiConfiguration.java"),
                        "report output and audit composition must not be owned by atf-ui"),
                () -> forbidFile(genericUiConfig.resolve("UiAuditSuiteConfiguration.java"),
                        "report audit suites must not be owned by atf-ui"),
                () -> forbidFile(genericUiConfig.resolve("AtfUiConfigurationLoader.java"),
                        "report YAML loading must not be owned by atf-ui"),
                () -> requireFile(reportingUiConfig.resolve("UiConfiguration.java"),
                        "report UI configuration belongs to reporting test scope"),
                () -> requireFile(reportingUiConfig.resolve("UiAuditSuiteConfiguration.java"),
                        "report audit suites belong to reporting test scope"),
                () -> requireFile(reportingUiConfig.resolve("AtfUiConfigurationLoader.java"),
                        "report YAML loading belongs to reporting test scope"),
                () -> assertFalse(Files.readString(modulePath(root, AtfModuleIds.UI, "pom.xml"))
                                .contains("jackson-dataformat-yaml"),
                        "generic atf-ui must not carry the reporting YAML parser dependency"));
    }

    @Test
    void atfSourceRootsExposeOneJavaPackageRootWithoutAtfAtf() throws Exception {
        Path module = locateProjectRoot().resolve(AtfModuleIds.REPORTING);
        Path main = module.resolve(MAIN_JAVA);
        Path tests = module.resolve(TEST_JAVA);
        assertAll(
                () -> assertTrue(Files.isDirectory(main.resolve("atf")), "src/main must expose the atf Java package directly"),
                () -> assertTrue(Files.isDirectory(tests.resolve("atf")), "src/test must expose the atf Java package directly"),
                () -> assertFalse(Files.exists(main.resolve("atf/atf")), "src/main must not recreate the atf.atf physical layer"),
                () -> assertFalse(Files.exists(tests.resolve("atf/atf")), "src/test must not recreate the atf.atf physical layer"),
                () -> assertNoPackagePrefix(main, "atf.atf"),
                () -> assertNoPackagePrefix(tests, "atf.atf"));
    }

    @Test
    void reportingSemanticPackagesHaveOneArtifactAndThemeOwner() throws Exception {
        Path reporting = modulePath(locateProjectRoot(), AtfModuleIds.REPORTING, MAIN_JAVA,
                "atf/reporting");
        assertAll(
                () -> forbidFile(reporting.resolve("ReportArtifactInspector.java"),
                        "artifact inspector must not be duplicated in the reporting facade package"),
                () -> requireFile(reporting.resolve("artifact/ReportArtifactInspector.java"),
                        "artifact inspector belongs to the artifact package"),
                () -> requireFile(reporting.resolve("artifact/contract/ReportArtifactContractChecks.java"),
                        "artifact contract orchestration belongs to artifact.contract"),
                () -> forbidFile(reporting.resolve("AtfReportFailsLayoutCss.java"),
                        "theme CSS must not be duplicated in the reporting facade package"),
                () -> requireFile(reporting.resolve("theme/AtfReportFailsLayoutCss.java"),
                        "theme CSS belongs to the theme package"));
    }

    @Test
    void reportingOwnsRendererNeutralAuditKernel() throws Exception {
        Path root = locateProjectRoot();
        Path reporting = modulePath(root, AtfModuleIds.REPORTING, MAIN_JAVA, "atf/reporting");
        Path globalTests = modulePath(root, AtfModuleIds.TESTS, TEST_JAVA);
        assertAll(
                () -> requireFile(reporting.resolve("audit/ValidationResult.java"),
                        "renderer-neutral validation result belongs to atf-reporting"),
                () -> requireFile(reporting.resolve("audit/ReportAuditSnapshot.java"),
                        "renderer-neutral report snapshot belongs to atf-reporting"),
                () -> requireFile(reporting.resolve("ui/audit/section/ScreenValidationSectionValidator.java"),
                        "pure UI report section validators belong to atf-reporting"),
                () -> requireFile(reporting.resolve("ui/failure/UiFailureReportAudit.java"),
                        "failure report metadata belongs to atf-reporting"),
                () -> requireFile(reporting.resolve("ui/failure/UiFailureReportSpec.java"),
                        "failure report specification belongs to atf-reporting"),
                () -> requireFile(reporting.resolve("ui/audit/source/ResolvedReportSource.java"),
                        "resolved report source belongs to atf-reporting"),
                () -> forbidFile(globalTests.resolve("atf/ui/tests/extent"),
                        "atf-tests governance must not absorb reporting UI test infrastructure"),
                () -> forbidFile(globalTests.resolve("atf/reporting"),
                        "atf-tests governance must not absorb reporting production models"));
    }

    @Test
    void reportUiAuditsUseOneSharedFlowBoundary() throws Exception {
        Path root = locateProjectRoot().resolve(AtfModuleIds.REPORTING_TESTS).resolve(TEST_JAVA)
                .resolve(REPORTING_UI_TESTS_PACKAGE);
        var violations = new ArrayList<String>();
        List<String> retired = List.of(
                "exploratory/ApiExtentReportExploratoryTest.java",
                "exploratory/ExistingExtentReportExploratoryTest.java",
                "exploratory/ExistingExtentCategoriesExploratoryTest.java");
        for (String relative : retired) {
            if (Files.exists(root.resolve(relative))) violations.add("retired UI composition root was reintroduced: " + relative);
        }
        Path selected = root.resolve("e2e/section/SelectedApiTestsReportUiTest.java");
        String selectedContent = Files.readString(selected);
        if (!selectedContent.contains("@ApiScenarioSetup(tests =")
                || !selectedContent.contains("@ApiTestRef(owner =")
                || !selectedContent.contains("SectionAuditDynamicEngine")) {
            violations.add("selected generated audit must be exact-method and section-engine based");
        }
        Path external = root.resolve("e2e/uireport/StaticUiExtentReportContractTest.java");
        String externalContent = Files.readString(external);
        if (!externalContent.contains("@UiExtentReportContract")
                || !externalContent.contains("UiReportContractDynamicEngine")) {
            violations.add("external UI report audit must remain the single opt-in browser gate");
        }
        assertTrue(violations.isEmpty(), () -> "UI audit flow boundary violations: " + violations);
    }

    private static Path modulePath(Path root, String module, String... segments) {
        Path path = root.resolve(module);
        for (String segment : segments) path = path.resolve(segment);
        return path;
    }

    private static void requireFile(Path path, String message) {
        assertTrue(Files.isRegularFile(path), message + ": " + path);
    }

    private static void forbidFile(Path path, String message) {
        assertFalse(Files.exists(path), message + ": " + path);
    }
}

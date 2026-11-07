package atf.ui.tests.extent.e2e.section;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;

import atf.testkit.setup.api.ApiScenarioSetup;
import atf.testkit.setup.api.ApiTestRef;
import atf.reporting.api.tests.api.lifecycle.LifecycleApiScenarios;
import atf.reporting.api.tests.api.posts.PostsApiScenarios;
import atf.reporting.api.tests.api.requests.RequestVariantsApiScenarios;
import atf.reporting.api.tests.api.users.UsersApiScenarios;
import atf.reporting.ui.audit.plan.ExtentJourney;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.plan.UiExtentReportSource;
import atf.reporting.ui.audit.plan.UiExtentSectionAudit;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.ui.tests.extent.support.section.runtime.SectionAuditFixture;
import atf.ui.tests.extent.support.section.runtime.SectionAuditDynamicEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.stream.Stream;

/**
 * The single generated API-report browser slice.
 *
 * <p>Three exact API methods provide deliberately different evidence: a
 * positive GET, an explicit soft-contract WARNING, a JSON POST and the
 * lifecycle matrix with positive, skipped and failed outcomes. All report
 * sections are audited through the shared section modules; the full API matrix
 * stays in API/reporting tests.</p>
 */
@ApiScenarioSetup(tests = {
        @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION),
        @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION_WARNING),
        @ApiTestRef(owner = PostsApiScenarios.class, method = PostsApiScenarios.CREATE_POST),
        @ApiTestRef(owner = RequestVariantsApiScenarios.class, method = RequestVariantsApiScenarios.PROFILE_UPDATE),
        @ApiTestRef(owner = LifecycleApiScenarios.class,
                method = LifecycleApiScenarios.LIFECYCLE_MATRIX)
})
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@UiExtentSectionAudit(source = UiExtentReportSource.GENERATED_API,
        sections = {ExtentSection.OVERVIEW, ExtentSection.TESTS, ExtentSection.CATEGORIES,
                ExtentSection.FAILS, ExtentSection.EVIDENCE},
        checks = {ExtentSectionCheck.STRUCTURE, ExtentSectionCheck.STYLE,
                ExtentSectionCheck.JSON_CONTRACT, ExtentSectionCheck.MOCK_RECONCILIATION,
                ExtentSectionCheck.COLLAPSED_BY_DEFAULT, ExtentSectionCheck.CROSS_SECTION},
        journey = ExtentJourney.FAILURE_NAVIGATION)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Selected API methods — complete Extent report sections")
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
@ReportBrowserTest
class SelectedApiTestsReportUiTest {
    private SectionAuditFixture fixture;

    @BeforeAll
    void open() {
        UiConfiguration configuration = ReportingTestResources.uiConfiguration(getClass());
        fixture = SectionAuditFixture.open(getClass(), configuration);
    }

    @AfterAll
    void close() {
        if (fixture != null) fixture.close();
    }

    @TestFactory
    Stream<DynamicNode> selectedApiReportSections() {
        return Stream.of(SectionAuditDynamicEngine.container(fixture));
    }
}

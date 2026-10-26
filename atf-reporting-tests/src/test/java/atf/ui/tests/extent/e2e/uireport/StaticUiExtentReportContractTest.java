package atf.ui.tests.extent.e2e.uireport;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.reporting.artifact.ReportArtifactIdentity;
import atf.ui.tests.extent.support.uireport.UiReportContractDynamicEngine;
import atf.ui.tests.extent.support.uireport.browser.UiReportContractFixture;
import atf.reporting.ui.contract.UiExtentReportContract;

import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
import atf.ui.tests.extent.support.uireport.browser.UiReportContractInputFactory;
import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Plug-and-play browser gate for a previously generated UI Extent report.
 *
 * <p>An explicit {@code -Datf.ui.report=<report.html>} selects a previous
 * artifact. Without one, a fresh UI evidence fixture is generated, so a
 * direct IntelliJ run remains self-contained and exercises the UI contract.</p>
 */
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@UiExtentReportContract
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Static UI Extent report — all tabs and renderer contract")
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
class StaticUiExtentReportContractTest {
    private UiReportContractFixture fixture;

    @BeforeAll
    void open() {
        UiConfiguration configuration = ReportingTestResources.uiConfiguration(getClass());
        String reportOverride = System.getProperty("atf.ui.report");
        Path inputReport = reportOverride != null && !reportOverride.isBlank()
                ? Path.of(reportOverride).toAbsolutePath().normalize()
                : configuration.auditInputReport().orElseGet(
                        () -> UiReportContractInputFactory.create(configuration));
        fixture = UiReportContractFixture.open(getClass(), configuration,
                new atf.ui.tests.extent.support.uireport.contract.UiReportInput(
                        inputReport, ReportArtifactIdentity.read(inputReport)));
    }

    @AfterAll
    void close() {
        if (fixture != null) fixture.close();
    }

    @TestFactory
    Stream<DynamicNode> allTabs() {
        return Stream.of(UiReportContractDynamicEngine.container(fixture));
    }
}

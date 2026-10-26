package atf.ui.tests.extent.e2e.failure;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.ui.tests.extent.support.failure.UiFailureReportDynamicEngine;
import atf.ui.tests.extent.support.failure.UiFailureReportFixture;

import atf.reporting.ui.failure.UiFailureReportAudit;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.testkit.output.ProjectRelativeUiReportDirectory;
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

import java.util.stream.Stream;

/** Real browser contract for intentional UI failure surfaces. */
@UiFailureReportAudit
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("UI Extent failure report — setup, Playwright, audit and assertion")
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
@ProjectRelativeUiReportDirectory("artifacts/reports/ui/e2e")
class UiFailureReportContractTest {
    private UiFailureReportFixture fixture;

    @BeforeAll
    void open() {
        UiConfiguration configuration = ReportingTestResources.uiConfiguration(getClass());
        fixture = UiFailureReportFixture.open(getClass(), configuration);
    }

    @AfterAll
    void close() {
        if (fixture != null) fixture.close();
    }

    @TestFactory
    Stream<DynamicNode> failureSurfaces() {
        return Stream.of(UiFailureReportDynamicEngine.container(fixture));
    }
}

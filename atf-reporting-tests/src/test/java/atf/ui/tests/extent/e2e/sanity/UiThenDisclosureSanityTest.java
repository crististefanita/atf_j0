package atf.ui.tests.extent.e2e.sanity;
import atf.testkit.AtfE2eTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.ui.tests.extent.support.navigation.ThenDisclosureFlow;
import atf.reporting.ui.contract.ReportUiContract;

import atf.debugui.DebugUiConfiguration;
import atf.debugui.DebugUiServer;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.tests.extent.support.sanity.UiThenDisclosureReportFixtures;
import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Intent/assertions only; the DOM contract is owned by {@link ThenDisclosureFlow}. */
@AtfModuleTest(requires = {AtfModule.REPORTING, AtfModule.UI})
@AtfE2eTest
@AtfTestChannel(TestChannel.UI)
class UiThenDisclosureSanityTest {
    @Test
    void explicitTeardownOutcomeIsStandaloneGherkinDisclosure() throws Exception {
        UiConfiguration configuration = ReportingTestResources.uiConfiguration(getClass());
        Path root = Files.createTempDirectory("atf-ui-then-disclosure-");
        Path report = UiThenDisclosureReportFixtures.publishTeardown(root);
        try (DebugUiServer server = DebugUiServer.start(DebugUiConfiguration.forHtml(report, 0));
             PlaywrightUiSession browser = PlaywrightUiSession.open(configuration.browser())) {
            browser.navigate(java.net.URI.create(server.uriFor(report) + ReportUiContract.Tab.TESTS.hash()));
            ThenDisclosureFlow.ThenSnapshot snapshot = new ThenDisclosureFlow().inspectThenDisclosure(browser.page());
            assertAll(
                    () -> assertTrue(snapshot.rendered(), "The THEN outcome must be rendered"),
                    () -> assertEquals("request", snapshot.role()),
                    () -> assertTrue(snapshot.initialized(), "The THEN card must be initialized as a disclosure"),
                    () -> assertTrue(snapshot.disclosureGlyph(), "The collapsed THEN card must expose a disclosure glyph"),
                    () -> assertEquals(0, snapshot.nestedUiValidations(), "The publication outcome must not contain a synthetic UI validation child"),
                    () -> assertTrue(snapshot.titleVisible(), "The final Gherkin outcome must remain visible"));
        }
    }

    @Test
    void uiAuditWithoutTeardownRendersOnlyRequestedPhases() throws Exception {
        UiConfiguration configuration = ReportingTestResources.uiConfiguration(getClass());
        Path root = Files.createTempDirectory("atf-ui-no-teardown-sanity-");
        Path report = UiThenDisclosureReportFixtures.publishWithoutTeardown(root);
        try (DebugUiServer server = DebugUiServer.start(DebugUiConfiguration.forHtml(report, 0));
             PlaywrightUiSession browser = PlaywrightUiSession.open(configuration.browser())) {
            browser.navigate(java.net.URI.create(server.uriFor(report) + ReportUiContract.Tab.TESTS.hash()));
            ThenDisclosureFlow.PhaseSnapshot snapshot = new ThenDisclosureFlow().inspectWithoutTeardown(browser.page());
            assertAll(
                    () -> assertEquals(2, snapshot.phaseCount(), "Only SETUP and TEST BODY should render"),
                    () -> assertTrue(snapshot.setupVisible()),
                    () -> assertTrue(snapshot.testBodyVisible()),
                    () -> assertTrue(!snapshot.teardownVisible(), "A UI audit without cleanup must not render TEARDOWN"));
        }
    }

}

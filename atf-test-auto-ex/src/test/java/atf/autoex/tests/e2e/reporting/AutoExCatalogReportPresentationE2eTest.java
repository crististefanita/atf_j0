package atf.autoex.tests.e2e.reporting;

import atf.autoex.tests.support.reporting.AutoExCatalogApiReportFixture;
import atf.core.reporting.console.AtfConsoleSink;
import atf.testkit.AtfE2eTest;
import atf.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Opens only the generated local report and audits its collapsible Extent evidence. */
@AtfE2eTest
public class AutoExCatalogReportPresentationE2eTest {
    @Test
    void selectedCatalogAssertionAndRequestDetailsExpandInExtent() throws Exception {
        Path reportRoot = Path.of(System.getProperty("atf.test.reportDirectory", "target/autoex-runtime-reports"))
                .toAbsolutePath().resolve("local-report-ui");
        Path reportPath = AutoExCatalogApiReportFixture.create(reportRoot);

        try (var viewer = PlaywrightUiSession.open(
                AutoExCatalogApiReportFixture.browserConfiguration(), AtfConsoleSink.noOp())) {
            Page page = viewer.page();
            page.navigate(reportPath.toUri().toString());
            page.locator("#nav-test").click(new Locator.ClickOptions().setTimeout(3000));
            var reportTest = page.locator("p.name:text-is('CatalogApiCorrelation')");
            assertTrue(reportTest.isVisible(), "the generated Extent test is listed");
            reportTest.locator("xpath=ancestor::li[contains(concat(' ', normalize-space(@class), ' '), ' test-item ')][1]")
                    .click(new Locator.ClickOptions().setTimeout(3000));
            var detail = page.locator("#test-view .test-content-detail .detail-body");
            var scenario = detail.locator("a.node").filter(new Locator.FilterOptions()
                    .setHasText("one selected catalog response owns the business comparison")).last();
            assertTrue(scenario.isVisible(), "the generated Extent scenario is visible");
            scenario.click(new Locator.ClickOptions().setTimeout(3000));
            var phase = detail.locator("a.node").filter(new Locator.FilterOptions().setHasText("TEST BODY")).last();
            assertTrue(phase.isVisible(), "the test phase appears after expanding the scenario");
            phase.click(new Locator.ClickOptions().setTimeout(3000));
            var step = detail.locator("a.node").filter(new Locator.FilterOptions()
                    .setHasText("GIVEN the catalog API confirms the configured product before UI purchase")).last();
            assertTrue(step.isVisible(), "the generated Extent step is available to expand");
            step.click(new Locator.ClickOptions().setTimeout(3000));
            var apiValidation = detail.locator("a.node").filter(new Locator.FilterOptions()
                    .setHasText("API validation")).last();
            assertTrue(apiValidation.isVisible(), "API validation appears after expanding the step");
            apiValidation.click(new Locator.ClickOptions().setTimeout(3000));

            var exchange = detail.locator("a.node").filter(new Locator.FilterOptions()
                    .setHasText("GET /api/productsList — Exchange 2")).last();
            assertTrue(exchange.isVisible(), "the selected GET appears after expanding API validation");
            exchange.click(new Locator.ClickOptions().setTimeout(3000));
            var apiAssertions = detail.locator("a.node").filter(new Locator.FilterOptions()
                    .setHasText("API assertions")).last();
            assertTrue(apiAssertions.isVisible(), "the selected GET exposes its assertion group");
            var assertionCard = apiAssertions.locator("xpath=../../..");
            var assertion = assertionCard.locator("table.helm-validation-table td")
                    .filter(new Locator.FilterOptions().setHasText("Catalog result count"));
            assertEquals(1, assertion.count());
            assertFalse(assertion.isVisible(), "the assertion row starts collapsed");
            apiAssertions.click(new Locator.ClickOptions().setTimeout(3000));
            assertTrue(assertion.isVisible(), "expanding API assertions reveals the business comparison");

            var exchangeCard = exchange.locator("xpath=../../..");
            var requestDetails = exchangeCard.locator("details.request-evidence summary");
            assertTrue(requestDetails.isVisible(), "the selected GET exposes its request disclosure");
            requestDetails.click(new Locator.ClickOptions().setTimeout(3000));
            assertTrue(exchangeCard.locator("table.helm-request-table").isVisible());
        }
    }
}

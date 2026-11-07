package atf.ui.tests.extent.support.inspection.flow;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.support.flow.BodyOnlyUnifiedFlowFixture;
import atf.ui.tests.extent.support.locators.flow.BodyOnlyUnifiedFlowLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Observes all original table rows and real disclosure actions for the body-only regression. */
public final class BodyOnlyUnifiedFlowProbe {
    private static final BodyOnlyUnifiedFlowLocators LOCATORS = new BodyOnlyUnifiedFlowLocators();
    private BodyOnlyUnifiedFlowProbe() { }

    public static void verify(boolean fallback, int limit, int width) throws Exception {
        var published = BodyOnlyUnifiedFlowFixture.publish(Files.createTempDirectory("atf-body-flow-browser-"),
                fallback, ReportScreenshotPolicy.EACH_CHECKPOINT, limit, BodyOnlyUnifiedFlowFixture.Variant.SUCCESS);
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value(), new Browser.NewContextOptions().setViewportSize(width, 900)))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(4000);
                var errors = new ArrayList<String>();page.onPageError(errors::add);
                page.navigate(published.path().toUri().toString(),
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                LOCATORS.tests(page, fallback).click();
                if (!fallback) {
                    assertEquals(1, LOCATORS.owner(page).count());
                    LOCATORS.owner(page).click();
                }
                assertThat(LOCATORS.flows(page, fallback)).hasCount(1);
                assertThat(LOCATORS.validations(page, fallback)).hasCount(1);
                assertEquals(1, LOCATORS.nestedValidations(LOCATORS.flows(page, fallback)).count());
                assertFalse(LOCATORS.actions(page, fallback).isVisible(), "Initial parent disclosures are collapsed");
                if (!fallback) LOCATORS.expandAll(page).click();
                int opened = 0;
                while (LOCATORS.closedDetails(page, fallback).count() > 0) {
                    assertTrue(++opened < 20, "Finite source disclosures are opened by their own summary");
                    LOCATORS.summary(LOCATORS.closedDetails(page, fallback).first()).click();
                }
                assertEquals(1, LOCATORS.actions(page, fallback).count());
                assertEquals(1, LOCATORS.observations(page, fallback).count());
                assertEquals(1, LOCATORS.assertions(page, fallback).count());
                assertEquals(1, LOCATORS.compactTrace(page, fallback).count());
                assertEquals(1, LOCATORS.images(page, fallback).count());
                assertEquals(4, LOCATORS.rows(LOCATORS.actions(page, fallback)).count());
                assertEquals(2, LOCATORS.rows(LOCATORS.observations(page, fallback)).count());
                var assertions = LOCATORS.assertions(page, fallback);
                assertThat(assertions).isVisible();
                assertEquals(List.of("Configuration retained", "Product retained", "Quantity retained", "Order retained"),
                        LOCATORS.rows(assertions).all().stream().map(row -> LOCATORS.cells(row).first().innerText()).toList());
                var terminal = LOCATORS.rows(assertions).last();
                assertEquals("v".repeat(limit - 1) + "…", LOCATORS.cells(terminal).nth(1).innerText());
                assertEquals("v".repeat(limit - 1) + "…", LOCATORS.cells(terminal).nth(2).innerText());
                var detail = LOCATORS.assertionDisclosure(assertions);
                assertEquals(1, detail.count(), "Assertions have a real collapsible owner");
                LOCATORS.summary(detail).click();assertThat(assertions).not().isVisible();
                LOCATORS.summary(detail).press("Space");assertThat(assertions).isVisible();
                assertEquals(4, LOCATORS.rows(assertions).count());
                page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                LOCATORS.tests(page, fallback).click();
                if (!fallback) LOCATORS.owner(page).click();
                assertThat(LOCATORS.flows(page, fallback)).hasCount(1);
                assertThat(LOCATORS.validations(page, fallback)).hasCount(1);
                assertTrue(errors.isEmpty(), errors::toString);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}

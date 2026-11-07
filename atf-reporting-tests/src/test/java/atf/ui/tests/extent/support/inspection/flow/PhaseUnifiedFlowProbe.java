package atf.ui.tests.extent.support.inspection.flow;

import atf.ui.tests.extent.support.lifecycle.BrowserResources;

import atf.reporting.support.flow.PhaseUnifiedFlowFixture;
import atf.ui.tests.extent.support.locators.flow.BodyOnlyUnifiedFlowLocators;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.nio.file.Files;
import java.util.ArrayList;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Independent DOM checks for the phase-local path, including fallback disclosures. */
public final class PhaseUnifiedFlowProbe {
    private static final BodyOnlyUnifiedFlowLocators LOCATORS = new BodyOnlyUnifiedFlowLocators();
    private PhaseUnifiedFlowProbe() { }

    public static void verify(boolean fallback, int limit, int width) throws Exception {
        var fixture = PhaseUnifiedFlowFixture.publish(Files.createTempDirectory("atf-phase-flow-browser-"),
                fallback, limit, PhaseUnifiedFlowFixture.Variant.UI);
        try (var playwrightLease = BrowserResources.open(Playwright::create);
             var browserLease = BrowserResources.open(() -> BrowserResources.launch(playwrightLease.value(), new BrowserType.LaunchOptions().setHeadless(true)));
             var pageLease = BrowserResources.open(() -> BrowserResources.newPage(browserLease.value(), new Browser.NewContextOptions().setViewportSize(width, 900)))) {
            try {
                var page = pageLease.value();
                page.setDefaultTimeout(4000);
                var errors = new ArrayList<String>();page.onPageError(errors::add);
                page.navigate(fixture.path().toUri().toString());
                LOCATORS.tests(page, fallback).click();
                if (!fallback) { LOCATORS.owner(page).click();LOCATORS.expandAll(page).click(); }
                assertThat(LOCATORS.flows(page, fallback)).hasCount(1);
                assertThat(LOCATORS.validations(page, fallback)).hasCount(1);
                var assertions = LOCATORS.assertions(page, fallback);
                var detail = LOCATORS.assertionDisclosure(assertions);
                assertEquals(1, detail.count(), "Phase-local assertions require their own collapsible owner");
                assertThat(assertions).not().isVisible();
                LOCATORS.summary(detail).press("Enter");assertThat(assertions).isVisible();
                assertEquals(2, LOCATORS.rows(assertions).count());
                var terminal = LOCATORS.rows(assertions).last();
                assertEquals("v".repeat(limit - 1) + "…", LOCATORS.cells(terminal).nth(1).innerText());
                assertEquals("v".repeat(limit - 1) + "…", LOCATORS.cells(terminal).nth(2).innerText());
                LOCATORS.summary(detail).press("Space");assertThat(assertions).not().isVisible();
                assertEquals(1, LOCATORS.compactTrace(page, fallback).count(), "UI context has a native disclosure");
                int opened = 0;
                while (LOCATORS.closedDetails(page, fallback).count() > 0) {
                    assertTrue(++opened < 20);
                    LOCATORS.summary(LOCATORS.closedDetails(page, fallback).first()).click();
                }
                assertEquals(2, LOCATORS.rows(LOCATORS.actions(page, fallback)).count());
                assertEquals(1, LOCATORS.rows(LOCATORS.observations(page, fallback)).count());
                assertEquals(1, LOCATORS.images(page, fallback).count());
                assertTrue(LOCATORS.compactTrace(page, fallback).innerText().contains("TEST BODY"));
                page.reload();LOCATORS.tests(page, fallback).click();
                if (!fallback) { LOCATORS.owner(page).click();LOCATORS.expandAll(page).click(); }
                assertThat(LOCATORS.assertions(page, fallback)).not().isVisible();
                assertTrue(errors.isEmpty(), errors::toString);
        
            } catch (Exception | Error reportingBrowserFailure) {
                BrowserResources.recordFailure(reportingBrowserFailure);
                throw reportingBrowserFailure;
            }
        }
    }
}

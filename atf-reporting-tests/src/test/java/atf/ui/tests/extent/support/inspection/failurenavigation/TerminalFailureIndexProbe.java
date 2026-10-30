package atf.ui.tests.extent.support.inspection.failurenavigation;

import atf.reporting.support.failurecorrelation.TerminalFailureFixture;
import atf.ui.tests.extent.support.locators.terminal.TerminalFailureLocators;
import com.microsoft.playwright.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public final class TerminalFailureIndexProbe {
    private static final TerminalFailureLocators LOCATORS=new TerminalFailureLocators();
    private TerminalFailureIndexProbe() { }

    public static void verify(boolean fallback,int width,TerminalFailureFixture.Case kind) {
        var output=TerminalFailureFixture.publish(fallback,kind);
        try(var playwright=Playwright.create();
            var browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            var page=browser.newPage(new Browser.NewPageOptions().setViewportSize(width,900))) {
            var errors=new ArrayList<String>();page.onPageError(errors::add);
            page.navigate(output.toUri().toString());LOCATORS.fails(page,fallback).click();
            int expected=kind==TerminalFailureFixture.Case.EXPECTED_PASS?0:kind==TerminalFailureFixture.Case.WITH_ASSERTION?2:1;
            assertEquals(expected,LOCATORS.failures(page,fallback).count());
            if(kind==TerminalFailureFixture.Case.EXPECTED_PASS_WITH_FAILED_SIBLING) {
                assertEquals(0,LOCATORS.executionFailure(page,fallback).count(),"A passing terminal exchange stays excluded inside a failed scenario");
                LOCATORS.failures(page,fallback).first().click();
                if(!fallback) {
                    assertTrue(LOCATORS.detail(page).innerText().contains("GET /after"));
                    assertFalse(LOCATORS.detail(page).innerText().contains("terminal transport canary"));
                }
            } else if(expected>0) {
                var terminal=LOCATORS.executionFailure(page,fallback);assertEquals(1,terminal.count());terminal.click();
                if(!fallback) {
                    assertTrue(LOCATORS.detail(page).innerText().contains("GET /terminal"));
                    assertFalse(LOCATORS.detail(page).innerText().contains("/after"));
                    assertTrue(LOCATORS.detail(page).innerText().contains("terminal transport canary"));
                    assertEquals("execution",LOCATORS.backlink(page).getAttribute("data-atf-failure-kind"));
                    LOCATORS.backlink(page).click();
                }
                var evidence=LOCATORS.terminal(page,fallback);assertEquals(1,evidence.count());
                page.waitForCondition(evidence::isVisible);
                assertTrue(evidence.isVisible(),"Original execution evidence is visible without Expand all");
                assertTrue(evidence.innerText().contains("terminal transport canary"));
                evidence.evaluate("element => element.style.setProperty('display','none','important')");
                assertFalse(evidence.isVisible(),"The oracle rejects a hidden original evidence target");
                page.reload();page.waitForCondition(evidence::isVisible);
                assertTrue(evidence.isVisible(),"Reload reconstructs the execution disclosure path");
                page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Path.of(output+"-"+width+".png")));
            }
            assertTrue(errors.isEmpty(),errors::toString);
        }
    }
}

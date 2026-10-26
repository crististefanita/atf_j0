package atf.autoex.tests.component;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import atf.autoex.support.browser.AutoExPrivacyConsentHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offline component proof for a consent overlay that may reappear mid-journey. */
@AtfComponentTest
class AutoExPrivacyConsentHandlerComponentTest {
    private static final String CONSENT_FIXTURE = """
            <div class="fc-consent-root">
              <button id="manage" type="button" onclick="document.querySelector('#preferences').hidden = false">
                Manage options
              </button>
              <section id="preferences" hidden>
                <input id="purpose-analytics" class="fc-preference-slider-purpose" type="checkbox" checked>
                <label for="purpose-analytics">Optional analytics</label>
                <button id="confirm" type="button"
                        onclick="document.querySelector('.fc-consent-root').hidden = true">
                  Confirm choices
                </button>
              </section>
            </div>
            """;

    @Test
    void visibleConsentOverlayIsReconciledAndEveryDecisionIsTraceable() {
        try (PlaywrightUiSession session = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            var page = session.page();
            page.setContent(CONSENT_FIXTURE);
            var actions = session.newActionSequence();
            var handler = new AutoExPrivacyConsentHandler();

            assertTrue(handler.rejectOptionalProcessingIfPrompted(page, actions));

            assertFalse(page.locator(".fc-consent-root").isVisible());
            assertEquals(0, page.locator(".fc-consent-root input.fc-preference-slider-purpose:checked").count());
            assertEquals(3, actions.snapshot().size(),
                    "manage, opt-out, and confirm must remain in the Playwright action evidence");
            assertFalse(handler.rejectOptionalProcessingIfPrompted(page, actions),
                    "a missing overlay must not add phantom actions");
            assertEquals(3, actions.snapshot().size());
        }
    }
}

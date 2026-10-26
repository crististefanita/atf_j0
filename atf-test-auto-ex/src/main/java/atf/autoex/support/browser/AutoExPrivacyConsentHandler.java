package atf.autoex.support.browser;

import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.autoex.locator.AutoExSelectors;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Re-applies the site's required-only consent choice whenever its dialog returns. */
public final class AutoExPrivacyConsentHandler {
    private static final Pattern SAFE_CONTROL_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

    /** @return true when a visible consent dialog was resolved. */
    public boolean rejectOptionalProcessingIfPrompted(Page page, PlaywrightActionSequence actions) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(actions, "actions");
        Locator dialog = page.locator(AutoExSelectors.PRIVACY_DIALOG);
        if (!dialog.isVisible()) return false;

        actions.click(new AtfLocator(page.locator(AutoExSelectors.PRIVACY_MANAGE_OPTIONS),
                        AutoExSelectors.PRIVACY_MANAGE_OPTIONS),
                "optional privacy purposes are reviewed");

        Locator selectedOptions = page.locator(AutoExSelectors.PRIVACY_SELECTED_OPTIONS);
        int selectedCount = selectedOptions.count();
        for (int index = 0; index < selectedCount; index++) {
            String controlId = selectedOptions.nth(index).getAttribute("id");
            if (controlId == null || !SAFE_CONTROL_ID.matcher(controlId).matches()) {
                throw new IllegalStateException("Consent option has no safe associated label");
            }
            String label = String.format(Locale.ROOT, AutoExSelectors.PRIVACY_OPTION_LABEL, controlId);
            actions.click(new AtfLocator(page.locator(label), label),
                    "a preselected optional privacy purpose is declined");
        }
        if (selectedOptions.count() != 0) {
            throw new IllegalStateException("Optional privacy purposes remain selected");
        }

        actions.click(new AtfLocator(page.locator(AutoExSelectors.PRIVACY_CONFIRM_CHOICES),
                        AutoExSelectors.PRIVACY_CONFIRM_CHOICES),
                "required-only privacy choices are confirmed");
        return true;
    }
}

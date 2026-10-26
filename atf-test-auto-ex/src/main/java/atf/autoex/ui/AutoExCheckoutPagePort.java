package atf.autoex.ui;

import com.microsoft.playwright.Page;

/** Checkout-page capabilities for the instrumented UI and its invoice download. */
public interface AutoExCheckoutPagePort extends AutoExUiActions {
    Page page();
    boolean dismissGoogleVignetteIfPresent();
}

package atf.autoex.ui;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.support.account.AutoExAccountLifecycle;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.reporting.contract.AtfAssertion;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.util.List;

/** Account-page capabilities, including owned-account cleanup and its visible checkpoints. */
public interface AutoExAccountPagePort extends AutoExUiActions {
    String observeCustomerName();
    AutoExConfiguration configuration();
    AutoExAccountLifecycle accountLifecycle();
    Page page();
    String currentUrl();
    void navigate(URI destination);
    void checkpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void onFailureCleanup(AccountCleanup cleanup);
    void clearFailureCleanup();
    boolean dismissGoogleVignetteIfPresent();

    @FunctionalInterface
    interface AccountCleanup { void run() throws Exception; }
}

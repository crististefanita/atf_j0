package atf.autoex.support.journey;

import atf.autoex.support.catalog.AutoExCatalogSnapshot;
import atf.autoex.support.account.AutoExAccountLifecycle;
import atf.autoex.ui.AutoExUiActions;
import atf.autoex.support.trace.AutoExTraceStep;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.config.AutoExConfiguration;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.util.List;

/** Application-facing browser/report contract; the JUnit lifecycle is supplied by the test harness. */
public interface AutoExPurchaseSession extends AutoExUiActions {
    AutoExConfiguration configuration();
    AutoExAccountLifecycle accountLifecycle();
    Page page();
    String currentUrl();
    void navigate(URI uri);
    void checkpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void apiCheckpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void finalScreenshot(AutoExTraceStep step, List<AtfAssertion> assertions);
    AutoExCatalogSnapshot inspectCatalogApi(String productName);
    boolean observesApiTraffic();
    void markCausalApiTrafficBaseline();
    void awaitCartExchange(String productId);
    void onFailureCleanup(AutoExAccountCleanup cleanup);
    void clearFailureCleanup();
    boolean dismissGoogleVignetteIfPresent();
    boolean dismissPrivacyPromptIfPresent();

    @FunctionalInterface
    interface AutoExAccountCleanup { void run() throws Exception; }
}

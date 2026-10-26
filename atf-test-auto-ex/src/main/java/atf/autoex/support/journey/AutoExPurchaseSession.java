package atf.autoex.support.journey;

import atf.autoex.support.catalog.AutoExCatalogSnapshot;
import atf.autoex.support.trace.AutoExTraceStep;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.config.AutoExConfiguration;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.util.List;

/** Application-facing browser/report contract; the JUnit lifecycle is supplied by the test harness. */
public interface AutoExPurchaseSession {
    AutoExConfiguration configuration();
    Page page();
    String currentUrl();
    void navigate(URI uri);
    void click(String selector, String expected);
    void clickFirst(String selector, String expected);
    void clickLast(String selector, String expected);
    void fill(String selector, String value, String expected);
    void waitForText(String selector, String expectedText, String expected);
    String text(String selector, String expected);
    String textFirst(String selector, String expected);
    void select(String selector, String label, String expected);
    boolean visible(String selector);
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

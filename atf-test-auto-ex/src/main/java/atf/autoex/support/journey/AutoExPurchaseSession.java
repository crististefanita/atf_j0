package atf.autoex.support.journey;

import atf.autoex.support.catalog.AutoExCatalogSnapshot;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.autoex.ui.AutoExCheckoutPagePort;
import atf.autoex.ui.AutoExHomePagePort;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.reporting.contract.AtfAssertion;
import java.util.List;

/** Journey facade composing page ports; the JUnit lifecycle is supplied by the test harness. */
public interface AutoExPurchaseSession extends AutoExAccountPagePort,
        AutoExCheckoutPagePort, AutoExHomePagePort {
    void checkpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void apiCheckpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
    void finalScreenshot(AutoExTraceStep step, List<AtfAssertion> assertions);
    AutoExCatalogSnapshot inspectCatalogApi(String productName);
    boolean observesApiTraffic();
    void markCausalApiTrafficBaseline();
    void awaitCartExchange(String productId);
}

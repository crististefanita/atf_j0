package atf.autoex.support.cart;

import atf.autoex.support.journey.AutoExCatalogSearchPort;
import atf.autoex.support.trace.AutoExTraceStep;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.reporting.contract.AtfAssertion;

import java.util.List;

/** UI cart-story capabilities without checkout or API-inspector controls. */
public interface AutoExCartJourneyPort extends AutoExAccountPagePort, AutoExCatalogSearchPort {
    void finalScreenshot(AutoExTraceStep step, List<AtfAssertion> assertions);
}

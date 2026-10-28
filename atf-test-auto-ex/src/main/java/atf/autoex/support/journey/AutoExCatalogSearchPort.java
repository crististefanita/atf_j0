package atf.autoex.support.journey;

import atf.autoex.support.trace.AutoExTraceStep;
import atf.autoex.ui.AutoExHomePagePort;
import atf.autoex.ui.AutoExCatalogPagePort;
import atf.reporting.contract.AtfAssertion;

import java.util.List;

/** Catalog search journey capabilities; it cannot inspect or publish API exchanges. */
public interface AutoExCatalogSearchPort extends AutoExHomePagePort, AutoExCatalogPagePort {
    String currentUrl();
    void checkpoint(AutoExTraceStep step, List<AtfAssertion> assertions);
}

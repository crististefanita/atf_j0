package atf.autoex.support.cart;

import atf.autoex.support.journey.AutoExCatalogSearchPort;
import atf.autoex.support.journey.AutoExAccountJourneyPort;

/** UI cart-story capabilities without checkout or API-inspector controls. */
public interface AutoExCartJourneyPort extends AutoExAccountJourneyPort, AutoExCatalogSearchPort { }

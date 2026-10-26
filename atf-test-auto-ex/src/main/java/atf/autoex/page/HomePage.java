package atf.autoex.page;

import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.journey.AutoExPurchaseSession;

/** Store entry and its primary navigation. */
public final class HomePage extends AutoExPage {
    private final AutoExPurchaseSession run;
    public HomePage(AutoExPurchaseSession run) { super(run); this.run = run; }

    public String open() {
        run.navigate(AutoExRoutes.HOME.resolve(run.configuration().baseUrl()));
        new PrivacyChoicesPage(run).rejectOptionalProcessingIfPrompted();
        return run.textFirst(AutoExSelectors.STORE_HEADING, "Automation Exercise home page is visible");
    }

    public void openProducts() {
        click(AutoExSelectors.PRODUCTS_LINK, "product catalog opens");
    }
}

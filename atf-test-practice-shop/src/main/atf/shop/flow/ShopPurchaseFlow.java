package atf.shop.flow;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopConfiguration;
import atf.shop.locators.ShopLocators;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopHomeScreenValidator;
import atf.shop.support.validation.ShopPageValidator;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.validation.ShopPurchaseScreenValidator;
import com.microsoft.playwright.Page;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public facade for one complete Practice Shop purchase journey.
 * Catalog and checkout transitions are delegated to semantic stages so callers
 * keep the same API while each business boundary can be tested independently.
 */
public final class ShopPurchaseFlow {
    private final ShopLocators locators;
    private final ShopPageValidator pageValidator;
    private final ShopHomeScreenValidator homeScreenValidator;
    private final ShopPurchaseScreenValidator purchaseScreenValidator;
    private final ShopCatalogPurchaseStage catalogStage = new ShopCatalogPurchaseStage();
    private final ShopCheckoutPurchaseStage checkoutStage = new ShopCheckoutPurchaseStage();

    public ShopPurchaseFlow(ShopLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
        this.pageValidator = new ShopPageValidator(locators);
        this.homeScreenValidator = new ShopHomeScreenValidator(locators);
        this.purchaseScreenValidator = new ShopPurchaseScreenValidator(locators);
    }

    public PurchaseResult purchaseConfiguredProduct(Page page, PlaywrightActionSequence actions,
                                                    ShopConfiguration configuration) {
        return purchaseConfiguredProduct(page, actions, configuration, ignored -> { });
    }

    /** Runs the journey while exposing immutable page checkpoints to any evidence sink. */
    public PurchaseResult purchaseConfiguredProduct(Page page, PlaywrightActionSequence actions,
                                                    ShopConfiguration configuration,
                                                    Consumer<ShopPurchaseCheckpoint> checkpointObserver) {
        return execute(page, actions, configuration, checkpointObserver,
                new ShopApiNetworkJournal(
                        atf.shop.config.ShopValidationProfile.UI_ONLY), false);
    }

    /** Same journey after an authenticated setup; the sign-in checkout step is validated explicitly. */
    public PurchaseResult purchaseConfiguredProductAuthenticated(Page page, PlaywrightActionSequence actions,
                                                                  ShopConfiguration configuration,
                                                                  Consumer<ShopPurchaseCheckpoint> checkpointObserver,
                                                                  ShopApiNetworkJournal apiNetwork) {
        return execute(page, actions, configuration, checkpointObserver, apiNetwork, true);
    }

    /** Backward-compatible API-aware guest journey used by the existing E2E proof. */
    public PurchaseResult purchaseConfiguredProduct(Page page, PlaywrightActionSequence actions,
                                                    ShopConfiguration configuration,
                                                    Consumer<ShopPurchaseCheckpoint> checkpointObserver,
                                                    ShopApiNetworkJournal apiNetwork) {
        return execute(page, actions, configuration, checkpointObserver, apiNetwork, false);
    }

    private PurchaseResult execute(Page page, PlaywrightActionSequence actions,
                                   ShopConfiguration configuration,
                                   Consumer<ShopPurchaseCheckpoint> checkpointObserver,
                                   ShopApiNetworkJournal apiNetwork,
                                   boolean authenticated) {
        var context = new ShopPurchaseJourneyContext(page, actions, configuration,
                checkpointObserver, apiNetwork, locators, pageValidator,
                homeScreenValidator, purchaseScreenValidator);
        catalogStage.prepareCart(context);
        return checkoutStage.complete(context, authenticated);
    }

    public record PurchaseResult(String productName, String quantity,
                                 String paymentMethod, String confirmation) {
    }
}

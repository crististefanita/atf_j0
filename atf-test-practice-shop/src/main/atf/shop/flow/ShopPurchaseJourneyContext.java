package atf.shop.flow;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.model.ReportExecutionEvidence;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopCheckoutConfiguration;
import atf.shop.api.ShopApiRoutes;
import atf.shop.locators.ShopLocators;
import atf.shop.routes.ShopUiRoutes;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopHomeScreenValidator;
import atf.shop.support.validation.ShopPageValidationResult;
import atf.shop.support.validation.ShopPageValidator;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.validation.ShopPurchaseScreenValidator;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Immutable per-journey inputs shared by the semantic stages.
 * Checkpoint construction and publication belong to its dedicated publisher.
 */
final class ShopPurchaseJourneyContext {
    private final CatalogInputs catalogInputs;
    private final CheckoutInputs checkoutInputs;

    ShopPurchaseJourneyContext(Page page, PlaywrightActionSequence actions,
                                       ShopConfiguration configuration,
                                       ShopPurchaseCheckpointPublisher checkpointPublisher,
                                       ShopApiNetworkJournal apiNetwork,
                                       ShopLocators locators,
                                       ShopPageValidator pageValidator,
                                       ShopHomeScreenValidator homeScreenValidator,
                                       ShopPurchaseScreenValidator purchaseScreenValidator) {
        ShopConfiguration fixture = Objects.requireNonNull(configuration, "configuration");
        this.catalogInputs = new CatalogInputs(page, actions, fixture.catalogSelection(), checkpointPublisher,
                apiNetwork, locators, pageValidator, homeScreenValidator, purchaseScreenValidator,
                fixture.api().endpoint().resolve(ShopApiRoutes.PRODUCT_COLLECTION));
        this.checkoutInputs = new CheckoutInputs(page, actions, fixture.uri(ShopUiRoutes.CHECKOUT),
                fixture.productName(), fixture.checkout(), checkpointPublisher,
                apiNetwork, locators, pageValidator, purchaseScreenValidator,
                fixture.api().endpoint().resolve(ShopApiRoutes.POSTCODE_LOOKUP));
    }

    CatalogInputs catalogInputs() { return catalogInputs; }
    CheckoutInputs checkoutInputs() { return checkoutInputs; }

    record CatalogInputs(Page page, PlaywrightActionSequence actions,
                         ShopConfiguration.CatalogSelection selection,
                         ShopPurchaseCheckpointPublisher checkpointPublisher,
                         ShopApiNetworkJournal apiNetwork, ShopLocators locators,
                         ShopPageValidator pageValidator, ShopHomeScreenValidator homeScreenValidator,
                         ShopPurchaseScreenValidator purchaseScreenValidator, URI catalogEndpoint) {
        CatalogInputs {
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(actions, "actions");
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(checkpointPublisher, "checkpointPublisher");
            Objects.requireNonNull(apiNetwork, "apiNetwork");
            Objects.requireNonNull(locators, "locators");
            Objects.requireNonNull(pageValidator, "pageValidator");
            Objects.requireNonNull(homeScreenValidator, "homeScreenValidator");
            Objects.requireNonNull(purchaseScreenValidator, "purchaseScreenValidator");
            Objects.requireNonNull(catalogEndpoint, "catalogEndpoint");
        }

        ShopPurchaseCheckpoint checkpoint(ShopPurchaseTraceStep step,
                                          ShopPageValidationResult validation,
                                          List<AtfAssertion> additional,
                                          List<ReportExecutionEvidence> api) {
            return checkpointPublisher.checkpoint(step, validation, additional, api);
        }

        void publish(ShopPurchaseCheckpoint checkpoint) { checkpointPublisher.publish(checkpoint); }

        void assertVisible(AtfLocator... controls) {
            ShopPurchaseJourneyContext.assertVisible(actions, controls);
        }
    }

    record CheckoutInputs(Page page, PlaywrightActionSequence actions, URI cartUrl,
                          String productName, ShopCheckoutConfiguration checkout,
                          ShopPurchaseCheckpointPublisher checkpointPublisher,
                          ShopApiNetworkJournal apiNetwork, ShopLocators locators,
                          ShopPageValidator pageValidator,
                          ShopPurchaseScreenValidator purchaseScreenValidator, URI postcodeEndpoint) {
        CheckoutInputs {
            Objects.requireNonNull(postcodeEndpoint, "postcodeEndpoint");
            Objects.requireNonNull(page, "page");
            Objects.requireNonNull(actions, "actions");
            Objects.requireNonNull(cartUrl, "cartUrl");
            Objects.requireNonNull(productName, "productName");
            Objects.requireNonNull(checkout, "checkout");
            Objects.requireNonNull(checkpointPublisher, "checkpointPublisher");
            Objects.requireNonNull(apiNetwork, "apiNetwork");
            Objects.requireNonNull(locators, "locators");
            Objects.requireNonNull(pageValidator, "pageValidator");
            Objects.requireNonNull(purchaseScreenValidator, "purchaseScreenValidator");
        }

        ShopPurchaseCheckpoint checkpoint(ShopPurchaseTraceStep step,
                                          ShopPageValidationResult validation,
                                          List<AtfAssertion> additional,
                                          List<ReportExecutionEvidence> api) {
            return checkpointPublisher.checkpoint(step, validation, additional, api);
        }

        void publish(ShopPurchaseCheckpoint checkpoint) { checkpointPublisher.publish(checkpoint); }

        void report(ShopPurchaseCheckpoint checkpoint) { checkpointPublisher.report(checkpoint); }
    }

    private static void assertVisible(PlaywrightActionSequence actions, AtfLocator... controls) {
        for (AtfLocator control : controls) {
            actions.assertVisible(new AtfLocator(control.locator().first(), control.selector()));
        }
    }
}

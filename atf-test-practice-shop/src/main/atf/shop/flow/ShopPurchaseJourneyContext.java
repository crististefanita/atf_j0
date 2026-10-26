package atf.shop.flow;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.model.ReportExecutionEvidence;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.shop.config.ShopConfiguration;
import atf.shop.locators.ShopLocators;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.validation.ShopHomeScreenValidator;
import atf.shop.support.validation.ShopPageValidationResult;
import atf.shop.support.validation.ShopPageValidator;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.validation.ShopPurchaseScreenValidator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable composition boundary shared by the semantic purchase stages.
 * It owns reporting mechanics, while the stages own business transitions.
 */
final class ShopPurchaseJourneyContext {
    private final Page page;
    private final PlaywrightActionSequence actions;
    private final ShopConfiguration configuration;
    private final Consumer<ShopPurchaseCheckpoint> checkpointObserver;
    private final ShopApiNetworkJournal apiNetwork;
    private final ShopLocators locators;
    private final ShopPageValidator pageValidator;
    private final ShopHomeScreenValidator homeScreenValidator;
    private final ShopPurchaseScreenValidator purchaseScreenValidator;

    ShopPurchaseJourneyContext(Page page, PlaywrightActionSequence actions,
                                       ShopConfiguration configuration,
                                       Consumer<ShopPurchaseCheckpoint> checkpointObserver,
                                       ShopApiNetworkJournal apiNetwork,
                                       ShopLocators locators,
                                       ShopPageValidator pageValidator,
                                       ShopHomeScreenValidator homeScreenValidator,
                                       ShopPurchaseScreenValidator purchaseScreenValidator) {
        this.page = Objects.requireNonNull(page, "page");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.checkpointObserver = Objects.requireNonNull(checkpointObserver, "checkpointObserver");
        this.apiNetwork = Objects.requireNonNull(apiNetwork, "apiNetwork");
        this.locators = Objects.requireNonNull(locators, "locators");
        this.pageValidator = Objects.requireNonNull(pageValidator, "pageValidator");
        this.homeScreenValidator = Objects.requireNonNull(homeScreenValidator, "homeScreenValidator");
        this.purchaseScreenValidator = Objects.requireNonNull(purchaseScreenValidator, "purchaseScreenValidator");
    }

    Page page() { return page; }
    PlaywrightActionSequence actions() { return actions; }
    ShopConfiguration configuration() { return configuration; }
    ShopApiNetworkJournal apiNetwork() { return apiNetwork; }
    ShopLocators locators() { return locators; }
    ShopPageValidator pageValidator() { return pageValidator; }
    ShopHomeScreenValidator homeScreenValidator() { return homeScreenValidator; }
    ShopPurchaseScreenValidator purchaseScreenValidator() { return purchaseScreenValidator; }

    ShopPurchaseCheckpoint checkpoint(AtfGherkinKeyword keyword, String title,
                                              ShopPageValidationResult validation,
                                              List<AtfAssertion> additional,
                                              List<ReportExecutionEvidence> api) {
        return new ShopPurchaseCheckpoint(keyword, title, validation.assertions())
                .withAdditionalAssertions(additional)
                .withApiEvidence(api);
    }

    ShopPurchaseCheckpoint checkpoint(ShopPurchaseTraceStep step,
                                              ShopPageValidationResult validation,
                                              List<AtfAssertion> additional,
                                              List<ReportExecutionEvidence> api) {
        return checkpoint(step.keyword(), step.title(), validation, additional, api);
    }

    void publish(ShopPurchaseCheckpoint checkpoint) {
        checkpointObserver.accept(checkpoint);
        if (!checkpoint.passed()) {
            throw new IllegalStateException("Page validation failed: " + checkpoint.title());
        }
    }

    void assertVisible(AtfLocator... controls) {
        for (AtfLocator control : controls) {
            actions.assertVisible(new AtfLocator(control.locator().first(), control.selector()));
        }
    }
}

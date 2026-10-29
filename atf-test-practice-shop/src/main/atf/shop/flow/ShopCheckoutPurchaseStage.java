package atf.shop.flow;

import atf.shop.config.ShopScreen;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.model.ShopBillingStrategy;
import atf.shop.locators.ShopControl;
import atf.shop.pages.ShopBillingPage;
import atf.shop.pages.ShopCartPage;
import atf.shop.pages.ShopConfirmationPage;
import atf.shop.pages.ShopGuestCheckoutPage;
import atf.shop.pages.ShopPaymentPage;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.api.validation.ShopApiValidationResult;
import atf.shop.support.validation.ShopPageValidationResult;
import atf.shop.support.validation.ShopOrderExpectation;
import atf.shop.support.validation.ShopPurchaseConfirmation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static atf.reporting.audit.ReportAuditChecks.check;

/** Playwright adapter that observes checkout screens and performs grouped transitions. */
final class ShopCheckoutPurchaseStage implements ShopCheckoutFlow.Port {
    private final ShopPurchaseJourneyContext.CheckoutInputs context;
    private final long cartAdditionFence;
    private final ShopOrderExpectation expectedOrder;

    ShopCheckoutPurchaseStage(ShopPurchaseJourneyContext.CheckoutInputs context, long cartAdditionFence,
                              ShopOrderExpectation expectedOrder) {
        this.context = Objects.requireNonNull(context, "context");
        this.cartAdditionFence = cartAdditionFence;
        this.expectedOrder = Objects.requireNonNull(expectedOrder, "expectedOrder");
    }

    @Override
    public ShopCheckoutFlow.Observation<ShopCartPage.CartLine> observeCart() {
        var page = context.page();
        var checkout = context.checkout();
        ShopCartPage cart = new ShopCartPage(page, context.locators(), context.actions());
        cart.open(context.cartUrl());
        ShopCartPage.CartLine line = cart.assertConfiguredLine(context.productName(), checkout.quantity());
        ShopCartPage.CartAmounts amounts = cart.amounts();
        var cartApiAssertions = context.apiNetwork().cartReady(cartAdditionFence, checkout.quantity());
        var cartApiValidation = ShopApiValidationResult.cart(
                context.apiNetwork().evidence(ShopApiCheckpoint.CART), cartApiAssertions)
                .reportingEvidence();
        var checkpoint = context.checkpoint(ShopPurchaseTraceStep.CART,
                context.pageValidator().validateValues(page, ShopCartPage.class,
                        Map.of(ShopControl.CART_PRODUCT_QUANTITY,
                                Integer.toString(checkout.quantity()))),
                List.of(
                        check("Cart product", context.productName(), line.productName(),
                                line.productName().contains(context.productName())),
                        check("Cart quantity", Integer.toString(checkout.quantity()), line.quantity(),
                                Integer.toString(checkout.quantity()).equals(line.quantity())),
                        check("Cart unit price", expectedOrder.unitPrice().toPlainString(), amounts.unitPrice(),
                                expectedOrder.matchesDisplayedUnitPrice(amounts.unitPrice())),
                        check("Cart line total", expectedOrder.total().toPlainString(), amounts.lineTotal(),
                                expectedOrder.matchesDisplayedTotal(amounts.lineTotal())),
                        check("Cart total", expectedOrder.total().toPlainString(), amounts.total(),
                                expectedOrder.matchesDisplayedTotal(amounts.total()))),
                List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().cart(page))
                .withApiValidation(cartApiValidation);
        return new ShopCheckoutFlow.Observation<>(line, checkpoint);
    }

    @Override
    public void proceedFromCart() {
        new ShopCartPage(context.page(), context.locators(), context.actions()).proceed();
    }

    @Override
    public ShopCheckoutFlow.Observation<Void> observeIdentity(boolean authenticated) {
        var page = context.page();
        ShopPurchaseCheckpoint checkpoint;
        if (authenticated) {
            context.actions().waitForVisible(context.locators().authenticatedProceed(page));
            checkpoint = context.checkpoint(ShopPurchaseTraceStep.AUTHENTICATED_CHECKOUT,
                    new ShopPageValidationResult(ShopScreen.AUTHENTICATED_CHECKOUT,
                            context.purchaseScreenValidator().authenticatedCheckout(page)),
                    List.of(), List.of());
        } else {
            var guest = new ShopGuestCheckoutPage(page, context.locators(), context.actions());
            var identity = context.checkout().guest();
            guest.fillIdentity(identity);
            checkpoint = context.checkpoint(ShopPurchaseTraceStep.GUEST_CHECKOUT,
                    context.pageValidator().validateValues(page, ShopGuestCheckoutPage.class, Map.of(
                            ShopControl.GUEST_EMAIL, identity.email(),
                            ShopControl.GUEST_FIRST_NAME, identity.firstName(),
                            ShopControl.GUEST_LAST_NAME, identity.lastName())),
                    List.of(), List.of())
                    .withAdditionalAssertions(context.purchaseScreenValidator().guestCheckout(page));
        }
        return observation(checkpoint);
    }

    @Override
    public void proceedToBilling(boolean authenticated) {
        var page = context.page();
        if (authenticated) {
            context.actions().click(context.locators().authenticatedProceed(page),
                    "authenticated customer proceeds to billing");
            return;
        }
        var guest = new ShopGuestCheckoutPage(page, context.locators(), context.actions());
        guest.submitIdentity();
        guest.proceedToBilling();
    }

    @Override
    public ShopCheckoutFlow.Observation<Void> observeBilling() {
        var page = context.page();
        var configuration = context.checkout();
        var billing = new ShopBillingPage(page, context.locators(), context.actions());
        ShopPageValidationResult validation = configuration.billingStrategy() == ShopBillingStrategy.CONFIGURED_FIELDS
                ? validateConfiguredBilling(billing, configuration.billing())
                : validateLookupBilling(billing, configuration.billing());
        var checkpoint = context.checkpoint(ShopPurchaseTraceStep.BILLING, validation, List.of(), List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().billing(page));
        return observation(checkpoint);
    }

    @Override
    public void proceedToPayment() {
        new ShopBillingPage(context.page(), context.locators(), context.actions()).proceedToPayment();
    }

    @Override
    public ShopCheckoutFlow.Observation<String> observePaymentMethod() {
        var page = context.page();
        var payment = new ShopPaymentPage(page, context.locators(), context.actions());
        String expected = context.checkout().paymentMethod();
        String observed = payment.chooseMethod(expected);
        var checkpoint = context.checkpoint(ShopPurchaseTraceStep.PAYMENT_METHOD,
                context.pageValidator().validate(page, ShopPaymentPage.class),
                List.of(check("Payment method", expected, observed, expected.equals(observed))), List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().payment(page));
        return new ShopCheckoutFlow.Observation<>(observed, checkpoint);
    }

    @Override
    public ShopCheckoutFlow.Observation<Void> checkPayment() {
        var page = context.page();
        long sequenceFence = context.apiNetwork().sequenceMarker();
        String paymentConfirmation = new ShopPaymentPage(page, context.locators(), context.actions()).checkPayment();
        var apiAssertions = context.apiNetwork().awaitPaymentConfirmed(page, sequenceFence, context.checkout().paymentMethod());
        var apiValidation = ShopApiValidationResult.payment(
                context.apiNetwork().evidence(ShopApiCheckpoint.PAYMENT), apiAssertions).reportingEvidence();
        var checkpoint = context.checkpoint(ShopPurchaseTraceStep.PAYMENT_CHECK,
                new ShopPageValidationResult(ShopScreen.PAYMENT,
                        List.of(context.pageValidator().validateVisible(page, ShopControl.PAYMENT_SUCCESS))),
                List.of(check("Payment check", "Payment was successful", paymentConfirmation,
                        paymentConfirmation.contains("Payment was successful"))), List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().paymentChecked(page))
                .withApiValidation(apiValidation);
        return observation(checkpoint);
    }

    @Override
    public ShopCheckoutFlow.Observation<String> confirmOrder() {
        var page = context.page();
        var confirmationPage = new ShopConfirmationPage(page, context.locators(), context.actions());
        var sequenceFence = context.apiNetwork().requestMarker();
        String confirmation;
        try {
            confirmation = confirmationPage.confirmOrder();
        } catch (RuntimeException | Error primary) {
            try {
                context.apiNetwork().orderCreated(sequenceFence, "", expectedOrder);
            } catch (RuntimeException | Error evidenceFailure) {
                if (evidenceFailure != primary) primary.addSuppressed(evidenceFailure);
            }
            throw primary;
        }
        var apiAssertions = context.apiNetwork().awaitOrderCreated(page, sequenceFence, confirmation, expectedOrder);
        var apiValidation = ShopApiValidationResult.order(
                context.apiNetwork().evidence(ShopApiCheckpoint.ORDER), apiAssertions).reportingEvidence();
        var checkpoint = context.checkpoint(ShopPurchaseTraceStep.ORDER_CONFIRMATION,
                context.pageValidator().validate(page, ShopConfirmationPage.class),
                List.of(check("Order confirmation", "Thanks for your order! and invoice number",
                        confirmation, confirmation.contains("Thanks for your order!")
                                && !ShopPurchaseConfirmation.invoiceNumber(confirmation).isBlank())), List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().orderConfirmation(page))
                .withApiValidation(apiValidation);
        return new ShopCheckoutFlow.Observation<>(confirmation, checkpoint);
    }

    private ShopPageValidationResult validateConfiguredBilling(
            ShopBillingPage billing, ShopBillingAddress address) {
        billing.fill(address);
        return context.pageValidator().validateValues(context.page(), ShopBillingPage.class, Map.of(
                ShopControl.COUNTRY, address.country(),
                ShopControl.POSTAL_CODE, address.postalCode(),
                ShopControl.HOUSE_NUMBER, address.houseNumber(),
                ShopControl.STREET, address.street(),
                ShopControl.CITY, address.city(),
                ShopControl.STATE, address.state()));
    }

    private ShopPageValidationResult validateLookupBilling(
            ShopBillingPage billing, ShopBillingAddress address) {
        billing.fillFromPostalLookup(address);
        return context.pageValidator().validatePopulatedValues(context.page(), ShopBillingPage.class, Map.of(
                        ShopControl.COUNTRY, address.country(),
                        ShopControl.POSTAL_CODE, address.postalCode(),
                        ShopControl.HOUSE_NUMBER, address.houseNumber()),
                ShopControl.STREET, ShopControl.CITY, ShopControl.STATE);
    }

    private static ShopCheckoutFlow.Observation<Void> observation(ShopPurchaseCheckpoint checkpoint) {
        return new ShopCheckoutFlow.Observation<>(null, checkpoint);
    }
}

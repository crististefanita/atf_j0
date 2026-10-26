package atf.shop.flow;

import atf.shop.config.ShopScreen;
import atf.shop.config.model.ShopBillingStrategy;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.locators.ShopControl;
import atf.shop.pages.ShopBillingPage;
import atf.shop.pages.ShopCartPage;
import atf.shop.pages.ShopConfirmationPage;
import atf.shop.pages.ShopGuestCheckoutPage;
import atf.shop.pages.ShopPaymentPage;
import atf.shop.support.validation.ShopPageValidationResult;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.support.ShopPurchaseTraceStep;
import atf.shop.support.api.validation.ShopApiValidationResult;

import java.util.List;
import java.util.Map;

import static atf.reporting.audit.ReportAuditChecks.check;

/** Checkout-side business stage: cart, identity, billing, payment and confirmation. */
final class ShopCheckoutPurchaseStage {
    ShopPurchaseFlow.PurchaseResult complete(ShopPurchaseJourneyContext context,
                                                      boolean authenticated) {
        var page = context.page();
        var actions = context.actions();
        var locators = context.locators();
        var configuration = context.configuration();

        ShopCartPage cart = new ShopCartPage(page, locators, actions);
        cart.open(configuration);
        ShopCartPage.CartLine line = cart.assertConfiguredLine(configuration);
        var cartApiAssertions = context.apiNetwork().cartReady(configuration.checkout().quantity());
        var cartApiValidation = ShopApiValidationResult.cart(
                context.apiNetwork().evidence(ShopApiCheckpoint.CART), cartApiAssertions)
                .reportingEvidence();
        context.publish(context.checkpoint(ShopPurchaseTraceStep.CART,
                context.pageValidator().validateValues(page, ShopCartPage.class,
                        Map.of(ShopControl.CART_PRODUCT_QUANTITY,
                                Integer.toString(configuration.checkout().quantity()))),
                List.of(
                        check("Cart product", configuration.productName(), line.productName(),
                                line.productName().contains(configuration.productName())),
                        check("Cart quantity", Integer.toString(configuration.checkout().quantity()), line.quantity(),
                                Integer.toString(configuration.checkout().quantity()).equals(line.quantity()))),
                List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().cart(page))
                .withApiValidation(cartApiValidation));
        cart.proceed();

        if (authenticated) {
            actions.waitForVisible(locators.authenticatedProceed(page));
            context.publish(context.checkpoint(ShopPurchaseTraceStep.AUTHENTICATED_CHECKOUT,
                    new ShopPageValidationResult(
                            ShopScreen.AUTHENTICATED_CHECKOUT,
                            context.purchaseScreenValidator().authenticatedCheckout(page)),
                    List.of(), List.of()));
            actions.click(locators.authenticatedProceed(page), "authenticated customer proceeds to billing");
        } else {
            ShopGuestCheckoutPage guest = new ShopGuestCheckoutPage(page, locators, actions);
            var guestIdentity = configuration.checkout().guest();
            guest.fillIdentity(guestIdentity);
            context.publish(context.checkpoint(ShopPurchaseTraceStep.GUEST_CHECKOUT,
                    context.pageValidator().validateValues(page, ShopGuestCheckoutPage.class, Map.of(
                            ShopControl.GUEST_EMAIL, guestIdentity.email(),
                            ShopControl.GUEST_FIRST_NAME, guestIdentity.firstName(),
                            ShopControl.GUEST_LAST_NAME, guestIdentity.lastName())),
                    List.of(), List.of())
                    .withAdditionalAssertions(context.purchaseScreenValidator().guestCheckout(page)));
            guest.submitIdentity();
            guest.proceedToBilling();
        }

        ShopBillingPage billing = new ShopBillingPage(page, locators, actions);
        var address = configuration.checkout().billing();
        var billingValidation = configuration.checkout().billingStrategy() == ShopBillingStrategy.CONFIGURED_FIELDS
                ? validateConfiguredBilling(billing, context, address)
                : validateLookupBilling(billing, context, address);
        context.publish(context.checkpoint(ShopPurchaseTraceStep.BILLING,
                billingValidation,
                List.of(), List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().billing(page)));
        billing.proceedToPayment();

        ShopPaymentPage payment = new ShopPaymentPage(page, locators, actions);
        String observedPaymentMethod = payment.chooseMethod(configuration.checkout().paymentMethod());
        context.publish(context.checkpoint(ShopPurchaseTraceStep.PAYMENT_METHOD,
                context.pageValidator().validate(page, ShopPaymentPage.class),
                List.of(check("Payment method", configuration.checkout().paymentMethod(),
                        observedPaymentMethod,
                        configuration.checkout().paymentMethod().equals(observedPaymentMethod))), List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().payment(page)));
        String paymentConfirmation = payment.checkPayment();
        var paymentApiAssertions = context.apiNetwork().paymentConfirmed(
                configuration.checkout().paymentMethod());
        var paymentApiValidation = ShopApiValidationResult.payment(
                context.apiNetwork().evidence(ShopApiCheckpoint.PAYMENT), paymentApiAssertions)
                .reportingEvidence();
        context.publish(context.checkpoint(ShopPurchaseTraceStep.PAYMENT_CHECK,
                new ShopPageValidationResult(ShopScreen.PAYMENT,
                        List.of(context.pageValidator().validateVisible(page, ShopControl.PAYMENT_SUCCESS))),
                List.of(check("Payment check", "Payment was successful", paymentConfirmation,
                        paymentConfirmation.contains("Payment was successful"))),
                List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().paymentChecked(page))
                .withApiValidation(paymentApiValidation));

        ShopConfirmationPage orderConfirmationPage =
                new ShopConfirmationPage(page, locators, actions);
        long orderSequenceFence = context.apiNetwork().sequenceMarker();
        String confirmation = orderConfirmationPage.confirmOrder();
        var orderApiAssertions = context.apiNetwork().orderCreated(orderSequenceFence, confirmation);
        var orderApiValidation = ShopApiValidationResult.order(
                context.apiNetwork().evidence(ShopApiCheckpoint.ORDER), orderApiAssertions)
                .reportingEvidence();
        context.publish(context.checkpoint(ShopPurchaseTraceStep.ORDER_CONFIRMATION,
                context.pageValidator().validate(page, ShopConfirmationPage.class),
                List.of(check("Order confirmation", "Thanks for your order! and invoice number",
                        confirmation, confirmation.contains("Thanks for your order!")
                                && confirmation.contains("INV-"))),
                List.of())
                .withAdditionalAssertions(context.purchaseScreenValidator().orderConfirmation(page))
                .withApiValidation(orderApiValidation));
        return new ShopPurchaseFlow.PurchaseResult(line.productName(), line.quantity(),
                configuration.checkout().paymentMethod(), confirmation);
    }

    private static ShopPageValidationResult validateConfiguredBilling(
            ShopBillingPage billing, ShopPurchaseJourneyContext context,
            ShopBillingAddress address) {
        billing.fill(address);
        return context.pageValidator().validateValues(context.page(), ShopBillingPage.class, Map.of(
                ShopControl.COUNTRY, address.country(),
                ShopControl.POSTAL_CODE, address.postalCode(),
                ShopControl.HOUSE_NUMBER, address.houseNumber(),
                ShopControl.STREET, address.street(),
                ShopControl.CITY, address.city(),
                ShopControl.STATE, address.state()));
    }

    private static ShopPageValidationResult validateLookupBilling(
            ShopBillingPage billing, ShopPurchaseJourneyContext context,
            ShopBillingAddress address) {
        billing.fillFromPostalLookup(address);
        return context.pageValidator().validatePopulatedValues(context.page(), ShopBillingPage.class,
                Map.of(ShopControl.COUNTRY, address.country(),
                        ShopControl.POSTAL_CODE, address.postalCode(),
                        ShopControl.HOUSE_NUMBER, address.houseNumber()),
                ShopControl.STREET, ShopControl.CITY, ShopControl.STATE);
    }
}

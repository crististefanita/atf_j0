package atf.shop.tests.api.component.exchange;

import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.ShopApiScreenFlow;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.api.screen.ShopCheckoutApi.AddressLookupPolicy;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiCallComponentTest;
import atf.shop.tests.api.support.ShopApiCallAuditContextTestSupport;
import org.junit.jupiter.api.Test;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertNotNull;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Verifies one reusable precondition and four screen boundaries without a browser. */
@ShopApiCallComponentTest
class ShopApiScreenFlowTest {
    @Test
    void loginPreconditionFeedsCatalogCartAndCheckoutScreens() {
        ShopApiClient client = ShopApiComponentFixture.client();
        ShopApiScreenFlow flow = new ShopApiScreenFlow(client);
        String productName = ShopApiScenarioFixture.PRODUCT_NAME;
        String postcode = ShopApiScenarioFixture.POSTCODE;
        String country = ShopApiScenarioFixture.COUNTRY;
        String houseNumber = ShopApiScenarioFixture.HOUSE_NUMBER;
        int quantity = ShopApiScenarioFixture.QUANTITY;

        ShopApiCallAuditContextTestSupport.expectNext("login establishes a valid session",
                ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext("catalog search returns the configured product",
                ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext("catalog detail returns the selected product",
                ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext("cart creation returns a new cart",
                ShopApiHttpStatus.CREATED);
        ShopApiCallAuditContextTestSupport.expectNext("cart add accepts the configured product",
                ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext("cart read returns the prepared cart",
                ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext("postcode lookup returns address details",
                ShopApiHttpStatus.OK);
        ShopApiCallAuditContextTestSupport.expectNext("payment check confirms the selected method",
                ShopApiHttpStatus.OK);

        var session = flow.login().establish(ShopApiScenarioFixture.validCredentials());
        var catalog = flow.catalog().openProduct(productName);
        var cart = flow.cart().prepare(catalog.productId(), quantity);
        var checkout = flow.checkout().confirm(country, postcode, houseNumber,
                ShopPaymentMethod.CASH_ON_DELIVERY);

        assertTrue(session.authenticated());
        assertTrue(ShopApiHttpStatus.OK.matches(session.loginResult().response().status()));
        assertEquals(ShopApiScenarioFixture.PRODUCT_ID, catalog.productId());
        assertTrue(ShopApiHttpStatus.OK.matches(catalog.search().response().status()));
        assertTrue(ShopApiHttpStatus.OK.matches(catalog.detail().response().status()));
        assertEquals(ShopApiScenarioFixture.CART_ID, cart.cartId());
        assertTrue(ShopApiHttpStatus.CREATED.matches(cart.create().response().status()));
        assertTrue(ShopApiHttpStatus.OK.matches(cart.add().response().status()));
        var cartResponse = cart.read().response();
        var paymentResponse = checkout.payment().response();
        var addressResponse = checkout.addressLookup().response();
        var addressBody = ShopJsonEvidence.parse(addressResponse.body());
        var searchBody = ShopJsonEvidence.parse(catalog.search().response().body());
        var detailBody = ShopJsonEvidence.parse(catalog.detail().response().body());
        var cartBody = ShopJsonEvidence.parse(cartResponse.body());
        var paymentBody = ShopJsonEvidence.parse(paymentResponse.body());

        assertTrue(ShopApiHttpStatus.OK.matches(cartResponse.status()));
        assertTrue(ShopApiHttpStatus.OK.matches(paymentResponse.status()));
        assertTrue(ShopApiHttpStatus.OK.matches(addressResponse.status()));
        assertNotNull(searchBody);
        var searchProducts = searchBody.path(ShopApiJsonField.DATA.wireValue());
        assertEquals(1, searchProducts.size());
        assertEquals(ShopApiScenarioFixture.PRODUCT_ID,
                searchProducts.get(0).path(ShopApiJsonField.ID.wireValue()).asText());
        assertEquals(productName,
                searchProducts.get(0).path(ShopApiJsonField.NAME.wireValue()).asText());
        assertNotNull(detailBody);
        assertEquals(ShopApiScenarioFixture.PRODUCT_ID,
                detailBody.path(ShopApiJsonField.ID.wireValue()).asText());
        assertEquals(productName,
                detailBody.path(ShopApiJsonField.NAME.wireValue()).asText());
        assertNotNull(cartBody);
        var cartItems = cartBody.path(ShopApiJsonField.CART_ITEMS.wireValue());
        assertEquals(1, cartItems.size());
        assertEquals(catalog.productId(), cartItems.get(0)
                .path(ShopApiJsonField.PRODUCT_ID.wireValue()).asText());
        assertEquals(quantity, cartItems.get(0)
                .path(ShopApiJsonField.QUANTITY.wireValue()).asInt());
        assertNotNull(paymentBody);
        assertEquals(ShopApiScenarioFixture.PAYMENT_SUCCESS_MESSAGE,
                paymentBody.path(ShopApiJsonField.MESSAGE.wireValue()).asText());
        assertEquals(AddressLookupPolicy.ADVISORY, checkout.addressLookupPolicy());
        assertEquals(postcode, checkout.requestedAddress().postcode());
        assertNotNull(addressBody);
        assertEquals(country, addressBody.path(ShopApiJsonField.COUNTRY.wireValue()).asText());
        assertEquals(postcode, addressBody.path(ShopApiJsonField.POSTCODE.wireValue()).asText());
    }
}

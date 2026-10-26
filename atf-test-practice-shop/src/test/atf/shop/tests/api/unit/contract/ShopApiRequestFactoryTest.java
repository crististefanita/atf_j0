package atf.shop.tests.api.unit.contract;

import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.testkit.AtfUnitTest;
import atf.core.http.auth.AuthProfiles;
import atf.core.http.auth.AuthType;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.contract.ShopApiQueryParameter;
import atf.shop.api.ShopApiRequestFactory;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.api.ShopProductQuery;
import atf.shop.api.contract.ShopProductSort;
import atf.shop.config.model.ShopCredentials;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ShopApiRequestFactoryTest {
    private final ShopApiRequestFactory requests = new ShopApiRequestFactory();

    @Test
    void loginIsAJsonWriteWithNoAuthenticationPrecondition() {
        ShopCredentials credentials = ShopApiScenarioFixture.validCredentials();
        ApiRequest request = requests.login(credentials);

        assertEquals(HttpMethod.POST.value(), request.method());
        assertEquals(ShopApiRoutes.LOGIN, request.path());
        assertEquals(AuthProfiles.NONE, request.authProfile());
        assertEquals(MediaTypes.APPLICATION_JSON, request.headers().get(HttpHeader.CONTENT_TYPE.value()));
        var body = ShopJsonEvidence.parse(request.bodyText());
        assertNotNull(body);
        assertEquals(ShopApiScenarioFixture.VALID_EMAIL,
                body.path(ShopApiJsonField.EMAIL.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.VALID_PASSWORD,
                body.path(ShopApiJsonField.PASSWORD.wireValue()).asText());
    }

    @Test
    void registrationAndGuestInvoiceBodiesSerializeTheirTypedFieldsAsSnakeCaseJson() {
        ApiRequest registration = requests.register(ShopApiScenarioFixture.validCredentials());
        ApiRequest invoice = requests.invoice(ShopApiScenarioFixture.validInvoice(),
                ShopApiScenarioFixture.BEARER_TOKEN);
        ApiRequest guestInvoice = requests.guestInvoice(ShopApiScenarioFixture.validGuestInvoice());
        var registrationBody = ShopJsonEvidence.parse(registration.bodyText());
        var authenticatedInvoiceBody = ShopJsonEvidence.parse(invoice.bodyText());
        var invoiceBody = ShopJsonEvidence.parse(guestInvoice.bodyText());

        assertEquals(ShopApiRoutes.USER_REGISTER, registration.path());
        assertNotNull(registrationBody);
        assertEquals(ShopApiScenarioFixture.VALID_FIRST_NAME,
                registrationBody.path(ShopApiJsonField.FIRST_NAME.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.VALID_EMAIL,
                registrationBody.path(ShopApiJsonField.EMAIL.wireValue()).asText());
        assertNull(registrationBody.get("firstName"));

        assertEquals(ShopApiRoutes.INVOICE_CREATE, invoice.path());
        assertEquals(AuthProfiles.standard(AuthType.BEARER), invoice.authProfile());
        assertEquals("Bearer " + ShopApiScenarioFixture.BEARER_TOKEN,
                invoice.headers().get(HttpHeader.AUTHORIZATION.value()));
        assertNotNull(authenticatedInvoiceBody);
        assertNull(authenticatedInvoiceBody.get(ShopApiJsonField.GUEST_EMAIL.wireValue()));

        assertEquals(ShopApiRoutes.GUEST_INVOICE_CREATE, guestInvoice.path());
        assertNotNull(invoiceBody);
        assertEquals(ShopApiScenarioFixture.CART_ID,
                invoiceBody.path(ShopApiJsonField.CART_ID.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.VALID_EMAIL,
                invoiceBody.path(ShopApiJsonField.GUEST_EMAIL.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.VALID_FIRST_NAME,
                invoiceBody.path(ShopApiJsonField.GUEST_FIRST_NAME.wireValue()).asText());
        assertNull(invoiceBody.get("guestEmail"));
    }

    @ParameterizedTest
    @EnumSource(ShopPaymentMethod.class)
    void paymentVariantsRemainJsonAndUseTheConfiguredWireValue(ShopPaymentMethod method) {
        ApiRequest request = requests.payment(method);

        assertEquals(HttpMethod.POST.value(), request.method());
        assertEquals(ShopApiRoutes.PAYMENT_CHECK, request.path());
        assertEquals(MediaTypes.APPLICATION_JSON, request.headers().get(HttpHeader.CONTENT_TYPE.value()));
        var body = ShopJsonEvidence.parse(request.bodyText());
        assertNotNull(body);
        assertEquals(method.wireValue(),
                body.path(ShopApiJsonField.PAYMENT_METHOD.wireValue()).asText());
    }

    @Test
    void invalidQuantityFailsBeforeAWriteCanReachTheCart() {
        assertThrows(IllegalArgumentException.class,
                () -> requests.addToCart(ShopApiScenarioFixture.MUTATION_CART_ID,
                        ShopApiScenarioFixture.MUTATION_PRODUCT_ID, 0));
    }

    @Test
    void queryVariantsAreKeptTypedAndEncodedByTheCoreUrlBuilder() {
        ApiRequest request = requests.postcodeLookup(ShopApiScenarioFixture.COUNTRY,
                ShopApiScenarioFixture.POSTCODE, ShopApiScenarioFixture.HOUSE_NUMBER);

        assertEquals(HttpMethod.GET.value(), request.method());
        assertEquals(ShopApiScenarioFixture.COUNTRY,
                request.query().get(ShopApiQueryParameter.COUNTRY.wireValue()));
        assertEquals(ShopApiScenarioFixture.POSTCODE,
                request.query().get(ShopApiQueryParameter.POSTCODE.wireValue()));
        assertEquals(ShopApiScenarioFixture.HOUSE_NUMBER,
                request.query().get(ShopApiQueryParameter.HOUSE_NUMBER.wireValue()));
    }

    @Test
    void productSearchUsesTheRealHttpGetVerbAndKeepsSearchAsBusinessIntent() {
        ApiRequest request = requests.search(ShopApiScenarioFixture.PRODUCT_NAME);

        assertEquals(HttpMethod.GET.value(), request.method());
        assertEquals(HttpMethod.GET.value(), ShopApiOperation.SEARCH.wireValue());
        assertEquals(ShopApiRoutes.PRODUCT_SEARCH, request.path());
    }

    @Test
    void catalogFiltersAreTypedAndKeepTheDocumentedQueryVocabulary() {
        ApiRequest request = requests.products(new ShopProductQuery(
                ShopApiScenarioFixture.FILTER_BRAND_ID, ShopApiScenarioFixture.CATEGORY_ID,
                ShopApiScenarioFixture.FILTER_RENTAL, ShopApiScenarioFixture.FILTER_PRICE_RANGE,
                ShopProductSort.PRICE_ASC, ShopApiScenarioFixture.FILTER_PAGE));

        assertEquals(HttpMethod.GET.value(), request.method());
        assertEquals(ShopApiRoutes.PRODUCT_COLLECTION, request.path());
        assertEquals(ShopApiScenarioFixture.FILTER_BRAND_ID,
                request.query().get(ShopApiQueryParameter.BRAND.wireValue()));
        assertEquals(ShopApiScenarioFixture.CATEGORY_ID,
                request.query().get(ShopApiQueryParameter.CATEGORY.wireValue()));
        assertEquals(Boolean.toString(ShopApiScenarioFixture.FILTER_RENTAL),
                request.query().get(ShopApiQueryParameter.RENTAL.wireValue()));
        assertEquals(ShopApiScenarioFixture.FILTER_PRICE_RANGE,
                request.query().get(ShopApiQueryParameter.PRICE_RANGE.wireValue()));
        assertEquals(ShopProductSort.PRICE_ASC.wireValue(),
                request.query().get(ShopApiQueryParameter.SORT.wireValue()));
        assertEquals(Integer.toString(ShopApiScenarioFixture.FILTER_PAGE),
                request.query().get(ShopApiQueryParameter.PAGE.wireValue()));
    }

    @Test
    void cartUpdateIsPutJsonAndRemoveHasAnExplicitProductRoute() {
        String cartId = ShopApiScenarioFixture.MUTATION_CART_ID;
        String productId = ShopApiScenarioFixture.MUTATION_PRODUCT_ID;
        int expectedQuantity = ShopApiScenarioFixture.UPDATED_QUANTITY;

        ApiRequest update = requests.updateCartQuantity(cartId, productId, expectedQuantity);
        ApiRequest remove = requests.removeFromCart(cartId, productId);

        assertEquals(HttpMethod.PUT.value(), update.method());
        assertEquals(ShopApiRoutes.updateCartQuantity(cartId), update.path());
        assertEquals(MediaTypes.APPLICATION_JSON, update.headers().get(HttpHeader.CONTENT_TYPE.value()));
        var updateBody = ShopJsonEvidence.parse(update.bodyText());
        assertNotNull(updateBody);
        assertEquals(productId,
                updateBody.path(ShopApiJsonField.PRODUCT_ID.wireValue()).asText());
        assertEquals(expectedQuantity,
                updateBody.path(ShopApiJsonField.QUANTITY.wireValue()).asInt());
        assertEquals(HttpMethod.DELETE.value(), remove.method());
        assertEquals(ShopApiRoutes.removeCartProduct(cartId, productId), remove.path());
        assertEquals("", remove.bodyText());
    }
}

package atf.shop.tests.api.component.contract;

import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.ShopApiRequestFactory;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiCallComponentTest;
import atf.shop.tests.api.support.ShopApiCallAuditContextTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertNotNull;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

/** Small contract matrix: few cases, deliberately different failure boundaries. */
@ShopApiCallComponentTest
class ShopApiContractVariantTest {
    private static final ShopApiRequestFactory REQUESTS = new ShopApiRequestFactory();

    @Test
    void registrationResponseIdentifiesTheConfiguredAccount() {
        ShopApiClient client = ShopApiComponentFixture.client();
        ShopApiCallAuditContextTestSupport.expectNext("registration returns configured account",
                ShopApiHttpStatus.CREATED);

        var response = client.execute(REQUESTS.register(ShopApiScenarioFixture.validCredentials()))
                .response();
        var body = ShopJsonEvidence.parse(response.body());

        assertTrue(ShopApiHttpStatus.CREATED.matches(response.status()));
        assertNotNull(body);
        assertEquals(ShopApiScenarioFixture.REGISTERED_USER_ID,
                body.path(ShopApiJsonField.ID.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.VALID_EMAIL,
                body.path(ShopApiJsonField.EMAIL.wireValue()).asText());
    }

    @Test
    void guestInvoiceResponseIdentifiesTheCreatedOrderAndInvoice() {
        ShopApiClient client = ShopApiComponentFixture.client();
        ShopApiCallAuditContextTestSupport.expectNext("guest invoice returns created order identity",
                ShopApiHttpStatus.OK);

        var response = client.execute(REQUESTS.guestInvoice(
                ShopApiScenarioFixture.validGuestInvoice())).response();
        var body = ShopJsonEvidence.parse(response.body());

        assertTrue(ShopApiHttpStatus.OK.matches(response.status()));
        assertNotNull(body);
        assertEquals(ShopApiScenarioFixture.CREATED_ORDER_ID,
                body.path(ShopApiJsonField.ID.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.CREATED_INVOICE_NUMBER,
                body.path(ShopApiJsonField.INVOICE_NUMBER.wireValue()).asText());
        assertEquals(ShopApiScenarioFixture.CREATED_ORDER_TOTAL,
                body.path(ShopApiJsonField.TOTAL.wireValue()).asDouble(), 0.001);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    void preservesPositiveAndNegativeApiShapes(String name, ShopApiHttpStatus expectedStatus,
                                               String expectedBody, ApiRequestSupplier request) {
        ShopApiClient client = ShopApiComponentFixture.client();

        ApiRequest apiRequest = request.create();
        ShopApiCallAuditContextTestSupport.expectNext(name, expectedStatus);
        var result = client.execute(apiRequest);
        var response = result.response();

        assertTrue(expectedStatus.matches(response.status()));
        assertEquals(expectedBody, response.body());
        assertTrue(response.header(HttpHeader.CONTENT_TYPE.value()).contains(MediaTypes.APPLICATION_JSON));
    }

    private static Stream<Arguments> variants() {
        return Stream.of(
                Arguments.of("invalid login is unauthorized", ShopApiHttpStatus.UNAUTHORIZED,
                        ShopApiScenarioFixture.UNAUTHORIZED_BODY,
                        (ApiRequestSupplier) () -> REQUESTS.login(
                                ShopApiScenarioFixture.invalidCredentials())),
                Arguments.of("missing product is not found", ShopApiHttpStatus.NOT_FOUND,
                        ShopApiScenarioFixture.NOT_FOUND_BODY,
                        (ApiRequestSupplier) () -> REQUESTS.product(
                                ShopApiScenarioFixture.MISSING_PRODUCT_ID, "")),
                Arguments.of("cash payment returns a JSON confirmation", ShopApiHttpStatus.OK,
                        ShopApiScenarioFixture.PAYMENT_SUCCESS_BODY,
                        (ApiRequestSupplier) () -> REQUESTS.payment(ShopPaymentMethod.CASH_ON_DELIVERY)));
    }

    @FunctionalInterface
    private interface ApiRequestSupplier { ApiRequest create(); }
}

package atf.autoex.tests.component.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.api.AutoExApiAccount;
import atf.autoex.api.AutoExApiOutcome;
import atf.autoex.api.AutoExApiJsonField;
import atf.autoex.api.AutoExApiRequestFactory;
import atf.autoex.api.AutoExApiRoute;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static atf.core.http.request.HttpMethod.DELETE;
import static atf.core.http.request.HttpMethod.GET;
import static atf.core.http.request.HttpMethod.POST;
import static atf.core.http.request.HttpMethod.PUT;
import static atf.core.http.request.MediaTypes.APPLICATION_FORM_URLENCODED;
import static atf.autoex.tests.support.AutoExApiFixture.BASE_URI;
import static atf.autoex.api.AutoExApiOutcome.ACCOUNT_DELETED;
import static atf.autoex.api.AutoExApiOutcome.LOGIN_PARAMETER_MISSING;
import static atf.autoex.api.AutoExApiOutcome.METHOD_NOT_SUPPORTED;
import static atf.autoex.api.AutoExApiOutcome.SEARCH_PARAMETER_MISSING;
import static atf.autoex.api.AutoExApiOutcome.USER_CREATED;
import static atf.autoex.api.AutoExApiOutcome.USER_EXISTS;
import static atf.autoex.api.AutoExApiOutcome.USER_NOT_FOUND;
import static atf.autoex.api.AutoExApiOutcome.USER_UPDATED;
import static atf.testkit.assertions.AuditedAssertions.assertAll;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;

/** Executes the consumer request catalog through the core executor and a deterministic offline transport. */
@AutoExComponentTest
class AutoExApiContractComponentTest {
    private static final int FIXTURE_HTTP_STATUS = 200;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String EMAIL = "atf-autoex-contract@example.test";
    private static final String PASSWORD = "synthetic-contract-password";
    private static final AutoExApiRequestFactory REQUESTS = new AutoExApiRequestFactory();
    private static final AutoExApiAccount ACCOUNT = new AutoExApiAccount(
            "ATF Contract", EMAIL, PASSWORD, "Mr", "10", "5", "1990", "ATF", "Contract",
            "ATF", "1 Example Street", "", "United States", "10001", "NY", "New York", "5550100");
    private static final Map<String, String> EXPECTED_ACCOUNT_FIELDS = Map.ofEntries(
            Map.entry("name", "ATF Contract"), Map.entry("email", EMAIL),
            Map.entry("password", PASSWORD), Map.entry("title", "Mr"),
            Map.entry("birth_date", "10"), Map.entry("birth_month", "5"),
            Map.entry("birth_year", "1990"), Map.entry("firstname", "ATF"),
            Map.entry("lastname", "Contract"), Map.entry("company", "ATF"),
            Map.entry("address1", "1 Example Street"), Map.entry("address2", ""),
            Map.entry("country", "United States"), Map.entry("zipcode", "10001"),
            Map.entry("state", "NY"), Map.entry("city", "New York"),
            Map.entry("mobile_number", "5550100"));

    @Test
    void allFourteenScenariosKeepFixtureTransportAndDocumentedJsonContractsDistinctOffline() {
        List<ContractCase> cases = List.of(
                contract("API 1 products list", REQUESTS.productsList(), GET, 200, Map.of(), Map.of(),
                        AutoExApiRoute.PRODUCTS_LIST, AutoExApiJsonField.PRODUCTS, null,
                        "{\"responseCode\":200,\"products\":[]}"),
                contract("API 2 unsupported products POST", REQUESTS.postProductsList(), POST, METHOD_NOT_SUPPORTED,
                        Map.of(), Map.of(),
                        AutoExApiRoute.PRODUCTS_LIST,
                        "{\"responseCode\":405,\"message\":\"This request method is not supported.\"}"),
                contract("API 3 brands list", REQUESTS.brandsList(), GET, 200, Map.of(), Map.of(),
                        AutoExApiRoute.BRANDS_LIST, AutoExApiJsonField.BRANDS, null,
                        "{\"responseCode\":200,\"brands\":[]}"),
                contract("API 4 unsupported brands PUT", REQUESTS.putBrandsList(), PUT, METHOD_NOT_SUPPORTED,
                        Map.of(), Map.of(),
                        AutoExApiRoute.BRANDS_LIST,
                        "{\"responseCode\":405,\"message\":\"This request method is not supported.\"}"),
                contract("API 5 product search", REQUESTS.searchProduct("top"), POST, 200,
                        Map.of("search_product", "top"), Map.of(),
                        AutoExApiRoute.SEARCH_PRODUCT, AutoExApiJsonField.PRODUCTS, null,
                        "{\"responseCode\":200,\"products\":[]}"),
                contract("API 6 search parameter missing", REQUESTS.searchProduct(null), POST, SEARCH_PARAMETER_MISSING,
                        Map.of(), Map.of(),
                        AutoExApiRoute.SEARCH_PRODUCT,
                        "{\"responseCode\":400,\"message\":\"Bad request, search_product parameter is missing in POST request.\"}"),
                contract("API 7 valid login", REQUESTS.verifyLogin(EMAIL, PASSWORD), POST, USER_EXISTS,
                        Map.of("email", EMAIL, "password", PASSWORD), Map.of(),
                        AutoExApiRoute.VERIFY_LOGIN,
                        "{\"responseCode\":200,\"message\":\"User exists!\"}"),
                contract("API 8 email missing", REQUESTS.verifyLoginWithoutEmail(PASSWORD), POST, LOGIN_PARAMETER_MISSING,
                        Map.of("password", PASSWORD), Map.of(),
                        AutoExApiRoute.VERIFY_LOGIN,
                        "{\"responseCode\":400,\"message\":\"Bad request, email or password parameter is missing in POST request.\"}"),
                contract("API 9 unsupported login DELETE", REQUESTS.deleteMethodOnVerifyLogin(), DELETE, METHOD_NOT_SUPPORTED,
                        Map.of(), Map.of(),
                        AutoExApiRoute.VERIFY_LOGIN,
                        "{\"responseCode\":405,\"message\":\"This request method is not supported.\"}"),
                contract("API 10 invalid login", REQUESTS.verifyLogin("missing@example.test", "wrong"), POST, USER_NOT_FOUND,
                        Map.of("email", "missing@example.test", "password", "wrong"), Map.of(),
                        AutoExApiRoute.VERIFY_LOGIN,
                        "{\"responseCode\":404,\"message\":\"User not found!\"}"),
                contract("API 11 create account", REQUESTS.createAccount(ACCOUNT), POST, USER_CREATED,
                        EXPECTED_ACCOUNT_FIELDS, Map.of(),
                        AutoExApiRoute.CREATE_ACCOUNT,
                        "{\"responseCode\":201,\"message\":\"User created!\"}"),
                contract("API 12 delete account", REQUESTS.deleteAccount(EMAIL, PASSWORD), DELETE, ACCOUNT_DELETED,
                        Map.of("email", EMAIL, "password", PASSWORD), Map.of(),
                        AutoExApiRoute.DELETE_ACCOUNT,
                        "{\"responseCode\":200,\"message\":\"Account deleted!\"}"),
                contract("API 13 update account", REQUESTS.updateAccount(ACCOUNT), PUT, USER_UPDATED,
                        EXPECTED_ACCOUNT_FIELDS, Map.of(),
                        AutoExApiRoute.UPDATE_ACCOUNT,
                        "{\"responseCode\":200,\"message\":\"User updated!\"}"),
                contract("API 14 user detail", REQUESTS.userDetail(EMAIL), GET, 200,
                        Map.of(), Map.of("email", EMAIL),
                        AutoExApiRoute.USER_DETAIL_BY_EMAIL, AutoExApiJsonField.USER,
                        null, "{\"responseCode\":200,\"user\":{\"email\":\"atf-autoex-contract@example.test\"}}"));

        for (ContractCase testCase : cases) {
            var calls = new AtomicInteger();
            ApiTransport fixture = (request, url, auth) -> {
                calls.incrementAndGet();
                assertEquals(testCase.route().path(), request.path(), testCase.name() + " path");
                assertEquals(testCase.expectedMethod().value(), request.method(), testCase.name() + " method");
                assertEquals(testCase.expectedQuery(), request.query(), testCase.name() + " query parameters");
                auditFormFields(testCase.expectedFormFields(), decodeForm(request.bodyText()),
                        testCase.name() + " form body fields");
                assertEquals(testCase.expectedFormFields().isEmpty() ? null : APPLICATION_FORM_URLENCODED,
                        request.headers().get(HttpHeader.CONTENT_TYPE.value()), testCase.name() + " content type");
                URI target = URI.create(url);
                assertEquals(BASE_URI.getScheme(), target.getScheme(), testCase.name() + " target scheme");
                assertEquals(BASE_URI.getAuthority(), target.getAuthority(), testCase.name() + " target authority");
                return CompletableFuture.completedFuture(new ApiResponse(FIXTURE_HTTP_STATUS,
                        testCase.fixtureBody(), Map.of(), null, url));
            };
            ApiResponse actual = new ApiExecutor(fixture, BASE_URI).execute(testCase.request()).response();

            assertAll(testCase.name(),
                    () -> assertEquals(FIXTURE_HTTP_STATUS, actual.status(),
                            "fixture transport status is deliberately separate from documented responseCode"),
                    () -> assertEquals(testCase.expectedResponseCode(), responseCode(actual.body()),
                            "documented business responseCode"),
                    () -> assertEquals(testCase.expectedMessage(), message(actual.body()),
                            "endpoint message from independent fixture oracle"),
                    () -> assertTrue(hasField(actual.body(), testCase.expectedBodyField()),
                            "response must carry the endpoint-specific JSON field"),
                    () -> assertEquals(1, calls.get(), "one scenario means one transport exchange"));
        }
        assertEquals(14, cases.size(), "all published scenarios must be represented");
    }

    @Test
    void requestsUseFormFieldsAndQueryParametersWithoutLeakingCredentialValuesInAccountModel() {
        ApiRequest login = REQUESTS.verifyLogin(EMAIL, PASSWORD);
        ApiRequest detail = REQUESTS.userDetail(EMAIL);
        ApiRequest delete = REQUESTS.deleteAccount(EMAIL, PASSWORD);
        Map<String, String> loginFields = decodeForm(login.bodyText());
        Map<String, String> deleteFields = decodeForm(delete.bodyText());

        assertAll(
                () -> assertEquals(POST.value(), login.method()),
                () -> auditFormFields(Map.of("email", EMAIL, "password", PASSWORD), loginFields, "login form"),
                () -> assertEquals(DELETE.value(), delete.method()),
                () -> auditFormFields(Map.of("email", EMAIL, "password", PASSWORD), deleteFields, "delete form"),
                () -> assertEquals(APPLICATION_FORM_URLENCODED,
                        delete.headers().get(HttpHeader.CONTENT_TYPE.value())),
                () -> assertEquals(Map.of("email", EMAIL), detail.query()),
                () -> assertEquals(AutoExApiRoute.USER_DETAIL_BY_EMAIL.path(), detail.path()),
                () -> assertFalse(ACCOUNT.toString().contains(PASSWORD),
                        "typed account diagnostics must redact its password"),
                () -> assertTrue(ACCOUNT.formFields().keySet().containsAll(SetOfRequiredAccountFields.VALUE)));
    }

    @Test
    void accountMutationsCarryTheCompletePublishedFormVocabularyAndSearchIsEncoded() {
        ApiRequest create = REQUESTS.createAccount(ACCOUNT);
        ApiRequest update = REQUESTS.updateAccount(ACCOUNT);
        ApiRequest search = REQUESTS.searchProduct("top & tools");
        String specialEmail = "user+demo&team@example.test";
        String specialPassword = "pw=two & + ü";
        ApiRequest encodedLogin = REQUESTS.verifyLogin(specialEmail, specialPassword);
        Map<String, String> createFields = decodeForm(create.bodyText());
        Map<String, String> updateFields = decodeForm(update.bodyText());

        assertAll(
                () -> assertEquals(POST.value(), create.method()),
                () -> assertEquals(PUT.value(), update.method()),
                () -> assertEquals(SetOfRequiredAccountFields.VALUE, createFields.keySet()),
                () -> assertEquals(SetOfRequiredAccountFields.VALUE, updateFields.keySet()),
                () -> assertEquals("top & tools", decodeForm(search.bodyText()).get("search_product")),
                () -> assertTrue(search.bodyText().contains("top+%26+tools")),
                () -> auditFormFields(Map.of("email", specialEmail, "password", specialPassword),
                        decodeForm(encodedLogin.bodyText()), "reserved and Unicode values must round-trip"),
                () -> assertEquals(Map.of("email", EMAIL),
                        decodeForm(REQUESTS.verifyLogin(EMAIL, null).bodyText()),
                        "an intentionally missing password must remain absent on the wire"),
                () -> assertTrue(REQUESTS.verifyLogin(null, null).bodyText().isEmpty()),
                () -> assertTrue(createFields.entrySet().stream()
                                .filter(field -> !field.getKey().equals("address2"))
                                .noneMatch(field -> field.getValue().isBlank()),
                        "the deterministic account fixture must satisfy every required field"));
    }

    @Test
    void credentialMismatchesAndFieldPresenceFailWithoutPuttingValuesIntoDiagnostics() {
        String expected = "AUTOEX_AUDIT_CREDENTIAL_CANARY expected & ü";
        String actual = "AUTOEX_AUDIT_CREDENTIAL_CANARY actual \\";
        AssertionError mismatch = assertThrows(AssertionError.class,
                () -> auditFormFields(Map.of("password", expected), Map.of("password", actual), "credential mismatch"));
        assertFalse(mismatch.getMessage().contains("AUTOEX_AUDIT_CREDENTIAL_CANARY"),
                "failure diagnostics exclude the original credential values");
        assertThrows(AssertionError.class, () -> auditFormFields(Map.of("password", expected), Map.of(), "missing credential"));
        assertThrows(AssertionError.class, () -> auditFormFields(Map.of(), Map.of("password", ""), "unexpected empty credential"));
        AssertionError casing = assertThrows(AssertionError.class,
                () -> auditFormFields(Map.of("password", expected), Map.of("Password", actual), "incorrect field casing"));
        assertFalse(casing.getMessage().contains("AUTOEX_AUDIT_CREDENTIAL_CANARY"));
    }

    private static void auditFormFields(Map<String, String> expected, Map<String, String> actual, String context) {
        var expectedPublic = new LinkedHashMap<>(expected);
        var actualPublic = new LinkedHashMap<>(actual);
        expectedPublic.replaceAll((name, value) -> name.equalsIgnoreCase("password") ? "<credential>" : value);
        actualPublic.replaceAll((name, value) -> name.equalsIgnoreCase("password") ? "<credential>" : value);
        assertAll(context,
                () -> assertEquals(expectedPublic, actualPublic, "non-sensitive form fields and exact key layout"),
                () -> assertTrue(expected.equals(actual), "complete original form values match without disclosure"));
    }

    private static ContractCase contract(String name, ApiRequest request, HttpMethod method,
                                         AutoExApiOutcome outcome, Map<String, String> formFields,
                                         Map<String, String> query, AutoExApiRoute route, String fixtureBody) {
        return contract(name, request, method, outcome.responseCode(), formFields, query, route,
                null, outcome.message(), fixtureBody);
    }

    private static ContractCase contract(String name, ApiRequest request, HttpMethod method, int responseCode,
                                         Map<String, String> formFields, Map<String, String> query,
                                         AutoExApiRoute route, AutoExApiJsonField bodyField,
                                         String message, String fixtureBody) {
        return new ContractCase(name, request, method, responseCode, formFields, query, route, bodyField, message,
                fixtureBody);
    }

    private static int responseCode(String responseBody) {
        return document(responseBody).path(AutoExApiJsonField.RESPONSE_CODE.path()).asInt(Integer.MIN_VALUE);
    }

    private static String message(String responseBody) {
        return document(responseBody).path(AutoExApiJsonField.MESSAGE.path()).asText(null);
    }

    private static boolean hasField(String responseBody, AutoExApiJsonField field) {
        return field == null || document(responseBody).has(field.path());
    }

    private static JsonNode document(String responseBody) {
        try {
            return JSON.readTree(responseBody);
        } catch (java.io.IOException invalidJson) {
            throw new AssertionError("fixture response must be valid JSON", invalidJson);
        }
    }

    private static Map<String, String> decodeForm(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return values;
        for (String pair : body.split("&")) {
            String[] field = pair.split("=", 2);
            values.put(URLDecoder.decode(field[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(field.length == 1 ? "" : field[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    private record ContractCase(String name, ApiRequest request, HttpMethod expectedMethod, int expectedResponseCode,
                                Map<String, String> expectedFormFields, Map<String, String> expectedQuery,
                                AutoExApiRoute route, AutoExApiJsonField expectedBodyField, String expectedMessage,
                                String fixtureBody) { }

    private static final class SetOfRequiredAccountFields {
        private static final java.util.Set<String> VALUE = java.util.Set.of("name", "email", "password",
                "title", "birth_date", "birth_month", "birth_year", "firstname", "lastname",
                "company", "address1", "address2", "country", "zipcode", "state", "city",
                "mobile_number");
    }
}

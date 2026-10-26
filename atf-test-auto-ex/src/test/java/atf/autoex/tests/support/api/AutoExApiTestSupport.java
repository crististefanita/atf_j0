package atf.autoex.tests.support.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import atf.reporting.contract.AtfAssertion;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.request.ApiRequest;
import atf.autoex.api.AutoExApiAccount;
import atf.autoex.api.AutoExApiOutcome;
import atf.autoex.api.AutoExApiJsonField;
import atf.autoex.api.AutoExApiResponseAssertions;
import atf.autoex.api.AutoExLiveApi;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.tests.api.AutoExApiContractReportExtension;

import java.util.Map;
import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** App-owned synthetic account fixture and JUnit publication/assertion adapter; never a test entry point. */
public final class AutoExApiTestSupport {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int EXPECTED_HTTP_STATUS = 200;
    private static final String BASE_URL_PROPERTY = "atf.autoex.api.base-url";
    private static final String TIMEOUT_PROPERTY = "atf.autoex.api.timeout";
    private static final String DEFAULT_TIMEOUT = "PT20S";
    private AutoExApiTestSupport() { }

    public static ApiExecutor liveExecutor(AutoExConfiguration configuration) {
        URI baseUrl = URI.create(System.getProperty(BASE_URL_PROPERTY, configuration.baseUrl().toString()));
        Duration timeout = Duration.parse(System.getProperty(TIMEOUT_PROPERTY, DEFAULT_TIMEOUT));
        return AutoExLiveApi.open(baseUrl, timeout);
    }

    public static AutoExApiAccount account(AutoExConfiguration configuration, String unique) {
        AutoExConfiguration.Registration profile = configuration.registration();
        return new AutoExApiAccount("ATF AutoEx " + unique,
                "atf-autoex+" + unique + "@example.com", "Atf-" + unique + "-Secure",
                "Mr", profile.birthDay(), profile.birthMonth(), profile.birthYear(), profile.firstName(),
                profile.lastName(), profile.company(), profile.address1(), profile.address2(),
                profile.country(), profile.postalCode(), profile.state(), profile.city(), profile.mobileNumber());
    }

    public static AutoExApiAccount updatedName(AutoExApiAccount account) {
        return new AutoExApiAccount(account.name() + " Updated", account.email(), account.password(),
                account.title(), account.birthDate(), account.birthMonth(), account.birthYear(),
                account.firstName(), account.lastName(), account.company(), account.address1(),
                account.address2(), account.country(), account.zipcode(), account.state(), account.city(),
                account.mobileNumber());
    }

    public static JsonNode assertResponse(String name, ApiExecutor api, ApiRequest request, int responseCode) {
        return assertResponse(name, api, request, responseCode, null, Map.of(), new AutoExApiJsonField[0]);
    }

    public static JsonNode assertResponse(String name, ApiExecutor api, ApiRequest request,
                                   int responseCode, String message) {
        return assertResponse(name, api, request, responseCode, message, Map.of(), new AutoExApiJsonField[0]);
    }

    public static JsonNode assertResponse(String name, ApiExecutor api, ApiRequest request,
                                   int responseCode, String message, AutoExApiJsonField... expectedArrayFields) {
        return assertResponse(name, api, request, responseCode, message, Map.of(), expectedArrayFields);
    }

    public static JsonNode assertResponse(String name, ApiExecutor api, ApiRequest request,
                                          AutoExApiOutcome outcome) {
        return assertResponse(name, api, request, outcome.responseCode(), outcome.message());
    }

    public static JsonNode assertResponseWithTextFields(String name, ApiExecutor api, ApiRequest request,
                                                 int responseCode, String message,
                                                 Map<String, String> expectedTextFields) {
        return assertResponse(name, api, request, responseCode, message, expectedTextFields,
                new AutoExApiJsonField[0]);
    }

    public static JsonNode assertSearchResponse(String name, ApiExecutor api, ApiRequest request,
                                         int responseCode, String searchTerm) {
        return assertResponse(name, api, request, responseCode, null, Map.of(),
                new AutoExApiJsonField[]{AutoExApiJsonField.PRODUCTS}, searchTerm);
    }

    private static JsonNode assertResponse(String name, ApiExecutor api, ApiRequest request,
                                           int responseCode, String message,
                                           Map<String, String> expectedTextFields,
                                           AutoExApiJsonField... expectedArrayFields) {
        return assertResponse(name, api, request, responseCode, message, expectedTextFields,
                expectedArrayFields, null);
    }

    private static JsonNode assertResponse(String name, ApiExecutor api, ApiRequest request,
                                           int responseCode, String message,
                                           Map<String, String> expectedTextFields,
                                           AutoExApiJsonField[] expectedArrayFields,
                                           String expectedSearchTerm) {
        var execution = api.execute(request);
        var response = execution.response();
        JsonNode body = null;
        java.io.IOException parseFailure = null;
        try {
            body = JSON.readTree(response.body());
        } catch (java.io.IOException invalidJson) {
            parseFailure = invalidJson;
        }
        JsonNode parsedBody = body;
        var semanticAssertions = new java.util.ArrayList<AtfAssertion>();
        semanticAssertions.add(new AtfAssertion("HTTP transport status",
                Integer.toString(EXPECTED_HTTP_STATUS), Integer.toString(response.status()),
                response.status() == EXPECTED_HTTP_STATUS));
        expectedTextFields.forEach((path, expected) -> {
            JsonNode value = jsonPath(parsedBody, path);
            String actual = value == null ? "<missing>"
                    : value.isTextual() ? value.asText() : value.toString();
            semanticAssertions.add(new AtfAssertion("JSON text field: " + path, expected, actual,
                    expected.equals(actual)));
        });
        java.util.Arrays.stream(expectedArrayFields).forEach(field -> {
            JsonNode value = parsedBody == null ? null : parsedBody.get(field.path());
            String actual = value == null ? "<missing>" : value.getNodeType().name();
            semanticAssertions.add(new AtfAssertion("JSON array field: " + field.path(), "ARRAY", actual,
                    value != null && value.isArray()));
        });
        AtfAssertion searchAssertion = expectedSearchTerm == null ? null
                : AutoExApiResponseAssertions.searchResultsContain(parsedBody, expectedSearchTerm);
        if (searchAssertion != null) semanticAssertions.add(searchAssertion);
        AutoExApiContractReportExtension.publish(name, execution, responseCode, message, semanticAssertions);
        assertEquals(EXPECTED_HTTP_STATUS, response.status(), name + " transport HTTP status");
        if (parseFailure != null) throw new AssertionError(name + " response body must be valid JSON", parseFailure);
        assertEquals(responseCode, body.path(AutoExApiJsonField.RESPONSE_CODE.path()).asInt(Integer.MIN_VALUE),
                name + " JSON responseCode (transport HTTP status=" + response.status() + ")");
        if (message != null) {
            assertEquals(message, body.path(AutoExApiJsonField.MESSAGE.path()).asText(null), name + " JSON message");
        }
        for (Map.Entry<String, String> expectation : expectedTextFields.entrySet()) {
            String path = expectation.getKey();
            JsonNode value = jsonPath(body, path);
            String actual = value == null ? "<missing>"
                    : value.isTextual() ? value.asText() : value.toString();
            assertEquals(expectation.getValue(), actual, name + " JSON field " + path);
        }
        for (AutoExApiJsonField field : expectedArrayFields) {
            assertTrue(body.path(field.path()).isArray(),
                    name + " JSON field '" + field.path() + "' must be an array");
        }
        if (searchAssertion != null) {
            assertTrue(searchAssertion.passed(), name + " must return a product matching '"
                    + expectedSearchTerm + "' (actual: " + searchAssertion.actual() + ")");
        }
        return body;
    }

    private static JsonNode jsonPath(JsonNode document, String path) {
        JsonNode current = document;
        for (String segment : path.split("\\.")) {
            if (current == null) return null;
            current = current.get(segment);
        }
        return current;
    }

}

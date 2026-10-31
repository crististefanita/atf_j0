package atf.core.security;
import atf.testkit.AtfUnitTest;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class RedactionStrategiesTest {
    @Test
    void masksTheWholeEscapedJsonSecretBeforeAnyConsumerPreview() throws Exception {
        String json = new ObjectMapper().writeValueAsString(Map.of(
                "password", "prefix\"CORE_SYNTHETIC_CANARY_" + "q".repeat(280) + "\\tail"));
        String safe = new SensitiveDataRedactor().redactText("metadata: " + json);
        assertFalse(safe.contains("CORE_SYNTHETIC_CANARY_"));
        assertTrue(safe.contains("REDACTED"));
    }

    @Test
    void specializedStrategiesOwnTheirBoundaries() {
        var context = new RedactionContext(RedactionSettings.defaults());

        assertAll(
                () -> assertFalse(new SecretPatternRedactor(context)
                        .redact("Authorization: Bearer secret").contains("secret")),
                () -> assertFalse(new PiiRedactor(new RedactionContext(
                        new RedactionSettings(true, "[hidden]", false, true, Set.of())))
                        .redact("person@example.test").contains("@")),
                () -> assertFalse(new LocalPathRedactor(context)
                        .redact("C:\\Users\\person\\secret.json").contains("person")));
    }

    @Test
    void structuredStrategyDoesNotMutateSourceAndFallsBackForMalformedJson() {
        var context = new RedactionContext(RedactionSettings.defaults());
        var strategy = new StructuredJsonRedactor(context, new SecretPatternRedactor(context));
        String source = "{\"token\":\"secret\",\"items\":[{\"api_key\":\"abc\"}]}";

        String safe = strategy.redact(source);
        String malformed = strategy.redact("token=secret");

        assertAll(
                () -> assertEquals("{\"token\":\"secret\",\"items\":[{\"api_key\":\"abc\"}]}", source),
                () -> assertFalse(safe.contains("secret")),
                () -> assertFalse(safe.contains("abc")),
                () -> assertEquals("token=***REDACTED***", malformed),
                () -> assertFalse(malformed.contains("secret")));
    }

    @Test
    void redactsWireCredentialHeaderNamesIncludingApiKeyVariants() {
        var redactor = new SensitiveDataRedactor();
        String safe = redactor.redactText("Authorization=Bearer secret, X-Api-Key=api-secret, X-Auth-Token=token");

        assertAll(
                () -> assertFalse(safe.contains("secret")),
                () -> assertFalse(safe.contains("api-secret")),
                () -> assertFalse(safe.contains("token")));
    }

    @Test
    void redactsCsrfTokensInFormAndStructuredRequestBodies() {
        var redactor = new SensitiveDataRedactor();
        String form = redactor.redactText(
                "csrfmiddlewaretoken=form-secret&email=user%40example.test&password=login-secret");
        String json = redactor.redactJson(
                "{\"csrfmiddlewaretoken\":\"json-secret\",\"csrf_token\":\"json-variant\"}");

        assertAll(
                () -> assertTrue(form.contains("csrfmiddlewaretoken=***REDACTED***")),
                () -> assertTrue(form.contains("password=***REDACTED***")),
                () -> assertFalse(form.contains("form-secret")),
                () -> assertFalse(form.contains("login-secret")),
                () -> assertFalse(json.contains("json-secret")),
                () -> assertFalse(json.contains("json-variant")),
                () -> assertTrue(json.contains("***REDACTED***")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"$0", "$1", "$MASK", "\\", "\\tail\\", "$0\\$MASK\\", "$9${name}\\tail"})
    void literalMaskPreservesCredentialPrefixesAndSurroundingEvidence(String mask) {
        var redactor = literalRedactor(mask, true, true);
        Map.of(
                "Authorization: Bearer CORE_AUTH_CANARY", "Authorization: Bearer " + mask,
                "Authorization=Basic CORE_BASIC_CANARY", "Authorization=Basic " + mask,
                "password=CORE_FORM_CANARY&state=ready", "password=" + mask + "&state=ready",
                "metadata: {\"token\":\"CORE_JSON_CANARY\"}", "metadata: {\"token\":\"" + mask + "\"}",
                "GET /catalog?token=CORE_QUERY_CANARY&limit=2", "GET /catalog?token=" + mask + "&limit=2",
                "/token/CORE_PATH_CANARY/item", "/token/" + mask + "/item")
                .forEach((source, expected) -> assertEquals(expected, redactor.redactText(source),
                        "The configured mask is literal; structural prefixes and neighbours survive"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"$0", "$1", "$MASK", "\\", "\\tail\\", "$0\\$MASK\\", "$9${name}\\tail"})
    void literalMaskCoversWholePiiMatchesAndRetainsOnlyTheLocalPathPrefix(String mask) {
        var redactor = literalRedactor(mask, true, true);
        assertAll(
                () -> assertEquals(mask, redactor.redactText("synthetic@example.test")),
                () -> assertEquals(mask, redactor.redactText("+40 (721) 123-456")),
                () -> assertEquals("C:\\Users\\" + mask,
                        redactor.redactText("C:\\Users\\CORE_USER_CANARY\\private.json")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"$0", "$1", "$MASK", "\\", "\\tail\\", "$0\\$MASK\\", "$9${name}\\tail"})
    void structuredJsonAndNamedFieldsKeepTheRawMaskRatherThanItsRegexEncoding(String mask) throws Exception {
        var redactor = literalRedactor(mask, true, true);
        var mapper = new ObjectMapper();
        String source = mapper.writeValueAsString(Map.of("token", "CORE_JSON_CANARY",
                "items", List.of(Map.of("api_key", "CORE_NESTED_CANARY")), "state", "ready"));
        String safe = redactor.redactJson(source);
        var json = mapper.readTree(safe);
        assertAll(
                () -> assertEquals(mask, json.path("token").textValue()),
                () -> assertEquals(mask, json.path("items").get(0).path("api_key").textValue()),
                () -> assertEquals("ready", json.path("state").textValue()),
                () -> assertEquals(mask, redactor.redactField("Authorization", "Bearer CORE_FIELD_CANARY")),
                () -> assertFalse(safe.contains("CORE_JSON_CANARY") || safe.contains("CORE_NESTED_CANARY")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"$0", "$1", "$MASK", "\\", "\\tail\\", "$0\\$MASK\\", "$9${name}\\tail"})
    void malformedJsonFallbackMasksTheWholeEscapedSecretWithoutRepairingTheDocument(String mask) {
        String malformed = "{\"token\":\"prefix\\\"CORE_FALLBACK_CANARY\\\\tail\"";
        String expected = "{\"token\":\"" + mask + "\"";
        assertEquals(expected, literalRedactor(mask, true, true).redactJson(malformed),
                "Fallback keeps malformed structure while removing the entire escaped credential");
    }

    @Test
    void literalEncodingDoesNotActivateDisabledTextJsonOrOptionalStrategies() {
        String mask = "$MASK\\";
        String source = "token=CORE_DISABLED_CANARY";
        String json = "{\"token\":\"CORE_DISABLED_CANARY\"}";
        var disabled = new SensitiveDataRedactor(new RedactionSettings(false, mask, true, true,
                RedactionSettings.defaults().sensitiveFields()));
        var optionalDisabled = literalRedactor(mask, false, false);
        assertAll(
                () -> assertEquals(source, disabled.redactText(source)),
                () -> assertEquals(json, disabled.redactJson(json)),
                () -> assertEquals("synthetic@example.test", optionalDisabled.redactText("synthetic@example.test")),
                () -> assertEquals("C:\\Users\\synthetic\\file.txt", optionalDisabled.redactText("C:\\Users\\synthetic\\file.txt")),
                () -> assertEquals("token=" + mask, optionalDisabled.redactText(source)));
    }

    private static SensitiveDataRedactor literalRedactor(String mask, boolean localPaths, boolean pii) {
        return new SensitiveDataRedactor(new RedactionSettings(true, mask, localPaths, pii,
                RedactionSettings.defaults().sensitiveFields()));
    }
}

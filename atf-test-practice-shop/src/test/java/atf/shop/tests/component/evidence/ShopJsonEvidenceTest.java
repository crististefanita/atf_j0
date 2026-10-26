package atf.shop.tests.component.evidence;

import atf.api.testkit.AtfComponentTest;
import atf.shop.support.ShopJsonEvidence;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopJsonEvidenceTest {
    @Test
    void redactsNestedArrayFieldsAndSensitiveHeaderVariantsBeforeEvidenceIsPublished() {
        String json = "{\"items\":[{\"password\":\"secret\",\"apiKey\":\"key-1\"}],"
                + "\"note\":\"escaped \\\"value\\\"\"}";

        String redacted = ShopJsonEvidence.redact(json);
        Map<String, String> headers = ShopJsonEvidence.redactHeaders(Map.of(
                "X-API-Key", "key-1", "apiKey", "key-2", "Authorization", "Bearer secret"));

        assertFalse(redacted.contains("secret"));
        assertFalse(redacted.contains("key-1"));
        assertTrue(redacted.contains("escaped"));
        assertTrue(headers.values().stream().allMatch("<redacted>"::equals));
    }

    @Test
    void registrationEvidenceRedactsIdentityAddressAndContactFieldsRecursively() {
        String json = "{\"data\":[{\"first_name\":\"Ada\",\"last_name\":\"Lovelace\","
                + "\"dob\":\"1990-01-01\",\"email\":\"ada@owned.test\","
                + "\"address\":{\"street\":\"Secret Street\",\"city\":\"Hidden City\","
                + "\"postal_code\":\"12345\",\"phone\":\"0700000000\"}}]}";

        String redacted = ShopJsonEvidence.redact(json);

        assertTrue(redacted.contains("<redacted>"));
        for (String personalValue : java.util.List.of("Ada", "Lovelace", "1990-01-01",
                "ada@owned.test", "Secret Street", "Hidden City", "12345", "0700000000")) {
            assertFalse(redacted.contains(personalValue), "leaked value: " + personalValue);
        }
    }

    @Test
    void malformedOrNonJsonBodiesFailClosedAndTransportMarkersRemainDiagnostic() {
        String malformed = "{\"email\":\"private@example.test\",\"address\":\"Secret Road\",";
        String unavailable = "<body unavailable: Chromium response body — payload was not exposed>";

        String redacted = ShopJsonEvidence.redact(malformed);

        assertFalse(redacted.contains("private@example.test"));
        assertFalse(redacted.contains("Secret Road"));
        assertTrue(redacted.contains("body omitted"));
        assertTrue(ShopJsonEvidence.redact(unavailable).contains("body unavailable"));
    }

    @Test
    void normalizedPersonalFieldAliasesAndIdentityHeadersUseOneDefaultPolicy() {
        String json = "{\"customerFirstName\":\"Ada\",\"emailAddress\":\"ada@owned.test\","
                + "\"shippingAddress\":{\"postal-code\":\"10101\",\"phone_number\":\"0700000000\"}}";
        Map<String, String> headers = ShopJsonEvidence.redactHeaders(Map.of(
                "X-User-Email", "ada@owned.test", "Set-Cookie", "session=secret"));

        String redacted = ShopJsonEvidence.redact(json);

        for (String personalValue : java.util.List.of("Ada", "ada@owned.test", "10101", "0700000000")) {
            assertFalse(redacted.contains(personalValue), "leaked value: " + personalValue);
        }
        assertTrue(headers.values().stream().allMatch("<redacted>"::equals));
    }

    @Test
    void embeddedEmailAddressesAreRedactedFromFreeTextJsonValues() {
        String json = "{\"message\":\"login failed for private.person@example.test\"}";

        String redacted = ShopJsonEvidence.redact(json);

        assertFalse(redacted.contains("private.person@example.test"));
        assertTrue(redacted.contains("login failed for <redacted>"));
    }
}

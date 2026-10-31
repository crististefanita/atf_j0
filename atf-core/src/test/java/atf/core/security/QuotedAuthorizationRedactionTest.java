package atf.core.security;

import atf.testkit.AtfUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AtfUnitTest
class QuotedAuthorizationRedactionTest {
    private static final String CANARY = "CORE_QUOTED_AUTH_CANARY";
    private static final ObjectMapper JSON = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"***REDACTED***", "$0", "$1", "$MASK", "\\", "\\tail\\", "$0\\$MASK\\", "$9${name}\\tail"})
    void masksTheWholeQuotedValueInTextAndMalformedJsonWithoutRepair(String mask) throws Exception {
        var redactor = redactor(mask, true);
        String encoded = JSON.writeValueAsString("Bearer prefix\"" + CANARY + "q".repeat(280) + "\\tail");
        String malformed = "{\"AuThOrIzAtIoN\" : " + encoded;
        String expected = "{\"AuThOrIzAtIoN\" : \"" + mask + "\"";
        String text = "metadata: " + malformed + ", \"state\":\"ready\"}";
        String safeText = redactor.redactText(text);
        String safeFallback = redactor.redactJson(malformed);

        assertAll(
                () -> assertEquals("metadata: " + expected + ", \"state\":\"ready\"}", safeText),
                () -> assertEquals(expected, safeFallback),
                () -> assertFalse(safeText.contains(CANARY)),
                () -> assertFalse(safeFallback.contains(CANARY)),
                () -> assertEquals("{\"AuThOrIzAtIoN\" : " + encoded, malformed));
    }

    @ParameterizedTest
    @ValueSource(strings = {"***REDACTED***", "$0", "$1", "$MASK", "\\", "\\tail\\", "$0\\$MASK\\", "$9${name}\\tail"})
    void keepsStructuralJsonAndPlainHeadersConsistentWithTheLiteralPolicy(String mask) throws Exception {
        var redactor = redactor(mask, true);
        String source = JSON.writeValueAsString(Map.of("authorization", "Basic " + CANARY, "state", "ready"));
        var safe = JSON.readTree(redactor.redactJson(source));
        assertAll(
                () -> assertEquals(mask, safe.path("authorization").textValue()),
                () -> assertEquals("ready", safe.path("state").textValue()),
                () -> assertEquals("Authorization: Bearer " + mask,
                        redactor.redactText("Authorization: Bearer " + CANARY)),
                () -> assertEquals("Authorization=Basic " + mask,
                        redactor.redactText("Authorization=Basic " + CANARY)));
    }

    @Test
    void masksAnUnterminatedQuotedValueIncludingAnIncompleteEscape() {
        String source = "{\"authorization\":\"Bearer " + CANARY + "\\tail\\";
        assertEquals("{\"authorization\":\"$MASK\\", redactor("$MASK\\", true).redactJson(source),
                "A truncated diagnostic keeps its missing terminator, without retaining credential bytes");
    }

    @Test
    void preservesDisabledRedactionAndNonSensitiveQuotedNeighbours() {
        String source = "metadata: {\"authorization\":\"Bearer " + CANARY + "\",\"state\":\"ready\"}";
        var enabled = redactor("[hidden]", true);
        assertAll(
                () -> assertEquals(source, redactor("$MASK\\", false).redactText(source)),
                () -> assertEquals("{\"state\":\"ready\",\"description\":\"Authorization policy\"}",
                        enabled.redactText("{\"state\":\"ready\",\"description\":\"Authorization policy\"}")));
    }

    private static SensitiveDataRedactor redactor(String mask, boolean enabled) {
        return new SensitiveDataRedactor(new RedactionSettings(enabled, mask, false, false,
                RedactionSettings.defaults().sensitiveFields()));
    }
}

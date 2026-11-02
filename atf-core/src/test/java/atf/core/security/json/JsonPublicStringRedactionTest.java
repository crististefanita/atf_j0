package atf.core.security.json;

import atf.core.security.RedactionSettings;
import atf.core.security.SensitiveDataRedactor;
import atf.testkit.AtfUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonPublicStringRedactionTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    @Test void redactsKnownCredentialSyntaxAfterDecodingJsonStrings() throws Exception {
        String canary = UUID.randomUUID().toString();
        for (String value : new String[]{"Authorization: Bearer " + canary, "/token/" + canary,
                "https://fixture.invalid/read?api_key=" + canary, "password=" + canary}) {
            String input = JSON.writeValueAsString(Map.of("message", value, "neighbor", "public text"));
            var actual = JSON.readTree(new SensitiveDataRedactor().redactJson(input));
            assertFalse(actual.path("message").asText().contains(canary), "Known credential syntax must be masked");
            assertEquals("public text", actual.path("neighbor").asText());
            assertTrue(input.contains(canary), "Input is immutable");
        }
    }
    @Test void disabledPolicyAndLiteralReplacementRemainExplicit() throws Exception {
        String canary = UUID.randomUUID().toString();
        String input = JSON.writeValueAsString(Map.of("message", "Authorization: Bearer " + canary));
        var disabled = new SensitiveDataRedactor(new RedactionSettings(false, "$1\\mask", false, false, Set.of()));
        assertTrue(disabled.redactJson(input).equals(input), "Disabled policy returns original input exactly");
        var enabled = new SensitiveDataRedactor(new RedactionSettings(true, "$1\\mask", false, false, Set.of()));
        assertTrue(("Authorization: Bearer $1\\mask").equals(JSON.readTree(enabled.redactJson(input)).path("message").asText()),
                "Literal replacement must survive JSON escaping exactly");
    }
}

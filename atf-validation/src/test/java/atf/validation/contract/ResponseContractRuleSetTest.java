package atf.validation.contract;
import atf.testkit.AtfUnitTest;

import atf.core.http.response.ApiResponse;
import atf.validation.evaluation.ValidationTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.time.Duration;
import java.util.Map;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AtfUnitTest
@Isolated("Exercises HTTP protocol comparisons under the Turkish JVM locale")

class ResponseContractRuleSetTest {
    @Test
    void httpComparisonsAndRuleIdentityRemainStableInTurkishLocale() {
        Locale previous = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var rules = new ResponseContractRuleSet(SchemaResourceResolver.fromWorkingDirectory());
            var response = new ApiResponse(200, "{}", Map.of("Content-Type", "APPLICATION/JSON"), Duration.ZERO, "");
            var nominal = new ValidationTable();
            var rejected = new ValidationTable();
            rules.contentTypeContains("application/json").rule().evaluate(response, nominal);
            rules.contentTypeContains("text/plain").rule().evaluate(response, rejected);
            assertTrue(nominal.rows().getFirst().passed());
            assertFalse(rejected.rows().getFirst().passed());
            assertEquals("header:x-identity", rules.headerPresent("X-IDENTITY").identity());
        } finally {
            Locale.setDefault(previous);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }

    @Test
    void createsIndependentRulesWithStableIdentityAndEvidence() {
        var rules = new ResponseContractRuleSet(SchemaResourceResolver.fromWorkingDirectory());
        var response = new ApiResponse(200, "{\"id\":1}",
                Map.of("Content-Type", "application/json"), Duration.ofMillis(2),
                "https://example.test/users");

        var status = rules.status(200);
        var path = rules.jsonPathExists("$.id");
        var statusTable = new ValidationTable();
        var pathTable = new ValidationTable();

        status.rule().evaluate(response, statusTable);
        path.rule().evaluate(response, pathTable);

        assertEquals("status:200", status.identity());
        assertEquals("HTTP status", status.title());
        assertTrue(statusTable.rows().getFirst().passed());
        assertTrue(pathTable.rows().getFirst().passed());
        assertEquals("{\"id\":1}", response.body());
    }
}

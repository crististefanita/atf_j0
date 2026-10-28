package atf.validation;

import atf.testkit.AtfUnitTest;
import atf.validation.evaluation.ValidationTable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ValidationPreviewRedactionTest {
    @Test void redactsFullJsonAndNamedSecretsBeforeTruncatingEitherValue() {
        String secret = "{\"password\":\"VALIDATION_SYNTHETIC_CANARY_" + "q".repeat(280) + "\"}";
        var table = new ValidationTable();
        table.check("json", secret, secret, () -> secret.equals(secret));
        table.add("password", "synthetic expected", "synthetic actual", true);
        for (var row : table.rows()) {
            assertFalse(row.expected().contains("CANARY") || row.actual().contains("CANARY"));
            assertTrue(row.expected().contains("REDACTED"));
            assertTrue(row.actual().contains("REDACTED"));
            assertTrue(row.expected().length() <= 240 && row.actual().length() <= 240);
            assertTrue(row.passed());
        }
    }
    @Test void preservesFullInputVerdictAndTheCustomAndDefaultBudgets() {
        String prefix = "x".repeat(240);
        var table = new ValidationTable();
        table.check("suffix differs", prefix + "a", prefix + "b", () -> (prefix + "a").equals(prefix + "b"));
        assertFalse(table.rows().getFirst().passed());
        assertEquals(240, table.rows().getFirst().actual().length());
        assertTrue(table.rows().getFirst().actual().endsWith("…"));
        assertEquals(10, new ValidationTable(10).preview("x".repeat(11)).length());
    }
    @Test void keepsEscapedSecretSuffixesOutOfTheBoundedRows() throws Exception {
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                "password", "prefix\"VALIDATION_ESCAPED_CANARY_" + "q".repeat(280)));
        var table = new ValidationTable();
        table.add("json", json, json, true);
        assertFalse(table.rows().getFirst().actual().contains("VALIDATION_ESCAPED_CANARY_"));
        assertFalse(table.rows().getFirst().expected().contains("VALIDATION_ESCAPED_CANARY_"));
    }
}

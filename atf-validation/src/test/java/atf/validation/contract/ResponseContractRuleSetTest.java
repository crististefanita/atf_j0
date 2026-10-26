package atf.validation.contract;
import atf.testkit.AtfUnitTest;

import atf.core.http.response.ApiResponse;
import atf.validation.evaluation.ValidationTable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class ResponseContractRuleSetTest {
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

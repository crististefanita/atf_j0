package atf.validation;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportStatus;
import atf.validation.evaluation.ValidationOutcomePolicy;
import atf.validation.evaluation.ValidationTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class ValidationOutcomePolicyTest {
    @Test
    void strictPolicyPublishesFailureAsFailingDecision() {
        var table = new ValidationTable();
        table.add("HTTP status", "200", "500", false);

        var decision = ValidationOutcomePolicy.strict().decide(table);

        assertEquals(AtfReportStatus.FAIL, decision.status());
        assertTrue(decision.shouldThrow());
        assertTrue(table.finished());
    }

    @Test
    void warningPolicyChangesOnlyFailedRowsToWarnings() {
        var table = new ValidationTable();
        table.add("HTTP status", "200", "500", false);
        table.add("Content-Type", "application/json", "application/json", true);

        var decision = ValidationOutcomePolicy.warning().decide(table);

        assertEquals(AtfReportStatus.WARNING, decision.status());
        assertEquals(AtfReportStatus.WARNING, decision.rows().get(0).resultStatus());
        assertEquals(AtfReportStatus.PASS, decision.rows().get(1).resultStatus());
        assertTrue(!decision.shouldThrow());
    }

    @Test
    void policyPublishesEmptyTableAsExplicitFailure() {
        var decision = ValidationOutcomePolicy.strict().decide(new ValidationTable());

        assertEquals(AtfReportStatus.FAIL, decision.status());
        assertTrue(decision.shouldThrow());
        assertEquals(1, decision.rows().size());
    }
}

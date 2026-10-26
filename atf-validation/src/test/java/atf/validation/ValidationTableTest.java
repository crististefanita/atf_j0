package atf.validation;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportStatus;
import atf.validation.evaluation.ValidationTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class ValidationTableTest {
    @Test
    void groupsAllAssertionsIntoOneDecisionAndPreservesRows() {
        var table = new ValidationTable(10);
        table.add("HTTP status", "200", "200", true);
        table.add("Content-Type", "application/json", "text/plain", false);

        var decision = table.decide(false);

        assertEquals(AtfReportStatus.FAIL, decision.status());
        assertTrue(decision.shouldThrow());
        assertEquals(2, decision.rows().size());
        assertEquals("text/plain", decision.rows().get(1).actual());
        assertThrows(IllegalStateException.class, () -> table.add("after", "x", "y", true));
    }

    @Test
    void warningModeKeepsFailedRowVisibleButDoesNotThrow() {
        var table = new ValidationTable();
        table.add("contract", "matched", "different", false);

        var decision = table.decide(true);

        assertEquals(AtfReportStatus.WARNING, decision.status());
        assertFalse(decision.shouldThrow());
        assertEquals(AtfReportStatus.WARNING, decision.rows().getFirst().resultStatus());
    }

    @Test
    void emptyValidationBecomesAnExplicitFailureRow() {
        var decision = new ValidationTable().decide(false);
        assertEquals(AtfReportStatus.FAIL, decision.status());
        assertEquals("Validation assertions", decision.rows().getFirst().name());
    }

    @Test
    void duplicateAssertionNamesAreRejected() {
        var table = new ValidationTable();
        table.add("Status", "200", "200", true);
        assertThrows(IllegalArgumentException.class, () -> table.add(" status ", "200", "200", true));
    }
}

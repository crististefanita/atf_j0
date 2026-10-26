package atf.reporting.audit;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportAuditChecksTest {
    @Test
    void parsesCountsWithoutConcatenatingUnrelatedNumbers() {
        assertEquals(7, ReportAuditChecks.count("7 tests"));
        assertEquals(7, ReportAuditChecks.count("7 passed 2 failed"));
        assertEquals(0, ReportAuditChecks.count("—"));
    }

    @Test
    void parsesDurationsWithExplicitUnits() {
        assertEquals(0d, ReportAuditChecks.parseDurationMs("0").orElseThrow());
        assertEquals(12d, ReportAuditChecks.parseDurationMs("12 ms").orElseThrow());
        assertEquals(1200d, ReportAuditChecks.parseDurationMs("1.2 s").orElseThrow());
        assertTrue(ReportAuditChecks.parseDurationMs("—").isEmpty());
        assertTrue(ReportAuditChecks.parseDurationMs("not a duration").isEmpty());
    }
}

package atf.reporting.audit;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportAuditSnapshotTest {
    @Test
    void categoryContractNamesZeroOneAndManyWithoutHardCodedCounts() {
        var empty = new ReportAuditSnapshot.Annotations(0, 0, "surface", true);
        var one = new ReportAuditSnapshot.Annotations(1, 1, "surface", true);
        var many = new ReportAuditSnapshot.Annotations(3, 7, "surface", true);

        assertEquals(ReportAuditSnapshot.Cardinality.ZERO, empty.cardinality());
        assertEquals(ReportAuditSnapshot.Cardinality.ONE, one.cardinality());
        assertEquals(ReportAuditSnapshot.Cardinality.MANY, many.cardinality());
        assertTrue(empty.hasConsistentLinkCardinality());
        assertTrue(one.hasConsistentLinkCardinality());
        assertTrue(many.hasConsistentLinkCardinality());
    }
}

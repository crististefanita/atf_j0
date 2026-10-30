package atf.reporting.evidence.diagnostic;

import atf.reporting.contract.*;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportAssertionSummaryTest {
    @Test void fullComparisonAndDeclaredFailureRemainIndependentOfTheBoundedSummary() {
        var row=new AtfAssertion("Different after budget","x".repeat(300)+"A","x".repeat(300)+"B",false);
        String summary=ReportAssertionSummary.failures(List.of(row));
        assertEquals(240,summary.length());assertTrue(summary.endsWith("…"));
        assertFalse(row.passed());assertNotEquals(row.expected(),row.actual());
        assertTrue(ReportAssertionSummary.failures(List.of(new AtfAssertion("Policy","allowed","rejected",true,AtfReportStatus.FAIL))).contains("Policy"));
    }
    @Test void namedCredentialRowsAreMaskedBeforeTheGlobalDisplayBudget() {
        var row=new AtfAssertion("password","expected-canary","actual-canary",false);
        for(String text:List.of(ReportAssertionSummary.failures(List.of(row)),ReportAssertionSummary.expected(List.of(row)))) {
            assertFalse(text.contains("expected-canary"));assertFalse(text.contains("actual-canary"));
        }
        assertEquals("",ReportAssertionSummary.failures(List.of()));
        assertEquals("Name: visible",ReportAssertionSummary.expected(List.of(new AtfAssertion("Name","visible","visible",true))));
    }
}

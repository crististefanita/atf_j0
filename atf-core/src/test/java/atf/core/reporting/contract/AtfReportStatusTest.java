package atf.core.reporting.contract;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class AtfReportStatusTest {
    @Test
    void exposesOneStableValueAndLabelForEveryReportStatus() {
        assertEquals("pass", AtfReportStatus.PASS.displayValue());
        assertEquals("Pass", AtfReportStatus.PASS.displayLabel());
        assertEquals("fail", AtfReportStatus.FAIL.displayValue());
        assertEquals("Warning", AtfReportStatus.WARNING.displayLabel());
        assertEquals("skip", AtfReportStatus.SKIP.displayValue());
        assertEquals("Info", AtfReportStatus.INFO.displayLabel());
    }
}

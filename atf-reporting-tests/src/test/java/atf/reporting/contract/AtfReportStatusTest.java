package atf.reporting.contract;

import atf.testkit.AtfUnitTest;
import atf.reporting.markup.ExtentReportNodeSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Locks the renderer-neutral status vocabulary used by every evidence adapter. */
@AtfUnitTest
class AtfReportStatusTest {
    @Test
    void exposesOneCanonicalMarkerAndResultForEveryStatus() {
        assertAll(
                () -> assertEquals("ℹ️", AtfReportStatus.INFO.marker()),
                () -> assertEquals("✅ PASS", AtfReportStatus.PASS.resultMarker()),
                () -> assertEquals("⚠️ WARNING", AtfReportStatus.WARNING.resultMarker()),
                () -> assertEquals("⏭️ SKIP", AtfReportStatus.SKIP.resultMarker()),
                () -> assertEquals("❌ FAIL", AtfReportStatus.FAIL.resultMarker()),
                () -> assertEquals("Pass", AtfReportStatus.PASS.displayLabel()),
                () -> assertEquals("❌", ExtentReportNodeSupport.resultMarker(AtfReportStatus.FAIL)),
                () -> assertEquals("ℹ️", ExtentReportNodeSupport.resultMarker(null)),
                () -> assertEquals(AtfReportStatus.INFO, AtfReportStatus.orInfo(null)));
    }
}

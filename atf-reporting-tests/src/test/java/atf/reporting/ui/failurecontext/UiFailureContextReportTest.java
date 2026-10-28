package atf.reporting.ui.failurecontext;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiFailureContextReportTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void failureSummaryRetainsActionBeforeLongTechnicalDiagnostic(boolean fallback) throws Exception {
        String html = Files.readString(UiFailureContextFixture.publish(fallback, 240));
        assertTrue(html.contains("Click #22 -&gt; #submit_search"), "compact failed action must survive the preview");
        assertTrue(html.contains("elapsed=1234 ms"), "measured action time must remain visible");
        assertTrue(html.contains("causal diagnostic"), "cause is available separately from the primary preview");
        assertTrue(html.contains("cleanup diagnostic"), "suppressed failure is retained separately");
        assertFalse(html.contains(UiFailureContextFixture.SECRET));
    }
}

package atf.reporting.audit;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ReportObservationBrowserComponentTest {
    @Test void nestedObservationsKeepIndependentGlyphsKeyboardAndCompleteEvidence() {
        atf.reporting.support.observation.ObservationBrowserProbe.verify();
    }
}

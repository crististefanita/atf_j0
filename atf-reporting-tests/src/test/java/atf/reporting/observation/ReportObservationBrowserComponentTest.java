package atf.reporting.observation;
import atf.testkit.AtfComponentTest;
import atf.reporting.support.observation.ObservationBrowserProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ReportObservationBrowserComponentTest {
    @Test void nestedObservationsKeepIndependentGlyphsKeyboardAndCompleteEvidence() {
        ObservationBrowserProbe.verify();
    }
}

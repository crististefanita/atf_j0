package atf.reporting.observation;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.testkit.AtfComponentTest;
import atf.reporting.support.observation.ObservationBrowserProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
@ReportBrowserTest
class ReportObservationBrowserComponentTest {
    @Test void nestedObservationsKeepIndependentGlyphsKeyboardAndCompleteEvidence() {
        ObservationBrowserProbe.verify();
    }
}

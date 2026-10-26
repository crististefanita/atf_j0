package atf.ui.tests.extent.component.inspection;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.parity.ApiExchangeParityProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class ApiExchangeParityComponentTest {
    @Test
    void sameCapturedExchangeHasTheSameEvidenceAcrossPublicEntryPointsAndRenderers() {
        ApiExchangeParityProbe.verify();
    }
}

package atf.ui.tests.extent.component.audit;

import atf.testkit.AtfComponentTest;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.support.failurecorrelation.UiFailureCorrelationFixture;
import atf.ui.tests.extent.support.inspection.reconciliation.UiFailureCorrelationProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@AtfComponentTest
class UiFailureCorrelationComponentTest {
    @ParameterizedTest
    @EnumSource(ReportEvidenceFlowMode.class)
    void onlyExplicitlyPropagatedUiFailuresShareTheOriginalFailsEntry(ReportEvidenceFlowMode mode) {
        UiFailureCorrelationProbe.verify(UiFailureCorrelationFixture.publish(mode));
    }
}

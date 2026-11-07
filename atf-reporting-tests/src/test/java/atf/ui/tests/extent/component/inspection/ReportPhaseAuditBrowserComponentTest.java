package atf.ui.tests.extent.component.inspection;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.diagnostic.ReportPhaseAuditProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
@ReportBrowserTest
class ReportPhaseAuditBrowserComponentTest {
    @Test
    void passingSetupDiagnosticDoesNotBecomeTheCauseOfAnotherPhaseFailure() {
        ReportPhaseAuditProbe.verifyPhaseDiagnosticIsolation();
    }

    @Test
    void hidingRenamingOrEmptyingThePhaseLogBandDoesNotSatisfyTheContract() {
        ReportPhaseAuditProbe.rejectsHiddenRenamedAndEmptyPhaseLogBands();
    }

    @Test
    void extentKeepsPhaseMetadataInternalAndPublishesActualFailureAtTheScenario() throws Exception {
        ReportPhaseAuditProbe.verify(false);
    }

    @Test
    void fallbackKeepsTheSameEvidenceOwnershipAndFailureDiagnostic() throws Exception {
        ReportPhaseAuditProbe.verify(true);
    }
}

package atf.ui.tests.extent.component.audit;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.reconciliation.UiApiVerdictProjectionProbe;
import org.junit.jupiter.api.Test;

/** Browser proof for independent UI/API statuses and actionable mixed failures. */
@AtfComponentTest
class UiApiVerdictProjectionComponentTest {
    @Test
    void mixedChannelFailuresRetainTheirCauseAndReplayToTheCorrectScenario() {
        UiApiVerdictProjectionProbe.verifyMixedVerdictsAndFailsReplay();
    }
}

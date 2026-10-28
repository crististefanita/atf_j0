package atf.ui.tests.extent.component.readability;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.readability.NarrowEvidenceReadabilityProbe;
import atf.ui.tests.extent.support.inspection.readability.NarrowEvidenceFailureFixture;
import atf.ui.tests.extent.support.inspection.table.TableViewportReportFixture;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class NarrowEvidenceReadabilityComponentTest {
    @Test
    void extentKeepsLabelsValuesAndFinalRowsReadableAtViewportAndPanelWidths() {
        NarrowEvidenceReadabilityProbe.verify(TableViewportReportFixture.publish(false), false);
    }

    @Test
    void fallbackUsesTheSameReadablePresentationWithoutViewportOnlyBreakpoints() {
        NarrowEvidenceReadabilityProbe.verify(TableViewportReportFixture.publish(true), true);
    }

    @Test
    void failedEvidenceRemainsReadableAfterKeyboardReopeningAndFailsNavigation() {
        NarrowEvidenceReadabilityProbe.verifyFailure(NarrowEvidenceFailureFixture.publish());
    }
}

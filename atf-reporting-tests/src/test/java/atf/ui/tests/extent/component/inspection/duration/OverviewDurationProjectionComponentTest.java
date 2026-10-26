package atf.ui.tests.extent.component.inspection.duration;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.duration.OverviewDurationProjectionProbe;
import org.junit.jupiter.api.Test;

@AtfComponentTest
class OverviewDurationProjectionComponentTest {
    @Test
    void measuredClassesReplaceStaleNativeTimelineInBothMetadataAndChart() {
        OverviewDurationProjectionProbe.verifyMeasuredProjectionAndRefresh();
    }

    @Test
    void zeroIsMeasuredAndOnlyMissingCanonicalDurationsUseNativeFallback() {
        OverviewDurationProjectionProbe.verifyZeroAndNativeFallback();
    }

    @Test
    void existingReportAuditRejectsNumericMarkersWithDifferentTimelineTotals() {
        OverviewDurationProjectionProbe.verifyAuditRejectsNumericButInconsistentProjection();
    }
}

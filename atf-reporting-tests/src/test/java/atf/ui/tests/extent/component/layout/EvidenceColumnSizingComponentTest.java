package atf.ui.tests.extent.component.layout;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.layout.EvidenceColumnSizingProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
@ReportBrowserTest
class EvidenceColumnSizingComponentTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void contentReclaimsSpaceFromShortColumnsWithoutLosingHeaders(boolean fallback) {
        EvidenceColumnSizingProbe.verifyAllocation(fallback);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void allocationFollowsChangedValuesResizeReopeningAndNewTableFamilies(boolean fallback) {
        EvidenceColumnSizingProbe.verifyInvalidation(fallback);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void oracleRejectsEqualWidthWasteAndVerticalCellsEvenWithValidAuditMarkers(boolean fallback) {
        EvidenceColumnSizingProbe.verifyNegative(fallback);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void competingLongValuesUseRealGlyphWidthsIncludingSetupWithoutAnArtificialCap(boolean fallback) {
        EvidenceColumnSizingProbe.verifyCompetingValues(fallback);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void dashboardOwnersAndPreexistingColgroupsUseTheSameAllocation(boolean fallback) {
        EvidenceColumnSizingProbe.verifyExistingOwners(fallback);
    }
}

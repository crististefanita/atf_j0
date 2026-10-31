package atf.ui.tests.extent.component.failurecontext;

import atf.reporting.support.failurecorrelation.FailureNavigationFixture.Owner;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.failurenavigation.FailureNavigationEdgeProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
class PublishedFailureNavigationEdgeComponentTest {
    @ParameterizedTest @CsvSource({"240,1280", "18,392"})
    void repeatedUiFailureLabelsKeepTheirOriginalOrdinalAcrossReloadAndReverseNavigation(int limit, int width) throws Exception {
        FailureNavigationEdgeProbe.verifyRepeatedUiFailures(limit, width);
    }
    @ParameterizedTest @CsvSource({"UI,392", "API,1280", "COMPONENT,392"})
    void fallbackBacklinksOpenTheOriginalComparisonAndSurviveReload(Owner owner, int width) throws Exception {
        FailureNavigationEdgeProbe.verifyFallback(owner, width, false);
    }
    @ParameterizedTest @CsvSource({"false,18", "false,240", "true,18", "true,240"})
    void diagnosticsWithoutAssertionsRemainTypedAndReachableAcrossAllPhases(boolean fallback, int limit) throws Exception {
        FailureNavigationEdgeProbe.verifyDiagnostics(fallback, limit);
    }
    @ParameterizedTest @CsvSource({"API,392", "COMPONENT,1280"})
    void fallbackIdentityPreservesTheInterleavingOfPhaseDiagnosticsAndComparisons(Owner owner, int width) throws Exception {
        FailureNavigationEdgeProbe.verifyFallback(owner, width, true);
    }
}

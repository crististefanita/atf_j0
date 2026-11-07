package atf.ui.tests.extent.component.failurecontext;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.reporting.support.failurecorrelation.TerminalFailureFixture;
import atf.ui.tests.extent.support.inspection.failurenavigation.TerminalFailureIndexProbe;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@AtfComponentTest
@ReportBrowserTest
class TerminalFailureNavigationComponentTest {
    @ParameterizedTest @CsvSource({"false,392,TERMINAL","false,1280,WITH_ASSERTION","true,392,TERMINAL",
            "true,1280,WITH_ASSERTION","false,1280,EXPECTED_PASS","true,392,EXPECTED_PASS",
            "false,1280,EXPECTED_PASS_WITH_FAILED_SIBLING","true,392,EXPECTED_PASS_WITH_FAILED_SIBLING"})
    void originalExecutionCauseIsNavigableWithoutOpeningItsSuccessfulNeighbour(boolean fallback,int width,TerminalFailureFixture.Case kind) {
        TerminalFailureIndexProbe.verify(fallback,width,kind);
    }
}

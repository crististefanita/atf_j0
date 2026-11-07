package atf.ui.tests.extent.component.verdict;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.audit.query.QueryFailureReportProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Query compliance must survive the actual report projection, including contradictory completion. */
@AtfComponentTest
@ReportBrowserTest
class QueryFailureReportComponentTest {
    @ParameterizedTest(name = "query evidence remains FAIL; fallback={0}")
    @ValueSource(booleans = {false, true})
    void aMissingRepeatedQueryCannotBecomeAPassInEitherReport(boolean fallback) throws Exception {
        QueryFailureReportProbe.verify(fallback);
    }
}

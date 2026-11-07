package atf.ui.tests.extent.component.preview;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.preview.LegacyPreviewBudgetProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
@ReportBrowserTest
class LegacyPreviewBudgetComponentTest {
    @ParameterizedTest(name = "raw UI evidence stays bounded; fallback={0}")
    @ValueSource(booleans = {false, true})
    void rawUiFailureEvidenceIsRedactedAndBoundedInTestsAndFails(boolean fallback) throws Exception {
        LegacyPreviewBudgetProbe.verify(fallback);
    }

    @ParameterizedTest(name = "custom budget reaches Fails; fallback={0}")
    @ValueSource(booleans = {false, true})
    void customBudgetIsSharedByOriginalEvidenceAndFailureProjection(boolean fallback) throws Exception {
        LegacyPreviewBudgetProbe.verify(fallback, 18);
    }
}

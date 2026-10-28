package atf.ui.tests.extent.component.failurecontext;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.failurecontext.UiFailureContextProbe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
class UiFailureContextComponentTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void testsAndFailsKeepContextAndSeparateDiagnostics(boolean fallback) throws Exception {
        UiFailureContextProbe.verify(fallback, 240);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void customPreviewBudgetAlsoBoundsFailureContextAndDiagnostics(boolean fallback) throws Exception {
        UiFailureContextProbe.verify(fallback, 18);
    }
}

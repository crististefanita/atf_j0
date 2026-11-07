package atf.ui.tests.extent.component.layout;

import atf.reporting.browser.junit.ReportBrowserTest;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.navigation.FallbackLongTitleProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@AtfComponentTest
@ReportBrowserTest
class FallbackLongTitleComponentTest {
    @ParameterizedTest
    @ValueSource(ints = {1280, 760, 392})
    void completeTitlesRemainContainedAndKeyboardNavigationRetainsIdentity(int width) throws Exception {
        FallbackLongTitleProbe.verify(width, false);
    }

    @Test
    void containmentOracleRejectsTheOriginalNoWrapRegression() throws Exception {
        FallbackLongTitleProbe.verify(1280, true);
    }
}

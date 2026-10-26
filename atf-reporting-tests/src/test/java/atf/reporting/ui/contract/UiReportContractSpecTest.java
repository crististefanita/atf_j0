package atf.reporting.ui.contract;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class UiReportContractSpecTest {
    @Test
    void resolvesTheDefaultProgrammaticContractWithoutBrowserState() {
        UiReportContractSpec spec = UiReportContractSpec.resolve(CompleteContract.class);

        assertEquals(EnumSet.allOf(ReportUiContract.Tab.class), spec.tabs());
        for (UiReportCheck check : UiReportCheck.values()) {
            assertTrue(spec.checks().contains(check));
        }
    }

    @Test
    void rejectsScreenChecksWithoutTheTestsTab() {
        assertThrows(IllegalArgumentException.class, () -> new UiReportContractSpec(
                "invalid", "atf.ui.report", Set.of(ReportUiContract.Tab.OVERVIEW),
                Set.of(UiReportCheck.SCREEN_VALIDATION)));
    }

    @UiExtentReportContract
    private static final class CompleteContract { }
}

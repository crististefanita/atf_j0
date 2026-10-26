package atf.reporting.ui.failure;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class UiFailureReportSpecTest {
    @Test
    void resolvesAnnotatedFailureContractWithoutTestKitDependencies() {
        UiFailureReportSpec spec = UiFailureReportSpec.resolve(StaticFailureContract.class);

        assertEquals("static-failure-contract", spec.id());
        assertEquals(UiFailureReportInput.STATIC_UI_REPORT, spec.input());
        assertEquals("atf.failure.report", spec.reportProperty());
        assertEquals(Set.of(UiFailureSurface.SETUP, UiFailureSurface.ASSERTION), spec.surfaces());
    }

    @Test
    void rejectsAnOwnerWithoutTheFailureContractAnnotation() {
        assertThrows(IllegalArgumentException.class,
                () -> UiFailureReportSpec.resolve(UiFailureReportSpecTest.class));
    }

    @Test
    void rejectsAnEmptyFailureSurfaceSelection() {
        assertThrows(IllegalArgumentException.class,
                () -> new UiFailureReportSpec("failure", UiFailureReportInput.STATIC_API_REPORT,
                        "atf.failure.report", Set.of()));
    }

    @UiFailureReportAudit(
            id = "static-failure-contract",
            input = UiFailureReportInput.STATIC_UI_REPORT,
            reportProperty = "atf.failure.report",
            surfaces = {UiFailureSurface.SETUP, UiFailureSurface.ASSERTION})
    private static final class StaticFailureContract {
    }
}

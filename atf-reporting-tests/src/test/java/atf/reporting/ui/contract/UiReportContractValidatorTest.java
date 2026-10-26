package atf.reporting.ui.contract;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class UiReportContractValidatorTest {
    private static final UiReportContractSpecView ALL_CHECKS =
            check -> EnumSet.allOf(UiReportCheck.class).contains(check);

    @Test
    void acceptsACompleteTestsSnapshotWithoutBrowserState() {
        var result = new UiReportContractValidator().validate(snapshot(ReportUiContract.Tab.TESTS,
                1, 1, 1, 0, 0, true, true), ALL_CHECKS);

        assertTrue(result.valid(), result::failureSummary);
    }

    @Test
    void reportsStyleAndDisclosureFailuresAsActionableAssertions() {
        var result = new UiReportContractValidator().validate(snapshot(ReportUiContract.Tab.TESTS,
                1, 1, 1, 1, 0, false, true), ALL_CHECKS);

        assertFalse(result.valid());
        assertTrue(result.failureSummary().contains("Rendered style"));
        assertTrue(result.failureSummary().contains("Collapsed by default"));
    }

    @Test
    void validatesFailsProjectionWithoutRequiringAPlaywrightSession() {
        var result = new UiReportContractValidator().validate(snapshot(ReportUiContract.Tab.FAILS,
                0, 0, 0, 0, 2, true, true), ALL_CHECKS);

        assertTrue(result.valid(), result::failureSummary);
    }

    @Test
    void rejectsNegativeObservedCountsAtTheSnapshotBoundary() {
        assertThrows(IllegalArgumentException.class, () -> snapshot(
                ReportUiContract.Tab.TESTS, -1, 1, 1, 0, 0, true, true));
    }

    private static UiReportTabSnapshot snapshot(ReportUiContract.Tab tab,
                                                int owners,
                                                int phases,
                                                int validations,
                                                int openDisclosures,
                                                int failures,
                                                boolean styleValid,
                                                boolean screenValid) {
        return new UiReportTabSnapshot(tab, true, true, 4, owners, 1, phases, validations,
                0, 0, true, failures, failures == 0, openDisclosures, 1, styleValid,
                styleValid ? List.of() : List.of("synthetic style drift"), screenValid,
                screenValid ? List.of() : List.of("synthetic screen drift"));
    }
}

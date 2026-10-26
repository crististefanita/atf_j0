package atf.core.ui;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest

class AtfUiActionRecordTest {
    @Test
    void keepsOrderedActionContractAndUsesCanonicalPassLabel() {
        var action = new AtfUiActionRecord(1, "Click", "role=button[name=Save]",
                "save dialog opens", "click completed", 547, AtfReportStatus.PASS);

        assertAll(
                () -> assertEquals("547 ms", action.durationText()),
                () -> assertEquals("Pass", action.resultLabel()),
                () -> assertEquals(AtfReportStatus.PASS, action.status()),
                () -> assertEquals(AtfUiEvidenceSource.ACTION, action.source()));
    }

    @Test
    void rejectsInvalidActionIdentityAndDuration() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AtfUiActionRecord(0, "Click", "target", "expected", "observed", 1, AtfReportStatus.PASS)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AtfUiActionRecord(1, "", "target", "expected", "observed", 1, AtfReportStatus.PASS)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AtfUiActionRecord(1, "Click", "target", "expected", "observed", -1, AtfReportStatus.PASS)));
    }
}

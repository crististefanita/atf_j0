package atf.core.reporting;

import atf.testkit.AtfUnitTest;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class AtfConsoleEventInteropTest {
    private enum ExternalType { START, COMPLETE, REPORT_PUBLISHED, UNKNOWN }
    private enum ExternalScope { SCENARIO, REPORT, UNKNOWN }
    private enum ExternalStatus { PASS, UNKNOWN }

    @Test
    void compatibleExternalEnumsPreserveEventDataWithoutDependingOnTheirOwner() {
        var event = AtfConsoleEvent.fromExternalEnums(ExternalType.COMPLETE,
                ExternalScope.SCENARIO, "purchase", ExternalStatus.PASS, 27, "field", "value");

        assertEquals(new AtfConsoleEvent(AtfConsoleEvent.Type.COMPLETE,
                AtfConsoleEvent.Scope.SCENARIO, "purchase", AtfReportStatus.PASS,
                27, "field", "value"), event);
    }

    @Test
    void unknownVocabularyFailsInsteadOfDowngradingOrDroppingTheEvent() {
        assertThrows(IllegalArgumentException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.UNKNOWN, ExternalScope.SCENARIO, "", null, 0, "", ""));
        assertThrows(IllegalArgumentException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.START, ExternalScope.UNKNOWN, "", null, 0, "", ""));
        assertThrows(IllegalArgumentException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.COMPLETE, ExternalScope.SCENARIO, "", ExternalStatus.UNKNOWN, 0, "", ""));
    }

    @Test
    void requiredEnumsAndCompletionStatusRemainRequired() {
        assertThrows(NullPointerException.class, () -> AtfConsoleEvent.fromExternalEnums(
                null, ExternalScope.SCENARIO, "", null, 0, "", ""));
        assertThrows(NullPointerException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.START, null, "", null, 0, "", ""));
        assertThrows(NullPointerException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.COMPLETE, ExternalScope.SCENARIO, "", null, 0, "", ""));
    }

    @Test
    void publicationAndDurationInvariantsAlsoApplyToExternalEvents() {
        Path report = Path.of("reports", "extent.html").toAbsolutePath().normalize();
        assertEquals(AtfConsoleEvent.reportPublished(report), AtfConsoleEvent.fromExternalEnums(
                ExternalType.REPORT_PUBLISHED, ExternalScope.REPORT, "Extent report",
                null, 0, "path", report.toString()));
        assertThrows(IllegalArgumentException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.REPORT_PUBLISHED, ExternalScope.REPORT, "", null, 0, "path", "relative.html"));
        assertThrows(IllegalArgumentException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.REPORT_PUBLISHED, ExternalScope.SCENARIO, "", null, 0, "path", report.toString()));
        assertThrows(IllegalArgumentException.class, () -> AtfConsoleEvent.fromExternalEnums(
                ExternalType.START, ExternalScope.SCENARIO, "", null, -1, "", ""));
    }
}

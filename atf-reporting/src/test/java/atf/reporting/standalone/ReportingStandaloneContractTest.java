package atf.reporting.standalone;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.duration.AtfDurationFormatter;
import atf.reporting.security.SensitiveDataRedactor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the reporting module can test its public contracts without ATF modules. */
class ReportingStandaloneContractTest {
    @Test
    void typedStatusAndDurationContractsAreAvailableStandalone() {
        assertEquals(AtfReportStatus.PASS, AtfReportStatus.valueOf("PASS"));
        assertEquals("12 ms", AtfDurationFormatter.format(java.time.Duration.ofMillis(12)));
    }

    @Test
    void sensitiveValuesAreRedactedWithoutAnAtfAdapter() {
        String safe = new SensitiveDataRedactor().redactJson("{\"token\":\"secret-value\",\"ok\":true}");
        assertFalse(safe.contains("secret-value"));
        assertEquals("{\"token\":\"***REDACTED***\",\"ok\":true}", safe);
    }

    @Test
    void genericRedactorCoversCredentialVariantsInTextQueryAndPath() {
        var redactor = new SensitiveDataRedactor();
        String safe = redactor.redactText("Authorization: Bearer auth-secret; "
                + "access_token=access-secret; ?refresh-token=refresh-secret&keep=visible "
                + "/token/path-secret");

        assertFalse(safe.contains("auth-secret"));
        assertFalse(safe.contains("access-secret"));
        assertFalse(safe.contains("refresh-secret"));
        assertFalse(safe.contains("path-secret"));
        assertTrue(safe.contains("keep=visible"));
        assertFalse(redactor.redactText("Bearer raw-consumer-token")
                .contains("raw-consumer-token"));
    }

    @Test
    void genericRedactorMasksStructuredCredentialFieldsAndHeaderAliases() {
        var redactor = new SensitiveDataRedactor();
        String safe = redactor.redactJson("{\"accessToken\":\"access-secret\","
                + "\"client_secret\":\"client-secret\",\"cookie\":\"session-secret\"}");

        assertFalse(safe.contains("access-secret"));
        assertFalse(safe.contains("client-secret"));
        assertFalse(safe.contains("session-secret"));
        assertEquals("***REDACTED***", redactor.redactField("X-Api-Key", "api-key-secret"));
    }
    @Test void capturedUiValuesPreserveSignificantWhitespaceForBothSources() {
        String captured = " \t\r\nState ♠ \n ";
        for (var source : AtfUiEvidenceSource.values()) {
            var row = new AtfUiActionRecord(7, "  Observe  ", "  #target  ", captured, captured, 1, AtfReportStatus.FAIL, source);
            assertEquals(captured, row.expected()); assertEquals(captured, row.observed());
            assertEquals("Observe", row.operation()); assertEquals("#target", row.target());
            assertEquals(7, row.sequence()); assertEquals(AtfReportStatus.FAIL, row.status());
        }
    }
    @Test void emptyCapturedUiValuesKeepTheExistingNullContract() {
        var row = new AtfUiActionRecord(1, "observe", "target", null, " \t ", 0, AtfReportStatus.INFO);
        assertEquals("", row.expected()); assertEquals(" \t ", row.observed());
    }
}

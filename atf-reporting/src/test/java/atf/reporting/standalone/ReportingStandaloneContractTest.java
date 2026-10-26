package atf.reporting.standalone;

import atf.reporting.contract.AtfReportStatus;
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
}

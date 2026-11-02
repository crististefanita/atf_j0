package atf.reporting.standalone.timing;

import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportingStandaloneTransportTimingTest {
    @Test void aCapturedZeroRemainsAnExplicitTransportMeasurement() {
        String attributes = ReportEvidenceMarkup.executionAttributes(evidence(Duration.ZERO), new SensitiveDataRedactor());
        assertTrue(attributes.contains("data-atf-transport-duration-ms=\"0\""));
    }
    @Test void missingTransportTimeDoesNotInventAZero() {
        String attributes = ReportEvidenceMarkup.executionAttributes(evidence(null), new SensitiveDataRedactor());
        assertFalse(attributes.contains("data-atf-transport-duration-ms"));
    }
    private static ReportExecutionEvidence evidence(Duration duration) {
        return new ReportExecutionEvidence(null, 1, duration, List.of(), List.of(), null, null);
    }
}

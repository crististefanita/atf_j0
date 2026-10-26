package atf.core.http;

import atf.testkit.AtfUnitTest;
import atf.core.http.response.ApiBodyEvidence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ApiBodyEvidenceTest {
    @Test
    void unavailableBodyCarriesAVisibleBoundedTransportDiagnostic() {
        String evidence = ApiBodyEvidence.unavailable(
                "Chromium response body", new IllegalStateException("payload released"));

        assertTrue(ApiBodyEvidence.isUnavailable(evidence));
        assertTrue(evidence.contains("IllegalStateException"));
        assertTrue(evidence.contains("payload released"));
        assertFalse(evidence.isBlank());
    }

    @Test
    void missingSourceOrDetailUsesNeutralFallbacks() {
        String evidence = ApiBodyEvidence.unavailable("  ", "  ");

        assertTrue(evidence.contains("API transport"));
        assertTrue(evidence.contains("payload was not exposed"));
        assertFalse(ApiBodyEvidence.isUnavailable("{}"));
    }
}

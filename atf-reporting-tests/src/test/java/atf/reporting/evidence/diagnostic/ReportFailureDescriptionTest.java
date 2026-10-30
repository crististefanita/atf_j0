package atf.reporting.evidence.diagnostic;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Generic failure-graph checks moved from the AutoEx adapter to their reporting owner. */
@AtfUnitTest
class ReportFailureDescriptionTest {
    @Test
    void cyclesAndSuppressedFailuresKeepFourDistinctNodesInObservationOrder() {
        var failure = new IllegalStateException("root");
        var cause = new IllegalArgumentException("cause");
        failure.initCause(cause);
        cause.initCause(failure);
        failure.addSuppressed(new UnsupportedOperationException("suppressed-one"));
        failure.addSuppressed(new ArithmeticException("suppressed-two"));
        failure.addSuppressed(new IndexOutOfBoundsException("outside-bound"));
        String diagnostic = ReportFailureDescription.describe(failure);
        assertEquals("IllegalStateException: root <- IllegalArgumentException: cause <- "
                + "UnsupportedOperationException: suppressed-one <- ArithmeticException: suppressed-two", diagnostic);
        assertSame(cause, failure.getCause());
        assertSame(failure, cause.getCause());
        assertEquals(3, failure.getSuppressed().length, "description never rewrites the failure graph");
    }

    @Test
    void theSafePreviewKeepsExactly240CharactersIncludingTheEllipsis() {
        String diagnostic = ReportFailureDescription.describe(new IllegalStateException("detail ".repeat(400)));
        assertEquals(240, diagnostic.length());
        assertTrue(diagnostic.endsWith("…"));
    }

    @Test
    void redactionPrecedesTheBoundEvenWhenASecretExtendsPastThePreview() {
        String secret = "API_DIAGNOSTIC_CANARY_" + "x".repeat(1_200);
        String diagnostic = ReportFailureDescription.describe(new IllegalStateException("{\"password\":\"" + secret + "\"}"));
        assertFalse(diagnostic.contains("API_DIAGNOSTIC_CANARY_"));
        assertTrue(diagnostic.contains("***REDACTED***"));
        assertTrue(diagnostic.length() <= 240);
    }
}

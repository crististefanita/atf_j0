package atf.testkit.assertions;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AuditedAssertionsTest {
    @Test void booleanDiagnosticsStayLazyAndKeepOneAuditedComparison() {
        var rows = new RecordedAssertions();
        var calls = new AtomicInteger();
        java.util.function.Supplier<String> message = () -> { calls.incrementAndGet(); return "controlled diagnostic"; };
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "booleanDiagnosticsStayLazyAndKeepOneAuditedComparison", rows)) {
            AuditedAssertions.assertFalse(false, message);
            AuditedAssertions.assertTrue(true, message);
            assertEquals(0, calls.get(), "Passing checks must not evaluate lazy diagnostics");
            var failure = assertThrows(org.opentest4j.AssertionFailedError.class,
                    () -> AuditedAssertions.assertFalse(true, message));
            assertTrue(failure.getMessage().contains("controlled diagnostic"));
        }
        assertEquals(1, calls.get());
        assertEquals(List.of(true, true, false), rows.checks().stream().map(RecordedAssertions.Check::passed).toList());
        assertEquals("false", rows.checks().getLast().expected());
        assertEquals("true", rows.checks().getLast().actual());
    }
    @Test
    void recordsExpectedExceptionsAsPassingChecksWithoutTheirInnerFailures() {
        var rows = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "recordsExpectedExceptionsAsPassingChecksWithoutTheirInnerFailures", rows)) {
            var failure = AuditedAssertions.assertThrows(AssertionError.class,
                    () -> AuditedAssertions.assertTrue(false));
            assertNotNull(failure);
        }
        assertEquals(1, rows.checks().size());
        assertTrue(rows.checks().getFirst().passed());
        assertEquals("org.opentest4j.AssertionFailedError", rows.checks().getFirst().actual());
    }

    @Test
    void delegatesNumericOverloadsAndKeepsMessageSuppliersLazy() {
        var rows = new RecordedAssertions();
        var evaluations = new AtomicInteger();
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "delegatesNumericOverloadsAndKeepsMessageSuppliersLazy", rows)) {
            AuditedAssertions.assertEquals(16, 16L);
            AuditedAssertions.assertEquals(2, Integer.valueOf(2));
            AuditedAssertions.assertEquals(1.0, 1.01, 0.1);
            AuditedAssertions.assertTrue(true, () -> "unused " + evaluations.incrementAndGet());
            assertEquals(0, evaluations.get());
            assertThrows(AssertionError.class, () -> AuditedAssertions.assertTrue(false,
                    () -> "evaluated " + evaluations.incrementAndGet()));
        }
        assertEquals(1, evaluations.get());
        assertEquals(5, rows.checks().size());
        assertFalse(rows.checks().getLast().passed());
    }

    @Test
    void keepsAllEvaluatedRowsAndTheOriginalJUnitAggregateFailure() {
        var rows = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "keepsAllEvaluatedRowsAndTheOriginalJUnitAggregateFailure", rows)) {
            var failure = assertThrows(MultipleFailuresError.class, () -> AuditedAssertions.assertAll(
                    () -> AuditedAssertions.assertEquals(1, 2),
                    () -> AuditedAssertions.assertFalse(true)));
            assertEquals(2, failure.getFailures().size());
        }
        assertEquals(2, rows.checks().size());
        assertTrue(rows.checks().stream().noneMatch(RecordedAssertions.Check::passed));
    }

    @Test
    void comparesTheFullValueAndKeepsTheConfiguredCharacterLimitIncludingEllipsis() {
        var rows = new RecordedAssertions();
        String prefix = "x".repeat(240);
        assertThrows(AssertionError.class, () -> rows.equals("different suffix", prefix + "a", prefix + "b"));
        assertFalse(rows.checks().getFirst().passed());
        assertEquals(240, rows.checks().getFirst().actual().length());
        assertTrue(rows.checks().getFirst().actual().endsWith("…"));
        var custom = new RecordedAssertions(10);
        custom.equals("custom", "x".repeat(11), "x".repeat(11));
        assertEquals(10, custom.checks().getFirst().actual().length());
        assertTrue(custom.checks().getFirst().actual().endsWith("…"));
        rows.equals("exact boundary", prefix, prefix);
        assertEquals(prefix, rows.checks().getLast().actual());
    }
    @Test
    void namedToleranceKeepsNumericFactsAndTheOriginalJUnitVerdict() {
        var rows = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(), "namedToleranceKeepsNumericFactsAndTheOriginalJUnitVerdict", rows)) {
            AuditedAssertions.assertEquals(100.0, 101.5, 2.0, "PCM frames");
            assertThrows(AssertionError.class, () ->
                    AuditedAssertions.assertEquals(100.0, 105.0, 2.0, "outside frame tolerance"));
        }
        assertEquals(2, rows.checks().size());
        var accepted = rows.checks().getFirst();
        assertTrue(accepted.name().contains("PCM frames"));
        assertEquals("100.0 ± 2.0", accepted.expected());
        assertEquals("101.5", accepted.actual());
        assertTrue(accepted.passed());
        assertFalse(rows.checks().getLast().passed());
        assertEquals("105.0", rows.checks().getLast().actual());
    }
    @Test void notEqualsKeepsTheCallerMessageAndOriginalFailureIdentity() {
        var checks = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(), "notEqualsKeepsTheCallerMessageAndOriginalFailureIdentity", checks)) {
            AuditedAssertions.assertNotEquals("old", "new", "Changed state");
            var failure = assertThrows(org.opentest4j.AssertionFailedError.class,
                    () -> AuditedAssertions.assertNotEquals("same", "same", "State must change"));
            assertSame(failure, checks.failure());
            assertTrue(failure.getMessage().contains("State must change"));
        }
        assertEquals(2, checks.checks().size());
        assertTrue(checks.checks().getFirst().passed());
        assertFalse(checks.checks().getLast().passed());
        assertTrue(checks.checks().getLast().name().endsWith(" · State must change"));
    }

    @Test void primitiveValuesAreCapturedWithoutChangingTheJUnitVerdict() {
        var checks = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(), "primitiveValuesAreCapturedWithoutChangingTheJUnitVerdict", checks)) {
            AuditedAssertions.assertArrayEquals(new byte[] {-1, 0, 1}, new byte[] {-1, 0, 1});
            AuditedAssertions.assertArrayEquals(new int[] {-1, 0, 65536}, new int[] {-1, 0, 65536});
        }
        assertEquals(2, checks.checks().size());
        assertEquals("[-1, 0, 1]", checks.checks().getFirst().expected());
        assertEquals("[-1, 0, 65536]", checks.checks().getLast().actual());
        assertTrue(checks.checks().stream().allMatch(RecordedAssertions.Check::passed));
    }
    @Test void messageOverloadsKeepIdentityNullAndLazySupplierSemantics() {
        var checks = new RecordedAssertions();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        Object same = new Object();
        try (var scope = AuditedAssertionScope.open(getClass().getName(), "messageOverloadsKeepIdentityNullAndLazySupplierSemantics", checks)) {
            AuditedAssertions.assertFalse(false, () -> { calls.incrementAndGet(); return "lazy message"; });
            assertEquals(0, calls.get());
            var failure = assertThrows(org.opentest4j.AssertionFailedError.class,
                    () -> AuditedAssertions.assertFalse(true, () -> { calls.incrementAndGet(); return "lazy message"; }));
            assertTrue(failure.getMessage().contains("lazy message"));
            AuditedAssertions.assertNull(null, "null contract");
            assertThrows(org.opentest4j.AssertionFailedError.class, () -> AuditedAssertions.assertNull(same, "null contract"));
            AuditedAssertions.assertSame(same, same, "identity contract");
            assertThrows(org.opentest4j.AssertionFailedError.class, () -> AuditedAssertions.assertSame(same, new Object(), "identity contract"));
        }
        assertEquals(1, calls.get());
        assertEquals(6, checks.checks().size());
        assertEquals(3, checks.checks().stream().filter(row -> !row.passed()).count());
    }
    @Test void mismatchesKeepTheOriginalAssertionErrorAndFailedValues() {
        var checks = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(), "mismatchesKeepTheOriginalAssertionErrorAndFailedValues", checks)) {
            assertThrows(org.opentest4j.AssertionFailedError.class,
                    () -> AuditedAssertions.assertArrayEquals(new byte[] {1}, new byte[] {2}));
            assertThrows(org.opentest4j.AssertionFailedError.class,
                    () -> AuditedAssertions.assertArrayEquals(new int[] {1}, new int[] {2}));
        }
        assertEquals(2, checks.checks().size());
        assertFalse(checks.checks().getFirst().passed());
        assertEquals("[2]", checks.checks().getLast().actual());
    }
}

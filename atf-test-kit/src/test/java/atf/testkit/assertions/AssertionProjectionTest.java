package atf.testkit.assertions;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AssertionProjectionTest {
    @Test void completeSanitizedValuesRemainAvailableBesideTheBoundedPreview() {
        var observed = new java.util.ArrayList<AssertionValueDisplay.Projection>();
        var display = new AssertionValueDisplay(8, (field, text) -> text.replace("secret", "MASK"), observed::add);
        var rows = new RecordedAssertions(display);
        rows.equals("long value", "secret-" + "x".repeat(30), "secret-" + "x".repeat(30));
        assertEquals(8, rows.checks().getFirst().expected().length());
        assertEquals(2, observed.size());
        assertEquals(AssertionValueDisplay.Role.EXPECTED, observed.getFirst().role());
        assertEquals(AssertionValueDisplay.Role.ACTUAL, observed.getLast().role());
        assertTrue(observed.getFirst().truncated());
        assertEquals("MASK-" + "x".repeat(30), observed.getFirst().fullText());
    }

    @Test void aBrokenProjectionObserverCannotReplaceTheOriginalFailureOrBeRetriedByAGroup() {
        var capture = new IllegalStateException("Evidence receiver failed");
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var rows = new RecordedAssertions(new AssertionValueDisplay(8, (field, text) -> text,
                value -> { calls.incrementAndGet(); throw capture; }));
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "aBrokenProjectionObserverCannotReplaceTheOriginalFailureOrBeRetriedByAGroup", rows)) {
            var failure = assertThrows(org.opentest4j.MultipleFailuresError.class,
                    () -> AuditedAssertions.assertAll(() -> AuditedAssertions.assertEquals(1, 2)));
            assertEquals(1, failure.getFailures().size());
            var original = assertInstanceOf(org.opentest4j.AssertionFailedError.class, failure.getFailures().getFirst());
            assertEquals(1, original.getExpected().getValue());
            assertEquals(2, original.getActual().getValue());
            assertSame(capture, original.getSuppressed()[0]);
        }
        assertEquals(1, calls.get());
    }
    @Test void sanitizesTheCompleteValueBeforeApplyingTheCharacterLimit() {
        String secret = "{\"password\":\"synthetic-canary-" + "q".repeat(280) + "\"}";
        var seen = new java.util.ArrayList<String>();
        var display = new AssertionValueDisplay(240, (field, text) -> {
            seen.add(text);
            return text.equals(secret) ? "{\"password\":\"REDACTED\"}" : text;
        });
        var rows = new RecordedAssertions(display);
        rows.equals("json", secret, secret);
        assertTrue(seen.contains(secret));
        assertEquals("{\"password\":\"REDACTED\"}", rows.checks().getFirst().actual());
        assertEquals(240, display.display(null, "x".repeat(241)).length());
        assertEquals(1, new AssertionValueDisplay(1, (field, text) -> text).display(null, "long").length());
    }

    @Test void truncationPreservesSupplementaryCharactersAndTheSanitizedOriginal() {
        String crossing = "x".repeat(238) + "😀" + "suffix";
        String fitting = "x".repeat(237) + "😀" + "suffix";
        var observed = new java.util.ArrayList<AssertionValueDisplay.Projection>();
        var sanitized = new java.util.ArrayList<String>();
        var display = new AssertionValueDisplay(240, (field, value) -> {
            sanitized.add(value);
            return value;
        }, observed::add);

        String crossingPreview = display.comparison("crossing", AssertionValueDisplay.Role.ACTUAL, crossing);
        String fittingPreview = display.comparison("fitting", AssertionValueDisplay.Role.ACTUAL, fitting);

        assertEquals("x".repeat(238) + "…", crossingPreview);
        assertEquals(239, crossingPreview.length());
        assertEquals("x".repeat(237) + "😀…", fittingPreview);
        assertEquals(240, fittingPreview.length());
        assertEquals(List.of(crossing, fitting), sanitized);
        assertEquals(List.of(crossing, fitting), observed.stream().map(AssertionValueDisplay.Projection::fullText).toList());
        assertTrue(observed.stream().allMatch(AssertionValueDisplay.Projection::truncated));
    }

    @Test void recordsTheSingleJUnitEvaluationAndKeepsBrokenDisplayOutOfItsVerdict() {
        var comparisons = new AtomicInteger();
        Object expected = new Object() {
            @Override public boolean equals(Object actual) { return comparisons.incrementAndGet() == 1; }
            @Override public int hashCode() { return 1; }
            @Override public String toString() { throw new IllegalStateException("synthetic display failure"); }
        };
        var rows = new RecordedAssertions();
        rows.equals("single evaluation", expected, new Object());
        assertEquals(1, comparisons.get());
        assertTrue(rows.checks().getFirst().passed());
        assertEquals("<display unavailable>", rows.checks().getFirst().expected());
        Object broken = new Object() { @Override public String toString() { throw new AssertionError("display"); } };
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "recordsTheSingleJUnitEvaluationAndKeepsBrokenDisplayOutOfItsVerdict", rows)) {
            AuditedAssertions.assertEquals(broken, broken);
            AuditedAssertions.assertNotEquals(broken, new Object());
        }
        assertTrue(rows.checks().stream().allMatch(RecordedAssertions.Check::passed));
        assertDoesNotThrow(() -> AuditedAssertions.assertNotEquals(broken, new Object()));
    }

    @Test void explicitRecorderAndFacadeShareExpectedFailureSuppression() {
        var rows = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "explicitRecorderAndFacadeShareExpectedFailureSuppression", rows)) {
            AuditedAssertions.assertThrows(AssertionError.class, () -> rows.equals("negative", 1, 2));
            AuditedAssertions.assertDoesNotThrow(() -> rows.equals("positive", 1, 1));
            AuditedAssertions.assertThrows(AssertionError.class, () -> AuditedAssertions.assertAll(
                    () -> rows.isTrue("nested negative", false), () -> AuditedAssertions.assertFalse(true)));
        }
        assertEquals(3, rows.checks().size());
        assertTrue(rows.checks().stream().allMatch(RecordedAssertions.Check::passed));
    }

    @Test void aDetachedRecorderAlsoFeedsTheBoundAuditThroughItsSafeDisplayPolicy() {
        var bound = new RecordedAssertions(new AssertionValueDisplay(240,
                (field, text) -> text.equals("synthetic secret") ? "REDACTED" : text));
        var detached = new RecordedAssertions();
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "aDetachedRecorderAlsoFeedsTheBoundAuditThroughItsSafeDisplayPolicy", bound)) {
            detached.equals("value", "synthetic secret", "synthetic secret");
        }
        assertEquals(1, detached.checks().size());
        assertEquals(1, bound.checks().size());
        assertEquals("REDACTED", bound.checks().getFirst().actual());
    }
}

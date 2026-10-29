package atf.validation.protocol;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.*;
import atf.core.reporting.inventory.ValidationSink;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.*;
import atf.validation.publishing.ResponseContractReporter;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ValidationProtocolTest {
    @Test void closingFailureIsSuppressedOnTheOriginalStrictVerdict() {
        var scope = new Scope(); scope.closeFailure = new IllegalStateException("close unavailable");
        var failure = assertThrows(ApiValidationException.class, () -> validate(scope, 503, false, true));
        assertEquals(List.of(scope.closeFailure), Arrays.asList(failure.getSuppressed()));
        assertEquals(1, scope.publications); assertEquals(1, scope.completions); assertEquals(1, scope.closes);
        assertEquals("200", scope.rows.getFirst().expected()); assertEquals("503", scope.rows.getFirst().actual());
    }

    @Test void evaluationPublicationAndCloseKeepOnePrimaryAndBothSecondaryCauses() {
        var scope = new Scope(); scope.publishFailure = new IllegalStateException("publisher unavailable");
        scope.closeFailure = new IllegalStateException("close unavailable");
        var failure = assertThrows(NullPointerException.class, () -> new ResponseContractReporter().validate(
                title -> scope, "contract", response(200), null, false));
        assertEquals(List.of(scope.publishFailure, scope.closeFailure), Arrays.asList(failure.getSuppressed()));
        assertEquals(1, scope.closes);
    }

    @Test void publishingAndCompletionAreNeverRetriedAfterTheirOwnFailure() {
        for (boolean publish : List.of(false, true)) {
            var scope = new Scope(); var primary = new IllegalStateException("controlled publication failure");
            if (publish) scope.publishFailure = primary; else scope.completeFailure = primary;
            scope.closeFailure = new IllegalStateException("controlled closing failure");
            assertSame(primary, assertThrows(IllegalStateException.class, () -> validate(scope, 200, false, true)));
            assertEquals(1, scope.publications); assertEquals(publish ? 0 : 1, scope.completions); assertEquals(1, scope.closes);
            assertEquals(List.of(scope.closeFailure), Arrays.asList(primary.getSuppressed()));
        }
    }

    @Test void theSameFailureFromPublicationAndClosingDoesNotMaskItself() {
        var scope = new Scope(); scope.publishFailure = scope.closeFailure = new IllegalStateException("same controlled cause");
        assertSame(scope.publishFailure, assertThrows(IllegalStateException.class, () -> validate(scope, 200, false, true)));
        assertEquals(0, scope.publishFailure.getSuppressed().length); assertEquals(1, scope.publications); assertEquals(1, scope.closes);
    }

    @Test void closeFailureCannotFabricateAPassWarningOrReturnedNonthrowingDecision() {
        for (var mode : List.of(new boolean[]{false, true}, new boolean[]{true, true}, new boolean[]{false, false})) {
            var scope = new Scope(); scope.closeFailure = new IllegalStateException("close unavailable");
            int status = mode[1] && !mode[0] ? 200 : 503;
            assertSame(scope.closeFailure, assertThrows(IllegalStateException.class, () -> validate(scope, status, mode[0], mode[1])));
            assertEquals(1, scope.publications); assertEquals(1, scope.closes);
        }
    }

    @Test void aPublishedTableRefusesCallbackEvaluationBeforeAnySideEffect() {
        var table = new ValidationTable(); table.add("initial", "one", "one", true); table.decide(false);
        var before = table.rows(); var calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> table.check("late", "true", "true", () -> { calls.incrementAndGet(); return true; }));
        assertEquals(0, calls.get()); assertEquals(before, table.rows());
    }

    @Test void invalidIdentitiesAndNullCallbacksDoNotConsumeThePredicateOrReserveARow() {
        var table = new ValidationTable(); table.add("First", "one", "one", true); var calls = new AtomicInteger();
        for (String name : Arrays.asList("first", " FIRST ", "", null)) {
            assertThrows(IllegalArgumentException.class, () -> table.check(name, "true", "true", () -> { calls.incrementAndGet(); return true; }));
        }
        assertThrows(NullPointerException.class, () -> table.check("second", "true", "true", null));
        table.check("second", "true", "true", () -> { calls.incrementAndGet(); return true; });
        assertEquals(1, calls.get()); assertEquals(2, table.rows().size());
    }

    @Test void anEligibleCallbackFailureIsEvaluatedOnceAndRetainsFailureEvidence() {
        var calls = new AtomicInteger(); var table = new ValidationTable();
        table.check("eligible", "completes", "observed", () -> { calls.incrementAndGet(); throw new IllegalStateException("predicate failed"); });
        assertEquals(1, calls.get()); assertFalse(table.rows().getFirst().passed());
        assertTrue(table.rows().getFirst().actual().contains("predicate failed"));
        assertEquals(AtfReportStatus.FAIL, table.decide(false).status());
    }

    private static ValidationDecision validate(Scope scope, int status, boolean warning, boolean strict) {
        return new ResponseContractReporter().validate(title -> scope, "contract", response(status), new ResponseContract().status(200), warning, strict);
    }
    private static ApiResponse response(int status) { return new ApiResponse(status, "{}", Map.of(), Duration.ZERO, "https://fixture.test"); }
    private static final class Scope implements ValidationSink.Scope {
        RuntimeException closeFailure, publishFailure, completeFailure;
        int publications, completions, closes;
        List<AtfAssertion> rows = List.of();
        public void assertions(Collection<AtfAssertion> value) { publications++; if (publishFailure != null) throw publishFailure; rows = List.copyOf(value); }
        public void complete(AtfReportStatus status) { completions++; if (completeFailure != null) throw completeFailure; }
        public void close() { closes++; if (closeFailure != null) throw closeFailure; }
    }
}

package atf.ui.recovery;

import java.time.Duration;
import java.util.Optional;

/** A probe/reobservation decision, never a replayed business action or a transport-duration measurement. */
public record UiRecoveryTrace(String operation, int attempt, Decision decision, Reason reason,
                               Optional<UiRecoverySignal> signal, Duration probeTime, Duration backoffTime,
                               Duration totalTime, String policySource) {
    public enum Dom { READY, PENDING, BUSINESS_INVALID, SESSION_MISMATCH, AMBIGUOUS }
    public enum Decision { INITIAL_READY, REOBSERVE, RECOVERED, REFUSED, EXHAUSTED, ABORTED }
    public enum Reason { READY, TRANSIENT, DOM_PENDING, DISABLED, CAPTURE_DISABLED, OPERATION_NOT_ALLOWED,
        DOCUMENT_CHANGED, CONFLICTING_REQUEST, MISSING_SIGNAL, UNCORRELATED_SIGNAL, HTTP_REFUSAL,
        REDIRECT_UNCONFIRMED, REQUEST_FAILED, INVALID_RETRY_AFTER, BUDGET, ATTEMPTS, BUSINESS_INVALID,
        SESSION_MISMATCH, AMBIGUOUS, PROBE_FAILURE, CAPTURE_FAILURE, INTERRUPTED }
    public String description() {
        return decision + "; " + reason + "; " + signal.map(UiRecoverySignal::description).orElse("backend signal not captured")
                + "; probe=" + probeTime.toMillis() + " ms; backoff=" + backoffTime.toMillis()
                + " ms; total=" + totalTime.toMillis() + " ms; observation only; no business action replay";
    }
}

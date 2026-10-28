package atf.ui.recovery;

import java.time.Instant;
import java.util.*;

/** Metadata-only receipt scoped before dispatch; signatures/bounded payloads are never treated as identity. */
public record UiRecoverySignal(Stamp stamp, long sequence, Instant capturedAt, Kind kind, int status, String retryAfter) {
    public enum Kind { PENDING, RESPONSE, REQUEST_FAILED }
    public record Stamp(UUID invocation, UUID operation, UUID frame, long document) {
        public Stamp { Objects.requireNonNull(invocation); Objects.requireNonNull(operation); Objects.requireNonNull(frame); }
    }
    public UiRecoverySignal {
        Objects.requireNonNull(stamp); Objects.requireNonNull(capturedAt); Objects.requireNonNull(kind);
        if (sequence < 1 || (kind == Kind.RESPONSE && (status < 100 || status > 599))
                || (kind != Kind.RESPONSE && status != 0)) throw new IllegalArgumentException("Invalid scoped recovery signal");
        retryAfter = retryAfter == null ? "" : retryAfter.trim();
    }
    public String description() { return kind == Kind.RESPONSE ? "HTTP " + status + "; sequence=" + sequence
            : kind.name() + "; response status not captured; sequence=" + sequence; }
}

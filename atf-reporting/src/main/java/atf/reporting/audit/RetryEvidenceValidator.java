package atf.reporting.audit;

import java.util.ArrayList;
import java.util.List;

/** Pure reconciliation rules for rendered retry metadata. */
public final class RetryEvidenceValidator {
    public List<String> validate(RetryEvidenceSnapshot snapshot) {
        if (snapshot == null) return List.of("retry snapshot is missing");
        List<String> violations = new ArrayList<>();
        if (snapshot.attempts() != snapshot.retryEvents() + 1) {
            violations.add("attempts must equal 1 + retry.events: attempts="
                    + snapshot.attempts() + ", retry.events=" + snapshot.retryEvents());
        }
        if (snapshot.history().size() != snapshot.attempts()) {
            violations.add("attempt history must contain every attempt: history="
                    + snapshot.history().size() + ", attempts=" + snapshot.attempts());
        }
        if (!isValidHttpStatus(snapshot.finalStatus())) {
            violations.add("final status must be a valid HTTP status: status=" + snapshot.finalStatus());
        }
        for (int index = 0; index < snapshot.history().size(); index++) {
            var attempt = snapshot.history().get(index);
            int expectedNumber = index + 1;
            if (attempt.number() != expectedNumber) {
                violations.add("attempt history must be sequential: expected="
                        + expectedNumber + ", actual=" + attempt.number());
            }
            if (!isValidHttpStatus(attempt.status())) {
                violations.add("attempt status must be a valid HTTP status: attempt="
                        + attempt.number() + ", status=" + attempt.status());
            }
            if (attempt.outcome().isBlank()) {
                violations.add("attempt outcome must be present: attempt=" + attempt.number());
            }
        }
        if (!snapshot.history().isEmpty()) {
            var last = snapshot.history().getLast();
            if (last.number() != snapshot.attempts()) {
                violations.add("last attempt number must equal attempts: last="
                        + last.number() + ", attempts=" + snapshot.attempts());
            }
            if (snapshot.finalStatus() != last.status()) {
                violations.add("last attempt status must equal response status: last="
                        + last.status() + ", response=" + snapshot.finalStatus());
            }
        }
        return List.copyOf(violations);
    }

    private boolean isValidHttpStatus(int status) {
        return status >= 100 && status <= 599;
    }
}

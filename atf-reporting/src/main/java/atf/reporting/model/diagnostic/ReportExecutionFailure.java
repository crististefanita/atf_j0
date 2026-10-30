package atf.reporting.model.diagnostic;

import atf.reporting.api.evidence.ReportTransportTiming;
import atf.reporting.model.ReportExecutionEvidence;
import java.util.Optional;

/** Captured transport cause; presence alone never turns an expected terminal outcome into FAIL. */
public record ReportExecutionFailure(String method, String path, String message, String duration) {
    public static Optional<ReportExecutionFailure> from(ReportExecutionEvidence execution) {
        if (execution == null || execution.request() == null) return Optional.empty();
        boolean unavailable = execution.response() == null || execution.response().status() < 0;
        if (!unavailable && execution.termination().isEmpty()) return Optional.empty();
        String message = execution.termination().map(value -> value.outcome() + ": " + value.diagnostic())
                .orElseGet(() -> execution.attemptHistory().stream().filter(attempt -> attempt.statusCode() < 0)
                        .reduce((first, last) -> last).map(attempt -> attempt.reason())
                        .filter(reason -> reason != null && !reason.isBlank()).orElse("HTTP response unavailable"));
        return Optional.of(new ReportExecutionFailure(execution.request().method(), execution.request().path(),
                message, ReportTransportTiming.display(execution.totalDuration())));
    }
    public String label() { return "Execution failure · " + method + " " + path; }
}

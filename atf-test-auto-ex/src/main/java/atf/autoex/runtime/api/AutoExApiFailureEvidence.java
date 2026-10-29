package atf.autoex.runtime.api;

import atf.core.http.execution.evidence.ApiExecutionFailure;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import java.util.Optional;

/** AutoEx's typed failure adapter; no JUnit lifecycle or renderer implementation is needed. */
public final class AutoExApiFailureEvidence {
    private AutoExApiFailureEvidence() { }
    public static Optional<String> publish(AtfReportPorts.Phase phase, Throwable failure, String diagnostic) {
        return ApiExecutionFailure.find(failure).map(snapshot -> {
            var request = snapshot.request();
            try (var node = phase.startRequest("Failed API execution", request.method(), request.path())) {
                String summary = ApiExchangeEvidence.publishFailure(node, ReportModelAdapters.partialExecution(snapshot),
                        snapshot.url(), snapshot.termination().name(), diagnostic);
                node.complete(AtfReportStatus.FAIL);
                return summary;
            }
        });
    }
}

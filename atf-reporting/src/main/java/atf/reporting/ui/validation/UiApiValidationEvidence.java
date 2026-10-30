package atf.reporting.ui.validation;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionEvidence;

import java.util.List;
import java.util.Objects;

/** Typed API verdicts attached to one UI journey checkpoint. */
public record UiApiValidationEvidence(List<AtfAssertion> assertions,
                                      List<Exchange> exchanges) {
    public UiApiValidationEvidence {
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
    }

    /** Explicit empty API channel for UI-only checkpoints. */
    public static UiApiValidationEvidence empty() {
        return new UiApiValidationEvidence(List.of(), List.of());
    }

    public boolean isEmpty() { return assertions.isEmpty() && exchanges.isEmpty(); }

    public AtfReportStatus status() {
        AtfReportStatus result = statusOf(assertions);
        for (Exchange exchange : exchanges) result = AtfReportStatus.worst(result, exchange.status());
        return result;
    }

    private static AtfReportStatus statusOf(List<AtfAssertion> assertions) {
        AtfReportStatus result = AtfReportStatus.INFO;
        for (AtfAssertion assertion : assertions) {
            if (assertion != null) result = AtfReportStatus.worst(result, assertion.resultStatus());
        }
        return result;
    }

    /**
     * Assertions belong to this exact exchange, not to sibling requests.
     * A missing response requires a caller verdict, either explicit or supplied by evaluated assertions.
     */
    public record Exchange(ReportExecutionEvidence evidence,
                           List<AtfAssertion> assertions,
                           AtfReportStatus status) {
        public Exchange {
            Objects.requireNonNull(evidence, "evidence");
            Objects.requireNonNull(evidence.request(), "captured request");
            assertions = assertions == null ? List.of() : List.copyOf(assertions);
            AtfReportStatus assertionStatus = statusOf(assertions);
            if (status == null || status == AtfReportStatus.INFO) {
                status = assertionStatus;
                if (status == AtfReportStatus.INFO) {
                    status = unassertedStatus(evidence);
                }
            } else {
                status = AtfReportStatus.worst(status, assertionStatus);
            }
        }

        public Exchange(ReportExecutionEvidence evidence, List<AtfAssertion> assertions) {
            this(evidence, assertions, null);
        }

        private static AtfReportStatus unassertedStatus(ReportExecutionEvidence evidence) {
            if (evidence.response() == null) {
                throw new IllegalArgumentException(
                        "An exchange without a captured response requires an explicit verdict or evaluated assertions");
            }
            return evidence.response().status() < 0 ? AtfReportStatus.WARNING : AtfReportStatus.PASS;
        }
    }
}

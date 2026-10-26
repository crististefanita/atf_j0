package atf.validation.publishing;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.reporting.inventory.ValidationSink;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ApiValidationException;
import atf.validation.evaluation.ValidationDecision;
import atf.validation.evaluation.ValidationEvaluator;

import java.util.List;
import java.util.Objects;

/**
 * Publishes one response contract as one validation sibling of its request.
 * The evaluator is independent of Extent; this class only crosses the
 * renderer-neutral reporting port.
 */
public final class ResponseContractReporter {
    private final ValidationEvaluator evaluator;

    public ResponseContractReporter() {
        this(new ValidationEvaluator());
    }

    public ResponseContractReporter(ValidationEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public ValidationDecision validate(ValidationSink request, String title,
                                       ApiResponse response, ResponseContract contract,
                                       boolean warningMode) {
        return validate(request, title, response, contract, warningMode, true);
    }

    /**
     * Renders the complete validation evidence while allowing a JUnit host to
     * keep the decision available to its normal assertions. Scenario runners
     * use the default strict overload; direct test adapters use
     * {@code throwOnFailure=false} so the test remains the owner of the
     * assertion policy.
     */
    public ValidationDecision validate(ValidationSink request, String title,
                                       ApiResponse response, ResponseContract contract,
                                       boolean warningMode, boolean throwOnFailure) {
        if (request == null || title == null || title.isBlank()) throw new IllegalArgumentException("request/title");
        var validation = request.startValidation(title);
        try {
            var decision = evaluator.evaluate(contract, response, warningMode);
            validation.assertions(decision.rows());
            validation.complete(decision.status());
            if (decision.shouldThrow() && throwOnFailure) {
                throw new ApiValidationException(decision.message());
            }
            return decision;
        } catch (RuntimeException failure) {
            try {
                validation.assertions(List.of(new AtfAssertion(
                        "Validation execution", "rule evaluation completes", failure.getMessage(), false,
                        AtfReportStatus.FAIL)));
                validation.complete(AtfReportStatus.FAIL);
            } catch (RuntimeException ignored) {
                // Preserve the functional failure; reporting is best effort at this boundary.
            }
            throw failure;
        } finally {
            validation.close();
        }
    }
}

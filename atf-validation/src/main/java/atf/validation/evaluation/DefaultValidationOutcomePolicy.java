package atf.validation.evaluation;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.Objects;

/** Default ATF outcome rules shared by evaluators and direct table users. */
final class DefaultValidationOutcomePolicy implements ValidationOutcomePolicy {
    static final DefaultValidationOutcomePolicy STRICT = new DefaultValidationOutcomePolicy(false);
    static final DefaultValidationOutcomePolicy WARNING = new DefaultValidationOutcomePolicy(true);

    private final boolean warningMode;

    private DefaultValidationOutcomePolicy(boolean warningMode) {
        this.warningMode = warningMode;
    }

    @Override
    public ValidationDecision decide(ValidationTable table) {
        Objects.requireNonNull(table, "table");
        if (table.rows().isEmpty()) {
            table.add("Validation assertions", "at least one assertion", "0 assertions registered", false);
        }
        table.publish();
        if (!table.hasFailures()) {
            return new ValidationDecision(table.rows(), AtfReportStatus.PASS,
                    table.rows().size() + " validations passed.", false, table);
        }
        if (!warningMode) {
            return new ValidationDecision(table.rows(), AtfReportStatus.FAIL,
                    "Validation failed.", true, table);
        }
        var warningRows = table.rows().stream()
                .map(row -> row.passed()
                        ? row
                        : new AtfAssertion(row.name(), row.expected(), row.actual(), false, AtfReportStatus.WARNING))
                .toList();
        return new ValidationDecision(warningRows, AtfReportStatus.WARNING,
                "Soft validation warning: one or more assertions failed.", false, table);
    }
}

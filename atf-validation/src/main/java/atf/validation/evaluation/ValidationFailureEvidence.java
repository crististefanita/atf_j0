package atf.validation.evaluation;
import atf.core.reporting.contract.AtfAssertion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Renderer-neutral explanation of a failed response contract.
 *
 * <p>The aggregate validation table remains the authoritative detailed
 * evidence. This projection is the compact phase-level explanation used by
 * lifecycle reports and failure indexes, so a reader can understand the
 * expected/actual mismatch without opening a stack trace.</p>
 */
public record ValidationFailureEvidence(
        String cause,
        String assertions,
        String expected,
        String actual) {

    public ValidationFailureEvidence {
        cause = requireText(cause, "cause");
        assertions = requireText(assertions, "assertions");
        expected = requireText(expected, "expected");
        actual = requireText(actual, "actual");
    }

    public static ValidationFailureEvidence from(List<AtfAssertion> rows) {
        Objects.requireNonNull(rows, "rows");
        List<AtfAssertion> failures = rows.stream()
                .filter(row -> !row.passed())
                .toList();
        if (failures.isEmpty()) {
            throw new IllegalArgumentException("at least one failed assertion is required");
        }
        String names = failures.stream()
                .map(AtfAssertion::name)
                .collect(Collectors.joining("; "));
        return new ValidationFailureEvidence(
                "Contract assertion failed: " + names,
                names,
                format(failures, AtfAssertion::expected),
                format(failures, AtfAssertion::actual));
    }

    /** Details are deliberately ordered for a stable, compact HTML table. */
    public Map<String, String> details() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Assertion", assertions);
        details.put("Expected", expected);
        details.put("Actual", actual);
        return java.util.Collections.unmodifiableMap(details);
    }

    private static String format(List<AtfAssertion> failures,
                                 java.util.function.Function<AtfAssertion, String> value) {
        if (failures.size() == 1) return value.apply(failures.getFirst());
        return failures.stream()
                .map(row -> row.name() + " = " + value.apply(row))
                .collect(Collectors.joining("; "));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}

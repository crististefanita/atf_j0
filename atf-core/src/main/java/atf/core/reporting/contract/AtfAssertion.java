package atf.core.reporting.contract;

import java.util.Objects;

/** One row in the single aggregate assertion table for a response. */
public record AtfAssertion(
        String name,
        String expected,
        String actual,
        boolean passed,
        AtfReportStatus resultStatus) {

    public AtfAssertion {
        name = requireText(name, "name");
        expected = Objects.requireNonNull(expected, "expected");
        actual = Objects.requireNonNull(actual, "actual");
        resultStatus = resultStatus == null ? (passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL) : resultStatus;
    }

    public AtfAssertion(String name, String expected, String actual, boolean passed) {
        this(name, expected, actual, passed, null);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}

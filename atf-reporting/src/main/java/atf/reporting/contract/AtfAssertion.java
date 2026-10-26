package atf.reporting.contract;

import java.util.Objects;

/** One immutable assertion row in a report. */
public record AtfAssertion(String name, String expected, String actual,
                           boolean passed, AtfReportStatus resultStatus) {
    public AtfAssertion {
        name = required(name, "name");
        expected = Objects.requireNonNull(expected, "expected");
        actual = Objects.requireNonNull(actual, "actual");
        resultStatus = resultStatus == null
                ? (passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL)
                : resultStatus;
    }

    public AtfAssertion(String name, String expected, String actual, boolean passed) {
        this(name, expected, actual, passed, null);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}

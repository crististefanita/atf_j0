package atf.reporting.model;

import atf.reporting.contract.AtfReportStatus;

import java.util.Objects;

/** Immutable setup-check row before it is serialized into an Extent table. */
public record ReportCheck(String check, String expected, String actual, AtfReportStatus status) {
    public ReportCheck {
        check = requireText(check, "check");
        expected = Objects.requireNonNullElse(expected, "");
        actual = Objects.requireNonNullElse(actual, "");
        status = Objects.requireNonNull(status, "status");
    }

    private static String requireText(String value, String field) {
        if (Objects.requireNonNull(value, field).isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}

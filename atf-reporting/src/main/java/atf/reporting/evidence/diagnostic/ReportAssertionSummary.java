package atf.reporting.evidence.diagnostic;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/** Safe diagnostic display only; the caller keeps the full rows and the business decision. */
public final class ReportAssertionSummary {
    private ReportAssertionSummary() { }

    public static String failures(Collection<AtfAssertion> assertions) {
        Objects.requireNonNull(assertions, "assertions");
        var redactor = new SensitiveDataRedactor();
        return safe(assertions.stream().filter(row -> !row.passed() || row.resultStatus() == AtfReportStatus.FAIL)
                .map(row -> redactor.redactText(row.name()) + " [expected=" + redactor.redactField(row.name(), row.expected())
                        + "; actual=" + redactor.redactField(row.name(), row.actual()) + "]")
                .collect(Collectors.joining("; ")));
    }

    public static String expected(Collection<AtfAssertion> assertions) {
        Objects.requireNonNull(assertions, "assertions");
        var redactor = new SensitiveDataRedactor();
        return safe(assertions.stream().map(row -> redactor.redactText(row.name()) + ": " + redactor.redactField(row.name(), row.expected()))
                .collect(Collectors.joining("; ")));
    }

    private static String safe(String value) {
        String safe = new SensitiveDataRedactor().redactText(value);
        int limit = ReportingSettings.DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS;
        return safe.length() <= limit ? safe : safe.substring(0, limit - 1) + "…";
    }
}

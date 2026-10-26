package atf.reporting.audit;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared assertion and value-normalization primitives for report audits. */
public final class ReportAuditChecks {
    private static final Pattern COUNT = Pattern.compile("(?<!\\d)(\\d+)(?!\\d)");
    private static final Pattern DURATION = Pattern.compile(
            "^\\s*(\\d+(?:[.,]\\d+)?)\\s*(ms|milliseconds?|s|seconds?)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private ReportAuditChecks() { }

    public static AtfAssertion check(String name, String expected, String actual, boolean passed) {
        return new AtfAssertion(name, expected, actual, passed);
    }

    public static AtfReportStatus status(boolean passed) {
        return passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
    }

    /** Parses one displayed count; a missing or malformed count is zero. */
    public static int count(String value) {
        return parseCount(value).orElse(0);
    }

    public static OptionalInt parseCount(String value) {
        if (value == null) return OptionalInt.empty();
        Matcher matcher = COUNT.matcher(value);
        return matcher.find() ? OptionalInt.of(Integer.parseInt(matcher.group(1))) : OptionalInt.empty();
    }

    /** Parses report duration text into milliseconds without conflating it with counts. */
    public static OptionalDouble parseDurationMs(String value) {
        if (value == null || value.isBlank() || value.trim().equals("—")) return OptionalDouble.empty();
        Matcher matcher = DURATION.matcher(value);
        if (!matcher.matches()) return OptionalDouble.empty();
        double amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
        String unit = matcher.group(2);
        if (unit != null && (unit.equalsIgnoreCase("s") || unit.toLowerCase().startsWith("second"))) {
            amount *= 1000d;
        }
        return OptionalDouble.of(amount);
    }

    public static int number(String value) {
        return count(value);
    }

    public static int count(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    /** @deprecated use {@link #count(Object)}; retained for existing audit tests. */
    @Deprecated
    public static int number(Object value) {
        return count(value);
    }
}

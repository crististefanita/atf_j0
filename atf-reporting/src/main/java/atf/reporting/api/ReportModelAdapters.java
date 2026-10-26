package atf.reporting.api;

import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional compatibility seam for consumers migrating from the former core DTOs.
 *
 * <p>The typed reporting records are the primary API. Reflection is retained
 * only for legacy consumers that cannot depend on {@code atf-core}; every
 * required value is read through this boundary and malformed input fails with
 * the source type and field name instead of becoming a silent default.</p>
 */
public final class ReportModelAdapters {
    private static final String NONE_AUTH_PROFILE = "none";

    private ReportModelAdapters() {
    }

    public static ReportRequest request(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof ReportRequest request) {
            return request;
        }
        String method = requiredText(source, "method");
        String path = requiredText(source, "path");
        return new ReportRequest(
                method,
                path,
                stringMap(source, "query"),
                stringMap(source, "headers"),
                optionalText(source, "authProfile", NONE_AUTH_PROFILE),
                optionalText(source, "bodyText", null));
    }

    public static ReportTransportProvenance provenance(Object source) {
        if (source == null) {
            return ReportTransportProvenance.UNKNOWN;
        }
        if (source instanceof ReportTransportProvenance value) {
            return value;
        }
        String name = source instanceof Enum<?> value
                ? value.name()
                : String.valueOf(source).trim();
        for (ReportTransportProvenance value : ReportTransportProvenance.values()) {
            if (value.name().equals(name) || value.reportLabel().equals(name)) {
                return value;
            }
        }
        throw failure(source, "provenance", "unsupported value '" + name + "'", null);
    }

    public static ReportExecutionEvidence execution(Object source) {
        if (source == null) {
            return null;
        }
        if (source instanceof ReportExecutionEvidence evidence) {
            return evidence;
        }
        Object response = requiredObject(source, "response");
        ReportResponse reportResponse = new ReportResponse(
                requiredInt(response, "status"),
                optionalText(response, "body", null),
                stringMap(response, "headers"),
                optionalDuration(response, "duration"),
                optionalText(response, "url", ""));

        return new ReportExecutionEvidence(
                reportResponse,
                requiredInt(source, "attempts"),
                optionalDuration(source, "totalDuration"),
                retryEvents(optionalValue(source, "retryEvents")),
                attemptHistory(optionalValue(source, "attemptHistory")),
                request(optionalValue(source, "request")),
                provenance(optionalValue(source, "provenance")));
    }

    /**
     * Adapts one legacy/core assertion without adding a compile-time dependency
     * from the standalone reporting library to the producer module.
     */
    public static AtfAssertion assertion(Object source) {
        if (source == null) return null;
        if (source instanceof AtfAssertion assertion) return assertion;
        String name = requiredText(source, "name");
        String expected = requiredString(source, "expected");
        String actual = requiredString(source, "actual");
        boolean passed = requiredBoolean(source, "passed");
        Object result = optionalValue(source, "resultStatus");
        return new AtfAssertion(name, expected, actual, passed,
                result == null ? null : reportStatus(source, "resultStatus", result));
    }

    /** Adapts one legacy/core UI action record into the renderer-neutral model. */
    public static AtfUiActionRecord uiAction(Object source) {
        if (source == null) return null;
        if (source instanceof AtfUiActionRecord action) return action;
        Object rawStatus = requiredObject(source, "status");
        Object rawSource = optionalValue(source, "source");
        return new AtfUiActionRecord(
                requiredInt(source, "sequence"),
                requiredText(source, "operation"),
                requiredText(source, "target"),
                optionalText(source, "expected", ""),
                optionalText(source, "observed", ""),
                requiredLong(source, "durationMillis"),
                reportStatus(source, "status", rawStatus),
                rawSource == null ? AtfUiEvidenceSource.ACTION
                        : evidenceSource(source, "source", rawSource));
    }

    /** Adapts a collection of legacy/core action rows while preserving order. */
    public static List<AtfUiActionRecord> uiActions(Iterable<?> source) {
        if (source == null) return List.of();
        List<AtfUiActionRecord> actions = new ArrayList<>();
        for (Object action : source) {
            if (action == null) {
                throw failure(source, "uiActions", "contains a null action", null);
            }
            actions.add(uiAction(action));
        }
        return List.copyOf(actions);
    }

    /** Maps the matching core/report status vocabulary, failing closed on drift. */
    public static AtfReportStatus status(Object source) {
        if (source == null) return AtfReportStatus.INFO;
        if (source instanceof AtfReportStatus status) return status;
        return reportStatus(source, "status", source);
    }

    private static List<ReportRetryEvent> retryEvents(Object value) {
        if (value == null) {
            return List.of();
        }
        Iterable<?> values = iterable(value, "retryEvents");
        List<ReportRetryEvent> result = new ArrayList<>();
        for (Object event : values) {
            if (event == null) {
                throw failure(value, "retryEvents", "contains a null event", null);
            }
            result.add(new ReportRetryEvent(
                    requiredInt(event, "attempt"),
                    requiredInt(event, "statusCode"),
                    optionalText(event, "header", ""),
                    optionalDuration(event, "delay"),
                    optionalText(event, "reason", "")));
        }
        return List.copyOf(result);
    }

    private static List<ReportAttemptRecord> attemptHistory(Object value) {
        if (value == null) {
            return List.of();
        }
        Iterable<?> values = iterable(value, "attemptHistory");
        List<ReportAttemptRecord> result = new ArrayList<>();
        for (Object attempt : values) {
            if (attempt == null) {
                throw failure(value, "attemptHistory", "contains a null attempt", null);
            }
            result.add(new ReportAttemptRecord(
                    requiredInt(attempt, "number"),
                    requiredInt(attempt, "statusCode"),
                    optionalDuration(attempt, "duration"),
                    optionalText(attempt, "outcome", ""),
                    optionalText(attempt, "reason", "")));
        }
        return List.copyOf(result);
    }

    private static Iterable<?> iterable(Object source, String field) {
        if (source instanceof Iterable<?> values) {
            return values;
        }
        throw failure(source, field, "expected Iterable but received "
                + source.getClass().getName(), null);
    }

    private static Map<String, String> stringMap(Object source, String field) {
        Object value = optionalValue(source, field);
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> values)) {
            throw failure(source, field, "expected Map but received "
                    + value.getClass().getName(), null);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw failure(source, field, "contains a non-text or blank key", null);
            }
            if (!(entry.getValue() instanceof String item)) {
                throw failure(source, field, "contains a non-text value for '" + key + "'", null);
            }
            result.put(key, item);
        }
        return Map.copyOf(result);
    }

    private static int requiredInt(Object source, String field) {
        Object value = requiredObject(source, field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException error) {
                throw failure(source, field, "is not an integer: " + text, error);
            }
        }
        throw failure(source, field, "expected Number or numeric String but received "
                + value.getClass().getName(), null);
    }

    private static long requiredLong(Object source, String field) {
        Object value = requiredObject(source, field);
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException error) {
                throw failure(source, field, "is not an integer: " + text, error);
            }
        }
        throw failure(source, field, "expected Number or numeric String but received "
                + value.getClass().getName(), null);
    }

    private static boolean requiredBoolean(Object source, String field) {
        Object value = requiredObject(source, field);
        if (value instanceof Boolean bool) return bool;
        throw failure(source, field, "expected Boolean but received " + value.getClass().getName(), null);
    }

    private static String requiredString(Object source, String field) {
        Object value = requiredObject(source, field);
        if (value instanceof String text) return text;
        throw failure(source, field, "expected String but received " + value.getClass().getName(), null);
    }

    private static AtfReportStatus reportStatus(Object owner, String field, Object value) {
        String name = enumName(value);
        try {
            return AtfReportStatus.valueOf(name);
        } catch (IllegalArgumentException error) {
            throw failure(owner, field, "unsupported status '" + name + "'", error);
        }
    }

    private static AtfUiEvidenceSource evidenceSource(Object owner, String field, Object value) {
        String name = enumName(value);
        try {
            return AtfUiEvidenceSource.valueOf(name);
        } catch (IllegalArgumentException error) {
            throw failure(owner, field, "unsupported UI evidence source '" + name + "'", error);
        }
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> enumValue
                ? enumValue.name() : String.valueOf(value).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static Duration optionalDuration(Object source, String field) {
        Object value = optionalValue(source, field);
        if (value == null) {
            return null;
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof String text) {
            try {
                return Duration.parse(text.trim());
            } catch (RuntimeException error) {
                throw failure(source, field, "is not an ISO-8601 duration: " + text, error);
            }
        }
        throw failure(source, field, "expected Duration but received "
                + value.getClass().getName(), null);
    }

    private static String requiredText(Object source, String field) {
        Object value = requiredObject(source, field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw failure(source, field, "must be a non-blank String", null);
        }
        return text;
    }

    private static String optionalText(Object source, String field, String fallback) {
        Object value = optionalValue(source, field);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof String text)) {
            if (value instanceof Enum<?> enumValue) {
                return enumValue.name();
            }
            throw failure(source, field, "expected String but received "
                    + value.getClass().getName(), null);
        }
        return text;
    }

    private static Object requiredObject(Object source, String field) {
        Object value = optionalValue(source, field);
        if (value == null) {
            throw failure(source, field, "is required but was missing or null", null);
        }
        return value;
    }

    private static Object optionalValue(Object source, String field) {
        if (source == null) {
            return null;
        }
        Method method;
        try {
            method = source.getClass().getMethod(field);
        } catch (NoSuchMethodException missing) {
            return null;
        } catch (SecurityException error) {
            throw failure(source, field, "cannot be inspected", error);
        }
        try {
            if (!method.canAccess(source)) {
                method.trySetAccessible();
            }
            return method.invoke(source);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException error) {
            Throwable cause = error instanceof InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : error;
            throw failure(source, field, "could not be read", cause);
        }
    }

    private static ReportModelAdapterException failure(Object source, String field,
                                                        String detail, Throwable cause) {
        String type = source == null ? "<null>" : source.getClass().getName();
        return new ReportModelAdapterException(
                "Cannot adapt " + type + "." + field + ": " + detail,
                type, field, cause);
    }
}

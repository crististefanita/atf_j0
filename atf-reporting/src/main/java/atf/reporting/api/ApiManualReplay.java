package atf.reporting.api;

import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.security.SensitiveDataRedactor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Builds truthful, safe manual replay evidence for one API exchange. */
public final class ApiManualReplay {
    private static final SensitiveDataRedactor REDACTOR = new SensitiveDataRedactor();

    private ApiManualReplay() { }

    public static String describe(ReportTransportProvenance provenance, ReportRequest request,
                                  String url, String requestBody) {
        Objects.requireNonNull(provenance, "provenance");
        if (!provenance.networkExecuted()) {
            return provenance == ReportTransportProvenance.ACCEPTANCE_MOCK
                    ? "NOT EXECUTABLE — ACCEPTANCE / MOCK; network was not executed. "
                    + "The URL is route metadata only; configure an explicit live base URI before replay."
                    : "NOT VERIFIED — transport provenance is unavailable. "
                    + "Declare acceptance/mock or explicit live transport before replay.";
        }
        if (request == null || url == null || url.isBlank()) {
            return "NOT REPLAYABLE — live exchange lacks the request URL or method.";
        }
        if (request.bodyAvailability() == ReportBodyAvailability.NOT_CAPTURED) {
            return "NOT REPLAYABLE — request body was not captured; the exact exchange cannot be reproduced.";
        }
        return curl(request, url, requestBody);
    }

    /** Migration overload: accepts a legacy core DTO without importing core. */
    public static String describe(Object provenance, Object request, String url, String requestBody) {
        return describe(ReportModelAdapters.provenance(provenance), ReportModelAdapters.request(request), url, requestBody);
    }

    /** Returns the effective non-secret headers a manual reader needs to reproduce the call. */
    public static String redactedHeaders(ReportRequest request) {
        if (request == null) return "[]";
        return effectiveHeaders(request).entrySet().stream()
                .map(ApiManualReplay::redactedHeader)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public static String redactedHeaders(Object request) { return redactedHeaders(ReportModelAdapters.request(request)); }

    /** Returns response headers without exposing credential-bearing values. */
    public static String redactedHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return "[]";
        return headers.entrySet().stream()
                .map(ApiManualReplay::redactedHeader)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /** Returns only stable, semantically useful response headers for a compact projection. */
    public static String relevantResponseHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) return "[]";
        return headers.entrySet().stream()
                .filter(entry -> isRelevantResponseHeader(entry.getKey()))
                .map(ApiManualReplay::redactedHeader)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String redactedHeader(Map.Entry<String, String> entry) {
        return entry.getKey() + "="
                + REDACTOR.redactField(entry.getKey(), entry.getValue());
    }

    private static String curl(ReportRequest request, String url, String requestBody) {
        StringBuilder command = new StringBuilder("curl --request ")
                .append(shellQuote(request.method()))
                .append(" --url ").append(shellQuote(REDACTOR.redactText(url)));
        effectiveHeaders(request).forEach((name, value) -> command.append(" --header ")
                .append(shellQuote(name + ": " + REDACTOR.redactField(name, value))));
        if (requestBody != null && request.bodyAvailability() == ReportBodyAvailability.AVAILABLE) {
            command.append(" --data-raw ")
                    .append(shellQuote(REDACTOR.redactJson(requestBody)));
        }
        if (!"none".equalsIgnoreCase(request.authProfile())) {
            command.append(" ; # auth profile '").append(request.authProfile())
                    .append("' is configured separately; credentials are redacted");
        }
        return command.toString();
    }

    private static Map<String, String> effectiveHeaders(ReportRequest request) {
        var headers = new LinkedHashMap<String, String>();
        request.headerEvidence().values().entrySet().stream()
                .filter(entry -> isReplayRelevantHeader(entry.getKey()))
                .forEach(entry -> putHeader(headers, entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(headers);
    }

    private static void putHeader(Map<String, String> headers, String name, String value) {
        String canonicalName = name.equalsIgnoreCase(ReportHttpHeaders.ACCEPT)
                ? ReportHttpHeaders.ACCEPT
                : name.equalsIgnoreCase(ReportHttpHeaders.CONTENT_TYPE)
                ? ReportHttpHeaders.CONTENT_TYPE
                : name;
        headers.put(canonicalName, value);
    }

    /**
     * Browser transport headers do not help an API reader reproduce the
     * exchange. Keep semantic/custom headers and authentication signals, but
     * omit client fingerprint, navigation and connection noise.
     */
    private static boolean isReplayRelevantHeader(String name) {
        if (name == null || name.isBlank()) return false;
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        return !normalized.startsWith("sec-")
                && !java.util.Set.of("user-agent", "referer", "origin", "host",
                "content-length", "accept-encoding", "connection").contains(normalized);
    }

    private static boolean isRelevantResponseHeader(String name) {
        if (name == null || name.isBlank()) return false;
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("access-control-")) return false;
        return !java.util.Set.of("content-type", "content-length", "date", "cache-control",
                "content-encoding", "connection", "transfer-encoding", "vary", "server",
                "via", "age", "expires", "strict-transport-security",
                "x-content-type-options", "x-frame-options", "referrer-policy",
                "permissions-policy", "set-cookie").contains(normalized);
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}

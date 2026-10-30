package atf.reporting.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import atf.reporting.api.evidence.ReportBodyAvailability;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Canonical identity for one logical request. It is deliberately owned by
 * reporting so every consumer projects the same signature without duplicating
 * hashing, query redaction or JSON ordering rules.
 */
public final class ReportRequestSignature {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final java.util.regex.Pattern QUERY_FINGERPRINT =
            java.util.regex.Pattern.compile("#query=([0-9a-f]{64})(?=\\s*#body=|$)");
    private static final String ABSENT_BODY = "<absent>";
    private static final String UNKNOWN_BODY = "<not-captured>";
    private static final String BODY_FRAGMENT = "#body=";
    private static final String BODY_ABSENT = "absent";
    private static final String BODY_PRESENT = "present";
    private static final String INTERNAL_DIGEST = " (internal digest hidden)";

    private ReportRequestSignature() {
    }

    public static String forRequest(ReportRequest request) {
        if (request == null) return "";
        return request.method().trim().toUpperCase(Locale.ROOT) + " "
                + safeTarget(request.path(), request.queryParameters())
                + (request.queryFingerprint().isEmpty() ? "" : (request.queryParameters().isEmpty() ? "?query-not-captured" : "")
                        + "#query=" + request.queryFingerprint())
                + BODY_FRAGMENT + requestBodyDigest(request);
    }

    public static String requestBodyDigest(ReportRequest request) {
        return request.bodyAvailability() == ReportBodyAvailability.NOT_CAPTURED
                ? sha256(UNKNOWN_BODY) : bodyDigest(request.bodyText());
    }

    public static String responseBodyDigest(ReportResponse response) {
        return response.bodyAvailability() == ReportBodyAvailability.NOT_CAPTURED
                ? sha256(UNKNOWN_BODY) : bodyDigest(response.body());
    }

    public static String bodyDigest(String body) {
        return sha256(canonicalBody(body));
    }

    /**
     * Returns the bounded signature suitable for report HTML. The digest is
     * still used internally for deterministic grouping, but the raw SHA-256
     * value is deliberately not part of the human-facing artifact.
     */
    public static String displaySignature(String signature) {
        if (signature == null || signature.isBlank()) return "";
        int marker = signature.indexOf(BODY_FRAGMENT);
        if (marker < 0) {
            int fingerprint = signature.indexOf("#query=");
            return fingerprint < 0 ? signature : signature.substring(0, fingerprint);
        }
        String target = signature.substring(0, marker);
        int queryFingerprint = target.indexOf("#query=");
        if (queryFingerprint >= 0) target = target.substring(0, queryFingerprint);
        return target + BODY_FRAGMENT
                + bodyState(signature.substring(marker + BODY_FRAGMENT.length()));
    }

    /** Delimited interoperability seam; returns only an opaque digest, never an external signature or URL. */
    public static String queryFingerprint(String signature) {
        if (signature == null) return "";
        var match = QUERY_FINGERPRINT.matcher(signature);
        return match.find() ? match.group(1) : "";
    }

    /** Returns a semantic body state while keeping the internal digest private. */
    public static String displayDigest(String digest) {
        if (digest == null || digest.isBlank()) return "";
        return bodyState(digest) + INTERNAL_DIGEST;
    }

    private static String bodyState(String digest) {
        if (sha256(UNKNOWN_BODY).equals(digest)) return "not-captured";
        return bodyDigest(null).equals(digest) ? BODY_ABSENT : BODY_PRESENT;
    }

    public static String correlationKey(ReportRequest request) {
        if (request == null) return "";
        return request.method().trim().toUpperCase(Locale.ROOT) + " "
                + normalizePath(request.path());
    }

    public static String canonicalBody(String body) {
        if (body == null || body.isEmpty()) return ABSENT_BODY;
        if (body.isBlank()) return body;
        try {
            JsonNode node = JSON.readTree(body);
            return node == null ? body : canonicalJson(node);
        } catch (Exception ignored) {
            // Non-JSON bodies remain distinct and deterministic. JSON
            // contracts are validated separately by ReportJsonBodyPolicy.
            return body;
        }
    }

    private static String canonicalJson(JsonNode node) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(Comparator.naturalOrder());
            StringJoiner result = new StringJoiner(",", "{", "}");
            for (String name : names) {
                result.add(quote(name) + ":" + canonicalJson(node.get(name)));
            }
            return result.toString();
        }
        if (node.isArray()) {
            StringJoiner result = new StringJoiner(",", "[", "]");
            node.elements().forEachRemaining(value -> result.add(canonicalJson(value)));
            return result.toString();
        }
        return node.toString();
    }

    private static String safeTarget(String path, ReportQueryParameters query) {
        String base = normalizePath(path);
        if (query == null || query.isEmpty()) return base;
        StringJoiner values = new StringJoiner("&");
        query.values().stream()
                .sorted(Comparator.comparing(ReportQueryParameter::name))
                .forEach(entry -> values.add(encode(entry.name()) + "="
                        + encode(isSensitive(entry.name()) ? "***REDACTED***" : entry.value())));
        return base + "?" + values;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        int fragment = path.indexOf('#');
        int end = fragment < 0 ? path.length() : fragment;
        String value = path.substring(0, end);
        int query = value.indexOf('?');
        return query < 0 ? value : value.substring(0, query);
    }

    private static boolean isSensitive(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return normalized.contains("token") || normalized.contains("secret")
                || normalized.contains("password") || normalized.contains("credential")
                || normalized.equals("auth") || normalized.equals("authorization")
                || normalized.equals("cookie");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        return JSON.valueToTree(value).toString();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}

package atf.ui.network;

import atf.core.security.SensitiveDataRedactor;
import atf.ui.config.UiNetworkInspectionConfiguration;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import atf.ui.network.identity.UiRequestIdentity;

/** One redacted, bounded browser exchange; safe for a renderer-neutral adapter. */
public record UiNetworkObservation(
        long sequence,
        Instant capturedAt,
        String method,
        String url,
        String path,
        int status,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders,
        String requestBody,
        String responseBody,
        String authMethod,
        String authSource,
        String service,
        String evidenceSource,
        String requestSignature,
        boolean responseBodyAvailable,
        String failureReason,
        UiNetworkCapturePolicy.ResourceKind resourceKind,
        Optional<UiRequestIdentity> requestIdentity) {
    private static final SensitiveDataRedactor REDACTOR = new SensitiveDataRedactor();

    public UiNetworkObservation {
        capturedAt = capturedAt == null ? Instant.EPOCH : capturedAt;
        method = normalize(method).toUpperCase(Locale.ROOT);
        url = normalize(url);
        path = normalize(path);
        requestHeaders = Map.copyOf(requestHeaders == null ? Map.of() : requestHeaders);
        responseHeaders = Map.copyOf(responseHeaders == null ? Map.of() : responseHeaders);
        requestBody = normalizeBody(requestBody);
        responseBody = normalizeBody(responseBody);
        authMethod = normalize(authMethod);
        authSource = normalize(authSource);
        service = normalize(service);
        evidenceSource = normalize(evidenceSource);
        requestSignature = normalize(requestSignature);
        failureReason = normalize(failureReason);
        resourceKind = resourceKind == null ? UiNetworkCapturePolicy.ResourceKind.OTHER : resourceKind;
        requestIdentity = requestIdentity == null ? Optional.empty() : requestIdentity;
    }

    /** Legacy observations lack start evidence; receipt order never fabricates it. */
    public UiNetworkObservation(long sequence, Instant capturedAt, String method, String url,
                                String path, int status, Map<String, String> requestHeaders,
                                Map<String, String> responseHeaders, String requestBody,
                                String responseBody, String authMethod, String authSource,
                                String service, String evidenceSource, String requestSignature,
                                boolean responseBodyAvailable, String failureReason, UiNetworkCapturePolicy.ResourceKind resourceKind) {
        this(sequence, capturedAt, method, url, path, status, requestHeaders, responseHeaders, requestBody,
                responseBody, authMethod, authSource, service, evidenceSource, requestSignature,
                responseBodyAvailable, failureReason, resourceKind, Optional.empty());
    }

    public UiNetworkObservation withRequestIdentity(UiRequestIdentity identity) {
        return new UiNetworkObservation(sequence, capturedAt, method, url, path, status, requestHeaders,
                responseHeaders, requestBody, responseBody, authMethod, authSource, service, evidenceSource,
                requestSignature, responseBodyAvailable, failureReason, resourceKind, Optional.ofNullable(identity));
    }

    /** Compatibility constructor; browser observations always carry a known resource type. */
    public UiNetworkObservation(long sequence, Instant capturedAt, String method, String url,
                                String path, int status, Map<String, String> requestHeaders,
                                Map<String, String> responseHeaders, String requestBody,
                                String responseBody, String authMethod, String authSource,
                                String service, String evidenceSource, String requestSignature,
                                boolean responseBodyAvailable, String failureReason) {
        this(sequence, capturedAt, method, url, path, status, requestHeaders, responseHeaders,
                requestBody, responseBody, authMethod, authSource, service, evidenceSource,
                requestSignature, responseBodyAvailable, failureReason, UiNetworkCapturePolicy.ResourceKind.OTHER);
    }

    /** Compatibility constructor for response observations created before request-failure evidence. */
    public UiNetworkObservation(long sequence, Instant capturedAt, String method, String url,
                                String path, int status, Map<String, String> requestHeaders,
                                Map<String, String> responseHeaders, String requestBody,
                                String responseBody, String authMethod, String authSource,
                                String service, String evidenceSource, String requestSignature,
                                boolean responseBodyAvailable) {
        this(sequence, capturedAt, method, url, path, status, requestHeaders, responseHeaders,
                requestBody, responseBody, authMethod, authSource, service, evidenceSource,
                requestSignature, responseBodyAvailable, "");
    }

    static UiNetworkObservation from(Response response, long sequence,
                                     UiNetworkInspectionConfiguration configuration) {
        return from(response, sequence, configuration, true);
    }

    /** Captures attachment response metadata without reading the downloaded bytes as a body preview. */
    static UiNetworkObservation fromAttachmentResponse(Response response, long sequence,
                                                       UiNetworkInspectionConfiguration configuration) {
        return from(response, sequence, configuration, false);
    }

    private static UiNetworkObservation from(Response response, long sequence,
                                             UiNetworkInspectionConfiguration configuration,
                                             boolean allowResponseBody) {
        Request request = response.request();
        String url = request.url();
        UiNetworkCapturePolicy.ResourceKind resourceKind =
                UiNetworkCapturePolicy.ResourceKind.fromPlaywright(request.resourceType());
        String requestBody = bounded(redactJson(request.postData()),
                configuration.maxBodyPreviewCharacters());
        String responseBody;
        boolean bodyAvailable = allowResponseBody
                && configuration.capturePolicy().capturesResponseBody(resourceKind);
        try {
            if (bodyAvailable) {
                response.finished();
                responseBody = bounded(redactJson(new String(response.body(), StandardCharsets.UTF_8)),
                        configuration.maxBodyPreviewCharacters());
            } else {
                responseBody = "";
            }
        } catch (RuntimeException unavailable) {
            bodyAvailable = false;
            responseBody = "<unavailable: " + unavailable.getClass().getSimpleName() + ">";
        }
        Map<String, String> browserHeaders = requestHeaders(request);
        Map<String, String> requestHeaders = redact(browserHeaders);
        Map<String, String> responseHeaders = redact(response.headers());
        String[] auth = authentication(browserHeaders, url);
        return new UiNetworkObservation(sequence, Instant.now(), request.method(), safeUrl(url),
                path(url), response.status(), requestHeaders, responseHeaders, requestBody,
                responseBody, auth[0], auth[1], host(url), "UI_BROWSER",
                signature(request.method(), url, requestBody), bodyAvailable, "",
                resourceKind);
    }

    static UiNetworkObservation fromFailure(Request request, long sequence,
                                            UiNetworkInspectionConfiguration configuration) {
        String url = request.url();
        String requestBody = bounded(redactJson(request.postData()),
                configuration.maxBodyPreviewCharacters());
        String failure = request.failure();
        String reason = bounded(failure == null ? "" : REDACTOR.redactText(
                failure.replaceAll("https?://[^\\s]+", "<url-redacted>")),
                configuration.maxBodyPreviewCharacters());
        if (reason.isBlank()) reason = "request failed before response";
        Map<String, String> browserHeaders = requestHeaders(request);
        Map<String, String> requestHeaders = redact(browserHeaders);
        String[] auth = authentication(browserHeaders, url);
        return new UiNetworkObservation(sequence, Instant.now(), request.method(), safeUrl(url),
                path(url), -1, requestHeaders, Map.of(), requestBody,
                "<unavailable: request failed before response>", auth[0], auth[1], host(url),
                "UI_BROWSER_REQUEST_FAILED", signature(request.method(), url, requestBody),
                false, reason, UiNetworkCapturePolicy.ResourceKind.fromPlaywright(request.resourceType()));
    }

    /** Returns a copy with a consumer-owned service label; the generic inspector stays host-based. */
    public UiNetworkObservation withService(String consumerService) {
        return new UiNetworkObservation(sequence, capturedAt, method, url, path, status,
                requestHeaders, responseHeaders, requestBody, responseBody, authMethod,
                authSource, consumerService, evidenceSource, requestSignature,
                responseBodyAvailable, failureReason, resourceKind, requestIdentity);
    }

    public boolean requestFailed() {
        return status < 0 && !failureReason.isBlank();
    }

    public String displayTarget() {
        return method + " " + path + " → " + (status < 0 ? "UNAVAILABLE" : status);
    }

    /**
     * Rebuilds a caller-provided observation at the browser-boundary policy.
     *
     * <p>The Playwright factories already produce safe values, but the public
     * typed capture seam must not trust arbitrary fixture or consumer objects.
     * Recomputing the target, body preview and signature here means that a
     * subscriber sees exactly the same bounded/redacted object that is stored
     * by the inspector.</p>
     */
    UiNetworkObservation sanitizedFor(UiNetworkInspectionConfiguration configuration) {
        String safeUrl = safeUrl(url);
        String safeRequestBody = bounded(safeRedactJson(requestBody),
                configuration.maxBodyPreviewCharacters());
        String safeResponseBody = bounded(safeRedactJson(responseBody),
                configuration.maxBodyPreviewCharacters());
        String safeService = bounded(REDACTOR.redactText(
                        service.isBlank() ? host(safeUrl) : service),
                configuration.maxBodyPreviewCharacters());
        String safeEvidenceSource = bounded(REDACTOR.redactText(evidenceSource),
                configuration.maxBodyPreviewCharacters());
        String safeAuthMethod = bounded(REDACTOR.redactText(authMethod),
                configuration.maxBodyPreviewCharacters());
        String safeAuthSource = bounded(REDACTOR.redactText(authSource),
                configuration.maxBodyPreviewCharacters());
        String safeFailureReason = bounded(REDACTOR.redactText(failureReason),
                configuration.maxBodyPreviewCharacters());
        return new UiNetworkObservation(sequence, capturedAt, method, safeUrl,
                path(safeUrl), status, redact(requestHeaders), redact(responseHeaders),
                safeRequestBody, safeResponseBody, safeAuthMethod, safeAuthSource,
                safeService, safeEvidenceSource, signature(method, safeUrl, safeRequestBody),
                responseBodyAvailable, safeFailureReason, resourceKind, requestIdentity);
    }

    private static String[] authentication(Map<String, String> headers, String url) {
        if (contains(headers, "authorization")) {
            String value = value(headers, "authorization").toLowerCase(Locale.ROOT);
            int space = value.indexOf(' ');
            String scheme = space > 0 ? value.substring(0, space) : "authorization";
            return new String[]{scheme, "request-header"};
        }
        if (contains(headers, "x-api-key") || contains(headers, "api-key")) {
            return new String[]{"api-key", "request-header"};
        }
        if (contains(headers, "cookie")) return new String[]{"cookie", "request-header"};
        if (hasSensitiveQuery(url)) return new String[]{"signed-url", "query"};
        return new String[]{"none", "none"};
    }

    private static boolean hasSensitiveQuery(String url) {
        try {
            String query = URI.create(url).getRawQuery();
            if (query == null) return false;
            return java.util.Arrays.stream(query.split("&"))
                    .map(part -> part.split("=", 2)[0].toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.contains("token") || name.contains("secret")
                            || name.equals("sig") || name.equals("code") || name.equals("key"));
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("UI network observation received an invalid URL: " + url, error);
        }
    }

    private static Map<String, String> redact(Map<String, String> headers) {
        Map<String, String> safe = new LinkedHashMap<>();
        if (headers == null) return safe;
        headers.forEach((key, value) -> {
            // HTTP/2 pseudo-headers duplicate the typed method/URL and can expose raw query values.
            if (!normalize(key).startsWith(":")) {
                safe.put(key, sensitive(key) ? "REDACTED" : safeHeaderValue(key, value));
            }
        });
        return safe;
    }

    private static Map<String, String> requestHeaders(Request request) {
        // Cached headers omit browser security headers; observe them before redaction.
        Map<String, String> complete = request.allHeaders();
        return complete == null ? request.headers() : complete;
    }

    private static String safeHeaderValue(String key, String value) {
        String safe = normalize(value);
        String name = normalize(key).toLowerCase(Locale.ROOT);
        if (name.equals("location") || name.equals("content-location") || name.equals("referer")) {
            try {
                if (URI.create(safe).getHost() != null) {
                    boolean relativeProtocol = safe.startsWith("//");
                    String redacted = safeUrl(relativeProtocol ? "https:" + safe : safe);
                    return relativeProtocol ? redacted.substring("https:".length()) : redacted;
                }
            } catch (IllegalArgumentException malformedHeaderUrl) {
                // Preserve useful relative/invalid target text, never its query or fragment.
            }
            safe = safe.replaceFirst("[?#].*$", "?redacted-query");
        }
        return REDACTOR.redactText(safe);
    }

    private static boolean sensitive(String key) {
        String name = normalize(key).toLowerCase(Locale.ROOT);
        return name.equals("authorization") || name.equals("proxy-authorization")
                || name.equals("cookie") || name.equals("set-cookie") || name.equals("set-cookie2")
                || name.contains("token") || name.contains("secret")
                || name.equals("x-api-key") || name.equals("api-key");
    }

    private static boolean contains(Map<String, String> headers, String name) {
        return headers != null && headers.keySet().stream()
                .anyMatch(key -> key.equalsIgnoreCase(name));
    }

    private static String value(Map<String, String> headers, String name) {
        if (headers == null) return "";
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue).findFirst().orElse("");
    }

    private static String bounded(String value, int limit) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() <= limit) return value;
        if (limit <= 14) return value.substring(0, limit);
        return value.substring(0, Math.max(0, limit - 14)) + "… [truncated]";
    }

    private static String redactJson(String value) {
        return value == null || value.isBlank() ? value : REDACTOR.redactJson(value);
    }

    private static String safeRedactJson(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            return REDACTOR.redactJson(value);
        } catch (RuntimeException malformedJson) {
            return REDACTOR.redactText(value);
        }
    }

    private static String signature(String method, String url, String body) {
        return normalize(method).toUpperCase(Locale.ROOT) + " " + safeTarget(url)
                + " #body=" + sha256(body == null ? "<absent>" : body);
    }

    private static String safeTarget(String url) {
        try {
            URI uri = URI.create(safeUrl(url));
            return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?<redacted-query>");
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Cannot derive a safe request target from URL: " + url, error);
        }
    }

    private static String safeUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("URL has no host");
            }
            String authority = host + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
            return REDACTOR.redactText(uri.getScheme() + "://" + authority + uri.getRawPath()
                    + (uri.getRawQuery() == null ? "" : "?redacted-query"));
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Cannot derive a safe request URL: " + url, error);
        }
    }

    private static String path(String url) {
        try {
            URI uri = URI.create(safeUrl(url));
            return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?redacted-query");
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("Cannot derive a request path from URL: " + url, error);
        }
    }

    private static String host(String url) {
        try { return normalize(URI.create(url).getHost()); }
        catch (IllegalArgumentException error) {
            throw new IllegalStateException("Cannot derive a service host from URL: " + url, error);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static String normalize(String value) { return value == null ? "" : value.trim(); }
    private static String normalizeBody(String value) { return value == null ? "" : value; }
}

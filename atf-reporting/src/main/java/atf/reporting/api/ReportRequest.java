package atf.reporting.api;

import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.api.request.ReportRequestHeaders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable request evidence; new consumers declare the header source explicitly. */
public final class ReportRequest {
    private static final Map<String, String> LEGACY_JSON_DEFAULTS = Map.of(
            ReportHttpHeaders.ACCEPT, "application/json", ReportHttpHeaders.CONTENT_TYPE, "application/json");
    private final Snapshot snapshot;

    /** Existing client constructors retain the ATF JSON-client defaults, not browser observations. */
    public ReportRequest(String method, String path, Map<String, String> query,
                         Map<String, String> headers, String authProfile, String bodyText,
                         ReportAuthObservation authObservation, ReportBodyAvailability bodyAvailability) {
        this(legacy(method, path, headers).query(query).authProfile(authProfile).body(bodyText, bodyAvailability)
                .authentication(authObservation));
    }

    public ReportRequest(String method, String path, Map<String, String> query,
                         Map<String, String> headers, String authProfile, String bodyText,
                         ReportAuthObservation authObservation) {
        this(method, path, query, headers, authProfile, bodyText, authObservation,
                ReportBodyAvailability.from(bodyText));
    }

    public ReportRequest(String method, String path, Map<String, String> query,
                         Map<String, String> headers, String authProfile, String bodyText) {
        this(method, path, query, headers, authProfile, bodyText, null);
    }

    private ReportRequest(Builder builder) {
        String method = required(builder.method, "method");
        String path = required(builder.path, "path");
        Map<String, String> query = immutable(builder.query);
        Map<String, String> headers = immutable(builder.authoredHeaders);
        String profile = builder.authProfile == null || builder.authProfile.isBlank() ? "none" : builder.authProfile;
        var availability = builder.bodyAvailability == null
                ? ReportBodyAvailability.from(builder.bodyText) : builder.bodyAvailability;
        var authentication = builder.authentication == null
                ? ReportAuthObservation.from(profile, headers, query) : builder.authentication;
        snapshot = new Snapshot(method, path, query, headers, profile,
                builder.bodyText == null ? "" : builder.bodyText, authentication, availability,
                Objects.requireNonNull(builder.headerEvidence, "headerEvidence"));
    }

    public static Builder builder(String method, String path) { return new Builder(method, path); }

    public Builder toBuilder() {
        Builder builder = builder(method(), path()).query(query()).authProfile(authProfile())
                .headers(headerEvidence()).body(bodyText(), bodyAvailability()).authentication(authObservation());
        builder.authoredHeaders = headers();
        return builder;
    }

    public String method() { return snapshot.method(); }
    public String path() { return snapshot.path(); }
    public Map<String, String> query() { return snapshot.query(); }
    /** Authored fields retained for compatibility; publication uses headerEvidence(). */
    public Map<String, String> headers() { return snapshot.headers(); }
    public String authProfile() { return snapshot.authProfile(); }
    public String bodyText() { return snapshot.bodyText(); }
    public ReportAuthObservation authObservation() { return snapshot.authObservation(); }
    public ReportBodyAvailability bodyAvailability() { return snapshot.bodyAvailability(); }
    public ReportRequestHeaders headerEvidence() { return snapshot.headerEvidence(); }
    public ReportAuthMethod authMethod() { return authObservation().method(); }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof ReportRequest request && snapshot.equals(request.snapshot);
    }
    @Override public int hashCode() { return snapshot.hashCode(); }
    @Override public String toString() { return "%s %s".formatted(method(), path()); }

    private static Builder legacy(String method, String path, Map<String, String> headers) {
        Builder builder = builder(method, path).headers(ReportRequestHeaders.configured(LEGACY_JSON_DEFAULTS, headers));
        builder.authoredHeaders = immutable(headers);
        return builder;
    }

    private static Map<String, String> immutable(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private record Snapshot(String method, String path, Map<String, String> query,
                            Map<String, String> headers, String authProfile, String bodyText,
                            ReportAuthObservation authObservation, ReportBodyAvailability bodyAvailability,
                            ReportRequestHeaders headerEvidence) { }

    public static final class Builder {
        private final String method;
        private final String path;
        private Map<String, String> query = Map.of();
        private Map<String, String> authoredHeaders = Map.of();
        private ReportRequestHeaders headerEvidence = ReportRequestHeaders.observed(Map.of());
        private String authProfile;
        private String bodyText;
        private ReportBodyAvailability bodyAvailability;
        private ReportAuthObservation authentication;

        private Builder(String method, String path) { this.method = method; this.path = path; }
        public Builder query(Map<String, String> value) { query = value; return this; }
        public Builder authProfile(String value) { authProfile = value; return this; }
        public Builder headers(ReportRequestHeaders value) {
            headerEvidence = Objects.requireNonNull(value, "headerEvidence");
            authoredHeaders = value.values();
            return this;
        }
        public Builder body(String value, ReportBodyAvailability availability) {
            bodyText = value; bodyAvailability = availability; return this;
        }
        public Builder authentication(ReportAuthObservation value) { authentication = value; return this; }
        public ReportRequest build() { return new ReportRequest(this); }
    }
}

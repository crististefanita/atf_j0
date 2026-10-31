package atf.reporting.model;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable non-transport evidence. It carries facts, never an assertion verdict. */
public record ReportObservation(String title, List<ReportField> fields,
                                List<Body> bodies, List<Artifact> artifacts) {
    public ReportObservation {
        title = label(title);
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        bodies = List.copyOf(Objects.requireNonNull(bodies, "bodies"));
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts"));
    }
    public enum Format { TEXT, JSON }
    public record Body(String label, String text, Format format) {
        public Body {
            label = ReportObservation.label(label);
            text = Objects.requireNonNull(text, "text");
            format = Objects.requireNonNull(format, "format");
        }
    }
    /** A reference only: Reporting never reads or publishes the referenced file. */
    public record Artifact(String label, URI location, String sha256) {
        public Artifact {
            label = ReportObservation.label(label);
            Objects.requireNonNull(location, "location");
            if (!location.isAbsolute() || location.isOpaque()
                    || !List.of("file", "http", "https").contains(location.getScheme().toLowerCase(Locale.ROOT))
                    || location.getRawUserInfo() != null || location.getRawQuery() != null
                    || location.getRawFragment() != null || location.getRawPath() == null
                    || !location.getRawPath().startsWith("/")
                    || (!location.getScheme().equalsIgnoreCase("file") && location.getHost() == null)) {
                throw new IllegalArgumentException("Artifact requires an absolute file/http/https URI without credentials, query or fragment");
            }
            sha256 = Objects.requireNonNullElse(sha256, "").toLowerCase(Locale.ROOT);
            if (!sha256.isEmpty() && !sha256.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid artifact SHA-256");
        }
    }
    private static String label(String value) {
        if (Objects.requireNonNull(value, "label").isBlank()) throw new IllegalArgumentException("Blank evidence label");
        return value;
    }
}

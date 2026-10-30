package atf.reporting.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import atf.reporting.security.SensitiveDataRedactor;

/** Supplies stable execution metadata without coupling lifecycle code to the host runtime. */
public final class ReportEnvironment {
    private ReportEnvironment() {
    }

    /** Operational facts owned by the renderer, independent of consumer metadata and body capture. */
    public static ReportEnvironmentSnapshot reportingSettings(ReportingSettings settings) {
        return ReportEnvironmentSnapshot.builder().put(ReportEnvironmentField.ASSERTION_PREVIEW,
                java.util.Objects.requireNonNull(settings, "settings").assertionMaxDisplayCharacters() + " characters").build();
    }

    public static Map<String, String> defaults() {
        var metadata = new LinkedHashMap<String, String>();
        metadata.put("Environment", "LOCAL");
        metadata.put("Runtime", "Java " + System.getProperty("java.version", "unknown"));
        metadata.put("OS", System.getProperty("os.name", "unknown") + " "
                + System.getProperty("os.version", "unknown"));
        metadata.put("Client", "Configured per execution");
        metadata.put("Reporting", "Extent · embedded offline HTML");
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Merges consumer-owned, already semantic metadata into the generic
     * environment without allowing secrets to cross the report boundary.
     */
    public static Map<String, String> with(Map<String, String> consumerMetadata,
                                           SensitiveDataRedactor redactor) {
        var metadata = new LinkedHashMap<>(defaults());
        if (consumerMetadata != null) {
            var safeRedactor = redactor == null ? new SensitiveDataRedactor() : redactor;
            consumerMetadata.forEach((name, value) -> {
                if (name == null || name.isBlank()) return;
                String key = name.trim();
                metadata.put(key, safeRedactor.redactField(key, value));
            });
        }
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Publishes the typed allowlist through the same redaction boundary as
     * legacy consumer metadata.
     */
    public static Map<String, String> with(ReportEnvironmentSnapshot snapshot,
                                           SensitiveDataRedactor redactor) {
        return with(snapshot == null ? Map.of() : snapshot.systemDisplayValues(), redactor);
    }
}

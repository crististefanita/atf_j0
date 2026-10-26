package atf.reporting.api.contract;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Stable field vocabulary emitted by the API evidence projection. */
public enum ApiReportEvidenceField {
    AUTH_PROFILE("AUTH PROFILE"),
    REQUEST_HEADERS("REQUEST HEADERS"),
    RESPONSE_HEADERS("RESPONSE HEADERS"),
    HTTP_STATUS("HTTP STATUS"),
    REQUEST_URL("URL"),
    CONTENT_TYPE("CONTENT TYPE", "CONTENT-TYPE", "response.content.type"),
    RESPONSE_STATUS("RESPONSE STATUS", "response.status"),
    RESPONSE_URL("RESPONSE URL", "response.url");

    private final String key;
    private final List<String> aliases;

    ApiReportEvidenceField(String key, String... aliases) {
        this.key = key;
        this.aliases = Stream.concat(Stream.of(key), Stream.of(aliases)).toList();
    }

    public String key() {
        return key;
    }

    public boolean matches(String candidate) {
        return candidate != null && aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(candidate.trim()));
    }

    /** Keeps every observed value so contradictory aliases cannot hide behind a preferred key. */
    public List<String> valuesIn(Map<String, String> fields) {
        return fields.entrySet().stream().filter(entry -> matches(entry.getKey()))
                .map(Map.Entry::getValue).distinct().toList();
    }
}

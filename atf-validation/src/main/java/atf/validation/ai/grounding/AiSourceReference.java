package atf.validation.ai.grounding;

import java.util.Objects;

/** Redacted, stable pointer to a source; it deliberately cannot carry source content. */
public record AiSourceReference(String sourceId, SourceType sourceType, String location,
                                String excerptReference, String fieldPath) {

    public AiSourceReference {
        sourceId = opaque(sourceId, "sourceId");
        sourceType = Objects.requireNonNull(sourceType, "sourceType");
        location = opaque(location, "location");
        excerptReference = opaque(excerptReference, "excerptReference");
        fieldPath = text(fieldPath, "fieldPath");
    }

    private static String text(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static String opaque(String value, String name) {
        value = text(value, name);
        if (value.length() > 160 || value.chars().anyMatch(Character::isWhitespace)
                || value.chars().anyMatch(Character::isISOControl) || value.contains("://")) {
            throw new IllegalArgumentException(name + " must be a bounded opaque reference");
        }
        return value;
    }

    public enum SourceType { INPUT_FIELD, DOCUMENT_SPAN, EVENT, API_RESPONSE, CONSUMER_DEFINED }
}

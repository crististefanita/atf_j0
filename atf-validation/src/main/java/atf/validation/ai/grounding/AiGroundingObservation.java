package atf.validation.ai.grounding;

/** One field-level provenance decision retained in the renderer-neutral snapshot. */
public record AiGroundingObservation(String fieldPath, AiSourceReference sourceReference,
                                     Result result, String reasonCode) {
    public AiGroundingObservation {
        if (fieldPath == null || fieldPath.isBlank()) throw new IllegalArgumentException("fieldPath must not be blank");
        if (result == null) throw new NullPointerException("result");
        if (reasonCode == null || reasonCode.isBlank()) throw new IllegalArgumentException("reasonCode must not be blank");
        if (result == Result.SUPPORTED && sourceReference == null) {
            throw new IllegalArgumentException("SUPPORTED grounding requires a source reference");
        }
    }

    public enum Result { SUPPORTED, UNSUPPORTED, AMBIGUOUS, NOT_REQUIRED, MISSING_REFERENCE }
}

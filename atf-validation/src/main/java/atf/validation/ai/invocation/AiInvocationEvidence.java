package atf.validation.ai.invocation;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Redacted references and lifecycle facts for one model/fallback invocation; never stores prompts or credentials. */
public record AiInvocationEvidence(
        Mode mode,
        String invocationId,
        String providerReference,
        String modelReference,
        boolean outputProduced,
        Instant startedAt,
        Instant completedAt,
        List<Reference> references) {

    public AiInvocationEvidence {
        Objects.requireNonNull(mode, "mode");
        references = List.copyOf(references);
        if ((startedAt == null) != (completedAt == null)) {
            throw new IllegalArgumentException("startedAt and completedAt must be provided together");
        }
        validateOpaqueId(invocationId, "invocationId");
        validateOpaqueId(providerReference, "providerReference");
        validateOpaqueId(modelReference, "modelReference");
        var kinds = new HashSet<ReferenceKind>();
        for (var reference : references) {
            if (!kinds.add(reference.kind())) throw new IllegalArgumentException("duplicate evidence reference kind: " + reference.kind());
        }
        if (startedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("completedAt must not precede startedAt");
        }
    }

    public boolean completeRealInvocation() {
        return mode == Mode.REAL_AI
                && hasText(invocationId) && hasText(providerReference) && hasText(modelReference)
                && outputProduced && startedAt != null && hasReference(ReferenceKind.INVOCATION)
                && hasReference(ReferenceKind.REQUEST) && hasReference(ReferenceKind.RESPONSE)
                && hasReference(ReferenceKind.OUTPUT);
    }

    public Reference reference(ReferenceKind kind) {
        return references.stream().filter(reference -> reference.kind() == kind).findFirst().orElse(null);
    }

    private boolean hasReference(ReferenceKind kind) {
        return references.stream().anyMatch(reference -> reference.kind() == kind);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    private static void validateOpaqueId(String value, String name) {
        if (value == null) return;
        if (value.length() > 160 || value.chars().anyMatch(Character::isWhitespace)
                || value.chars().anyMatch(Character::isISOControl) || value.contains("://")) {
            throw new IllegalArgumentException(name + " must be a bounded opaque identifier, not inline content or a URL");
        }
    }

    public enum Mode { REAL_AI, MOCK_MODEL, EXPLICIT_FALLBACK, UNKNOWN }
    public enum ReferenceKind { INVOCATION, REQUEST, RESPONSE, OUTPUT }

    /** Opaque evidence pointer only. Payloads, prompt text, tokens, and credentials are not accepted here. */
    public record Reference(ReferenceKind kind, String referenceId) {
        public Reference {
            Objects.requireNonNull(kind, "kind");
            if (referenceId == null || referenceId.isBlank()) {
                throw new IllegalArgumentException("referenceId must not be blank");
            }
            if (referenceId.length() > 160 || referenceId.chars().anyMatch(Character::isWhitespace)
                    || referenceId.chars().anyMatch(Character::isISOControl) || referenceId.contains("://")) {
                throw new IllegalArgumentException("referenceId must be a bounded opaque identifier, not inline content or a URL");
            }
        }
    }
}

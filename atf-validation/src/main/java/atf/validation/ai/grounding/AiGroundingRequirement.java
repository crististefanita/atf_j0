package atf.validation.ai.grounding;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Expected provenance for one AI field; it never contains source payloads. */
public record AiGroundingRequirement(boolean required, Set<String> allowedSourceIds) {

    public AiGroundingRequirement {
        allowedSourceIds = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(allowedSourceIds,
                "allowedSourceIds")));
        for (var sourceId : allowedSourceIds) {
            if (sourceId == null || sourceId.isBlank() || sourceId.length() > 160
                    || sourceId.chars().anyMatch(Character::isWhitespace)
                    || sourceId.chars().anyMatch(Character::isISOControl)
                    || sourceId.contains("://")) {
                throw new IllegalArgumentException("allowed source ids must be bounded opaque identifiers");
            }
        }
        if (required && allowedSourceIds.isEmpty()) {
            throw new IllegalArgumentException("required grounding must declare an allowed source");
        }
    }

    public static AiGroundingRequirement none() {
        return new AiGroundingRequirement(false, Set.of());
    }

    public static AiGroundingRequirement requiredFrom(String... sourceIds) {
        Objects.requireNonNull(sourceIds, "sourceIds");
        return new AiGroundingRequirement(true, Set.of(sourceIds));
    }
}

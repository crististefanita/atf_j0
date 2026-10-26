package atf.shop.config.api;

import atf.shop.config.ShopValidationProfile;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Typed, fail-closed binding between a Practice Shop journey and its API
 * checkpoints. It keeps selection out of the network journal and out of the
 * renderer, while retaining an explicit YAML seam for future journeys.
 */
public record ShopApiEvidenceBinding(Set<ShopApiCheckpoint> checkpoints) {
    public ShopApiEvidenceBinding {
        Objects.requireNonNull(checkpoints, "checkpoints");
        checkpoints = checkpoints.isEmpty()
                ? EnumSet.noneOf(ShopApiCheckpoint.class)
                : EnumSet.copyOf(checkpoints);
        checkpoints = Set.copyOf(checkpoints);
    }

    public static ShopApiEvidenceBinding all() {
        return new ShopApiEvidenceBinding(EnumSet.allOf(ShopApiCheckpoint.class));
    }

    public static ShopApiEvidenceBinding none() {
        return new ShopApiEvidenceBinding(EnumSet.noneOf(ShopApiCheckpoint.class));
    }

    /** Missing YAML means all checkpoints for UI_API, but none for UI_ONLY. */
    public static ShopApiEvidenceBinding forProfile(
            ShopValidationProfile profile, Collection<ShopApiCheckpoint> configured) {
        Objects.requireNonNull(profile, "profile");
        if (!profile.apiEvidenceRequired()) return none();
        return configured == null ? all() : new ShopApiEvidenceBinding(Set.copyOf(configured));
    }

    /** Null inherits the profile default; an explicitly blank value selects no checkpoints. */
    public static ShopApiEvidenceBinding fromCsv(
            ShopValidationProfile profile, String csv) {
        if (csv == null) return forProfile(profile, null);
        if (csv.isBlank()) return forProfile(profile, Set.of());
        Set<ShopApiCheckpoint> checkpoints = java.util.Arrays.stream(csv.split(","))
                .map(ShopApiCheckpoint::from)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ShopApiCheckpoint.class)));
        return forProfile(profile, checkpoints);
    }

    public boolean includes(ShopApiCheckpoint checkpoint) {
        return checkpoints.contains(Objects.requireNonNull(checkpoint, "checkpoint"));
    }

    public String displayValue() {
        return checkpoints.stream().map(ShopApiCheckpoint::key)
                .sorted().collect(Collectors.joining(", "));
    }
}

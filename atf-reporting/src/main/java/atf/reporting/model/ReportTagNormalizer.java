package atf.reporting.model;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Canonicalizes report tags for display, filtering and semantic snapshots. */
public final class ReportTagNormalizer {
    private ReportTagNormalizer() {
    }

    public static List<String> normalize(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}

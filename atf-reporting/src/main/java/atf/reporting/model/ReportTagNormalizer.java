package atf.reporting.model;

import atf.reporting.semantic.tags.ReportTags;
import java.util.Collection;
import java.util.List;

/**
 * Binary-compatible entry point for callers compiled before the semantic tag owner.
 * @deprecated Use {@link ReportTags}; this bounded bridge preserves the supported signature.
 */
@Deprecated(since = "1.0.0-j10", forRemoval = false)
public final class ReportTagNormalizer {
    private ReportTagNormalizer() { }

    public static List<String> normalize(Collection<String> tags) {
        return ReportTags.normalize(tags);
    }
}

package atf.reporting.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caller-owned, configurable size policy for native and published report assets.
 *
 * <p>The policy deliberately contains no fixed project baseline. A consumer may
 * load the limits from its configuration, while reporting keeps the comparison
 * deterministic and independent of generated HTML artifacts.</p>
 */
public record ReportAssetSizeBudget(
        long maxNativeBytes,
        long maxPublicationBytes,
        Map<ReportAsset.Format, Long> maxNativeBytesByFormat,
        Map<ReportAsset.Format, Long> maxPublicationBytesByFormat,
        Map<String, Long> maxNativeBytesByOwner,
        Map<String, Long> maxPublicationBytesByOwner) {

    public ReportAssetSizeBudget {
        requireNonNegative(maxNativeBytes, "maxNativeBytes");
        requireNonNegative(maxPublicationBytes, "maxPublicationBytes");
        maxNativeBytesByFormat = copyLimits(maxNativeBytesByFormat, "maxNativeBytesByFormat");
        maxPublicationBytesByFormat = copyLimits(maxPublicationBytesByFormat,
                "maxPublicationBytesByFormat");
        maxNativeBytesByOwner = copyLimits(maxNativeBytesByOwner, "maxNativeBytesByOwner");
        maxPublicationBytesByOwner = copyLimits(maxPublicationBytesByOwner,
                "maxPublicationBytesByOwner");
    }

    /** A policy that only validates the inventory's structural invariants. */
    public static ReportAssetSizeBudget unbounded() {
        return new ReportAssetSizeBudget(Long.MAX_VALUE, Long.MAX_VALUE,
                Map.of(), Map.of(), Map.of(), Map.of());
    }

    public List<String> violations(ReportAssetSizeInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        List<String> violations = new ArrayList<>();
        checkTotal(violations, "native total", inventory.nativeBytes(), maxNativeBytes);
        checkTotal(violations, "publication total", inventory.publicationBytes(), maxPublicationBytes);
        checkSegments(violations, "native format", inventory.bytesByFormat(false),
                maxNativeBytesByFormat);
        checkSegments(violations, "publication format", inventory.bytesByFormat(true),
                maxPublicationBytesByFormat);
        checkSegments(violations, "native owner", inventory.bytesByOwner(false),
                maxNativeBytesByOwner);
        checkSegments(violations, "publication owner", inventory.bytesByOwner(true),
                maxPublicationBytesByOwner);
        return List.copyOf(violations);
    }

    public boolean accepts(ReportAssetSizeInventory inventory) {
        return violations(inventory).isEmpty();
    }

    private static void checkTotal(List<String> violations, String label, long actual, long limit) {
        if (actual > limit) {
            violations.add(label + " exceeds " + limit + " bytes: " + actual);
        }
    }

    private static <K> void checkSegments(List<String> violations, String label,
                                          Map<K, Long> actual, Map<K, Long> limits) {
        for (Map.Entry<K, Long> limit : limits.entrySet()) {
            long observed = actual.getOrDefault(limit.getKey(), 0L);
            checkTotal(violations, label + "[" + limit.getKey() + "]", observed,
                    limit.getValue());
        }
    }

    private static <K> Map<K, Long> copyLimits(Map<K, Long> values, String field) {
        Objects.requireNonNull(values, field);
        values.forEach((key, value) -> {
            Objects.requireNonNull(key, field + " key");
            Objects.requireNonNull(value, field + "[" + key + "]");
            requireNonNegative(value, field + "[" + key + "]");
        });
        return Map.copyOf(values);
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must be non-negative");
    }
}

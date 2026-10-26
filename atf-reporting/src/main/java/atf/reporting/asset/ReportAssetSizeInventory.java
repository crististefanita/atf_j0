package atf.reporting.asset;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic size view for native sources and their publication copies. */
public record ReportAssetSizeInventory(List<Entry> entries) {
    public ReportAssetSizeInventory {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public static ReportAssetSizeInventory capture(ReportAssetLoader loader,
                                                   ReportAssetManifest manifest) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(manifest, "manifest");
        return new ReportAssetSizeInventory(manifest.assets().stream()
                .map(asset -> entry(asset, loader.load(asset), loader.loadForPublication(asset)))
                .toList());
    }

    public long nativeBytes() {
        return entries.stream().mapToLong(Entry::nativeBytes).sum();
    }

    public long publicationBytes() {
        return entries.stream().mapToLong(Entry::publicationBytes).sum();
    }

    public Map<ReportAsset.Format, Long> bytesByFormat(boolean publication) {
        EnumMap<ReportAsset.Format, Long> result = new EnumMap<>(ReportAsset.Format.class);
        for (Entry entry : entries) {
            long bytes = publication ? entry.publicationBytes() : entry.nativeBytes();
            result.merge(entry.asset().format(), bytes, Long::sum);
        }
        return Map.copyOf(result);
    }

    public Map<String, Long> bytesByOwner(boolean publication) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Entry entry : entries) {
            long bytes = publication ? entry.publicationBytes() : entry.nativeBytes();
            result.merge(entry.asset().owner(), bytes, Long::sum);
        }
        return Map.copyOf(result);
    }

    private static Entry entry(ReportAsset asset, String nativeSource, String publication) {
        return new Entry(asset,
                bytes(nativeSource), bytes(publication),
                nativeSource.contains("\n") || nativeSource.contains("\r"),
                !publication.contains("\n") && !publication.contains("\r"));
    }

    private static int bytes(String source) {
        return source.getBytes(StandardCharsets.UTF_8).length;
    }

    public record Entry(ReportAsset asset, int nativeBytes, int publicationBytes,
                        boolean nativeSourceFormatted, boolean publicationCompacted) {
        public Entry {
            asset = Objects.requireNonNull(asset, "asset");
            if (nativeBytes < 0 || publicationBytes < 0) {
                throw new IllegalArgumentException("asset sizes must not be negative");
            }
        }
    }
}

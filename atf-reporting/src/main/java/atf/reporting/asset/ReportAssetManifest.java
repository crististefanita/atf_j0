package atf.reporting.asset;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validated, deterministic inventory of every browser asset published by reporting.
 *
 * <p>The enum remains the compact asset vocabulary; this manifest is the explicit
 * composition boundary used by tests, diagnostics and future publication policies.
 * It prevents a new asset from silently bypassing the path, format, owner or
 * completeness contract.</p>
 */
public final class ReportAssetManifest {
    private static final ReportAssetManifest PRODUCTION =
            new ReportAssetManifest(Arrays.asList(ReportAsset.values()));

    private final List<ReportAsset> assets;

    private ReportAssetManifest(List<ReportAsset> assets) {
        this.assets = validate(assets);
    }

    public static ReportAssetManifest production() {
        return PRODUCTION;
    }

    static ReportAssetManifest of(List<ReportAsset> assets) {
        return new ReportAssetManifest(assets);
    }

    public List<ReportAsset> assets() {
        return assets;
    }

    public List<ReportAsset> cssAssets() {
        return assets.stream()
                .filter(asset -> asset.format() == ReportAsset.Format.CSS)
                .toList();
    }

    public List<ReportAsset> javascriptAssets() {
        return assets.stream()
                .filter(asset -> asset.format() == ReportAsset.Format.JAVASCRIPT)
                .toList();
    }

    private static List<ReportAsset> validate(List<ReportAsset> candidate) {
        Objects.requireNonNull(candidate, "assets");
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("Reporting asset manifest must not be empty");
        }

        Set<ReportAsset> seenAssets = EnumSet.noneOf(ReportAsset.class);
        Set<String> paths = new HashSet<>();
        Set<String> owners = new HashSet<>();
        for (ReportAsset asset : candidate) {
            Objects.requireNonNull(asset, "asset entry");
            if (!seenAssets.add(asset)) {
                throw new IllegalArgumentException("Duplicate reporting asset: " + asset.name());
            }
            if (!paths.add(asset.resourcePath())) {
                throw new IllegalArgumentException("Duplicate reporting asset path: " + asset.resourcePath());
            }
            if (!owners.add(asset.owner())) {
                throw new IllegalArgumentException("Duplicate reporting asset owner: " + asset.owner());
            }
            if (!asset.resourcePath().startsWith("/atf/reporting/")) {
                throw new IllegalArgumentException("Reporting asset outside owned namespace: "
                        + asset.resourcePath());
            }
            if (!asset.resourcePath().endsWith(asset.format().extension())) {
                throw new IllegalArgumentException("Reporting asset format does not match path: "
                        + asset.resourcePath());
            }
        }

        EnumSet<ReportAsset> expected = EnumSet.allOf(ReportAsset.class);
        if (!seenAssets.equals(expected)) {
            expected.removeAll(seenAssets);
            throw new IllegalArgumentException("Incomplete reporting asset manifest; missing: " + expected);
        }
        return List.copyOf(candidate);
    }
}

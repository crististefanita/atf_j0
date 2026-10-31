package atf.reporting.asset;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportAssetSizeInventoryTest {
    // The table-layout, network-disclosure and bounded-evidence contracts are
    // part of the published runtime; keep measured headroom without masking
    // unrelated growth.
    private static final long NATIVE_JAVASCRIPT_BASELINE_BYTES = 130_000L;
    // J8: fallback ARIA synchronization adds exactly 394 published bytes over J7.
    // Bound that owner separately so unrelated headroom cannot hide further growth.
    private static final long PUBLICATION_JAVASCRIPT_BASELINE_BYTES = 107_500L;
    private static final long FALLBACK_NAVIGATION_PUBLICATION_BYTES = 2_848L;
    private static final long NATIVE_TOTAL_BASELINE_BYTES = 265_000L;
    private static final long PUBLICATION_TOTAL_BASELINE_BYTES = 223_000L;
    private static final long NATIVE_CSS_BASELINE_BYTES = 140_000L;
    private static final long PUBLICATION_CSS_BASELINE_BYTES = 120_000L;

    // Includes the typed component assertion disclosure. The old native JS
    // ceiling was already below the source inventory at the base revision;
    // these ceilings retain less than 2 KB headroom per changed dimension.
    private static final ReportAssetSizeBudget CONSUMER_BASELINE = new ReportAssetSizeBudget(
            NATIVE_TOTAL_BASELINE_BYTES, PUBLICATION_TOTAL_BASELINE_BYTES,
            java.util.Map.of(
                    ReportAsset.Format.CSS, NATIVE_CSS_BASELINE_BYTES,
                    ReportAsset.Format.JAVASCRIPT, NATIVE_JAVASCRIPT_BASELINE_BYTES),
            java.util.Map.of(
                    // Tall shared disclosure summaries and sticky bounded
                    // evidence headers are an intentional publication cost.
                    ReportAsset.Format.CSS, PUBLICATION_CSS_BASELINE_BYTES,
                    ReportAsset.Format.JAVASCRIPT, PUBLICATION_JAVASCRIPT_BASELINE_BYTES),
            java.util.Map.of(), java.util.Map.of(
                    ReportAsset.FALLBACK_NAVIGATION_JS.owner(), FALLBACK_NAVIGATION_PUBLICATION_BYTES));

    @Test
    void measuresNativeAndPublicationCopiesWithoutLosingTheFormatBoundary() {
        ReportAssetSizeInventory inventory = ReportAssetSizeInventory.capture(
                new ReportAssetLoader(), ReportAssetManifest.production());

        assertAll(
                () -> assertEquals(ReportAsset.values().length, inventory.entries().size()),
                () -> assertTrue(inventory.nativeBytes() > 0),
                () -> assertTrue(inventory.publicationBytes() > 0),
                () -> assertTrue(inventory.nativeBytes() >= inventory.publicationBytes()),
                () -> assertEquals(2, inventory.bytesByFormat(false).size()),
                () -> assertEquals(ReportAsset.values().length, inventory.bytesByOwner(false).size()),
                () -> assertTrue(inventory.entries().stream()
                        .allMatch(ReportAssetSizeInventory.Entry::nativeSourceFormatted)),
                () -> assertTrue(inventory.entries().stream()
                        .allMatch(ReportAssetSizeInventory.Entry::publicationCompacted)),
                () -> assertFalse(inventory.entries().stream()
                        .anyMatch(entry -> entry.publicationBytes() > entry.nativeBytes())));
    }

    @Test
    void keepsTheConsumerAssetBundleInsideItsExplicitPublicationBaseline() {
        ReportAssetSizeInventory inventory = ReportAssetSizeInventory.capture(
                new ReportAssetLoader(), ReportAssetManifest.production());

        assertTrue(CONSUMER_BASELINE.accepts(inventory),
                () -> "Reporting asset baseline violations: "
                        + CONSUMER_BASELINE.violations(inventory));
    }

    @Test
    void rejectsOneExtraFallbackByteEvenWhenTheFormatBudgetHasHeadroom() {
        var inventory = ReportAssetSizeInventory.capture(new ReportAssetLoader(), ReportAssetManifest.production());
        var entries = inventory.entries().stream().map(entry ->
                entry.asset() == ReportAsset.FALLBACK_NAVIGATION_JS
                        ? new ReportAssetSizeInventory.Entry(entry.asset(), entry.nativeBytes() + 1,
                                (int) FALLBACK_NAVIGATION_PUBLICATION_BYTES + 1,
                                entry.nativeSourceFormatted(), entry.publicationCompacted())
                        : entry).toList();
        var changed = new ReportAssetSizeInventory(entries);
        assertTrue(changed.bytesByFormat(true).get(ReportAsset.Format.JAVASCRIPT)
                < PUBLICATION_JAVASCRIPT_BASELINE_BYTES);
        assertFalse(CONSUMER_BASELINE.accepts(changed));
        assertTrue(CONSUMER_BASELINE.violations(changed).stream()
                .anyMatch(message -> message.startsWith("publication owner[fallback-navigation] exceeds")));
    }
}

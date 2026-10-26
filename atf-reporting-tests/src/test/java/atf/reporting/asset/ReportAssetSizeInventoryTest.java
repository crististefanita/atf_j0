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
    private static final long NATIVE_JAVASCRIPT_BASELINE_BYTES = 125_000L;
    private static final long PUBLICATION_JAVASCRIPT_BASELINE_BYTES = 105_000L;
    private static final long NATIVE_TOTAL_BASELINE_BYTES = 265_000L;
    private static final long PUBLICATION_TOTAL_BASELINE_BYTES = 220_000L;
    private static final long NATIVE_CSS_BASELINE_BYTES = 140_000L;
    private static final long PUBLICATION_CSS_BASELINE_BYTES = 120_000L;

    // Rebased to the measured source/publication inventory while retaining a
    // small explicit headroom for the owned theme and evidence contracts.
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
            java.util.Map.of(), java.util.Map.of());

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
}

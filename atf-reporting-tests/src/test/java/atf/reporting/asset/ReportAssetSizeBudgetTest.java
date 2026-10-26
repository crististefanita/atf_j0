package atf.reporting.asset;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ReportAssetSizeBudgetTest {
    @Test
    void acceptsTheObservedInventoryWhenLimitsAreConfiguredFromTheBaseline() {
        ReportAssetSizeInventory inventory = ReportAssetSizeInventory.capture(
                new ReportAssetLoader(), ReportAssetManifest.production());
        ReportAssetSizeBudget budget = new ReportAssetSizeBudget(
                inventory.nativeBytes(), inventory.publicationBytes(),
                inventory.bytesByFormat(false), inventory.bytesByFormat(true),
                inventory.bytesByOwner(false), inventory.bytesByOwner(true));

        assertTrue(budget.accepts(inventory));
        assertTrue(budget.violations(inventory).isEmpty());
    }

    @Test
    void reportsOnlyTheConfiguredSegmentsThatExceedTheirLimits() {
        ReportAssetSizeInventory inventory = ReportAssetSizeInventory.capture(
                new ReportAssetLoader(), ReportAssetManifest.production());
        ReportAssetSizeBudget budget = new ReportAssetSizeBudget(
                inventory.nativeBytes() - 1, inventory.publicationBytes(),
                Map.of(ReportAsset.Format.CSS, 0L), Map.of(), Map.of(), Map.of());

        var violations = budget.violations(inventory);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(value -> value.startsWith("native total")));
        assertTrue(violations.stream().anyMatch(value -> value.startsWith("native format[CSS]")));
        assertTrue(violations.stream().noneMatch(value -> value.startsWith("publication total")));
    }

    @Test
    void rejectsNegativeLimitsAtConfigurationBoundary() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReportAssetSizeBudget(-1, 0, Map.of(), Map.of(), Map.of(), Map.of()));
    }
}

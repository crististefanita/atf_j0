package atf.autoex.support.catalog;

/** Bounded facts read from the public product-list API before the UI purchase. */
public record AutoExCatalogSnapshot(int status, int productCount, String configuredProductPrice) {
    public AutoExCatalogSnapshot {
        configuredProductPrice = configuredProductPrice == null ? "" : configuredProductPrice;
    }
}

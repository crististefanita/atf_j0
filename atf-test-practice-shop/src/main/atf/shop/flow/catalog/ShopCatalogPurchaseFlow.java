package atf.shop.flow.catalog;

import atf.shop.support.ShopPurchaseCheckpoint;

import java.util.Objects;
import java.util.function.Consumer;

/** Catalog gates publish their observed verdict before product selection or cart mutation. */
public final class ShopCatalogPurchaseFlow {
    public interface Port {
        ShopPurchaseCheckpoint openHome();
        ShopPurchaseCheckpoint inspectFilters();
        ShopPurchaseCheckpoint inspectProductGrid();
        ShopPurchaseCheckpoint openSelectedProduct();
        void addConfiguredQuantity();
    }

    public void prepareCart(Port port, Consumer<ShopPurchaseCheckpoint> publisher) {
        Objects.requireNonNull(port, "port");
        Objects.requireNonNull(publisher, "publisher");
        pass(port.openHome(), publisher);
        pass(port.inspectFilters(), publisher);
        pass(port.inspectProductGrid(), publisher);
        pass(port.openSelectedProduct(), publisher);
        port.addConfiguredQuantity();
    }

    private static void pass(ShopPurchaseCheckpoint checkpoint, Consumer<ShopPurchaseCheckpoint> publisher) {
        publisher.accept(checkpoint);
        if (!checkpoint.passed()) {
            throw new IllegalStateException("Page validation failed: " + checkpoint.title());
        }
    }
}

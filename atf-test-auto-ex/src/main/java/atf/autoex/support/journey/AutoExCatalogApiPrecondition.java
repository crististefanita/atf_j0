package atf.autoex.support.journey;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.support.catalog.AutoExCatalogSnapshot;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Keeps the read-only API setup exchange distinct from browser-causal traffic. */
public final class AutoExCatalogApiPrecondition {
    private AutoExCatalogApiPrecondition() { }

    public static AutoExCatalogSnapshot verifyAndMarkBaseline(
            String productName,
            String expectedPrice,
            Function<String, AutoExCatalogSnapshot> inspectCatalog,
            Consumer<List<AtfAssertion>> publishPrecondition,
            Runnable markCausalBaseline) {
        Objects.requireNonNull(inspectCatalog, "inspectCatalog");
        Objects.requireNonNull(publishPrecondition, "publishPrecondition");
        Objects.requireNonNull(markCausalBaseline, "markCausalBaseline");

        AutoExCatalogSnapshot catalog = inspectCatalog.apply(productName);
        List<AtfAssertion> assertions = List.of(
                new AtfAssertion("Catalog API status", "HTTP 200",
                        "HTTP " + catalog.status(), catalog.status() == 200),
                new AtfAssertion("Catalog API products", "> 0 products",
                        Integer.toString(catalog.productCount()), catalog.productCount() > 0),
                new AtfAssertion("Configured product price", expectedPrice,
                        catalog.configuredProductPrice(),
                        expectedPrice.equals(catalog.configuredProductPrice())));

        publishPrecondition.accept(assertions);
        markCausalBaseline.run();
        return catalog;
    }
}

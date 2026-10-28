package atf.shop.support.api.run;

import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.transport.ApiTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.shop.api.ShopApiClient;
import atf.shop.api.ShopApiScreenFlow;
import atf.shop.api.ShopProductQuery;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.api.contract.ShopProductSort;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.model.ShopCredentials;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.support.api.ShopApiAssertions;
import atf.shop.support.api.ShopApiExecutionExchange;
import atf.shop.support.api.ShopApiReportValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Run-owned API resources and business observations; no JUnit or HTML assertions. */
public final class ShopApiBusinessRun {
    public record Inputs(String productName, int quantity, ShopBillingAddress billing, ShopPaymentMethod payment) {
        public Inputs {
            if (productName == null || productName.isBlank() || quantity < 1)
                throw new IllegalArgumentException("A product and positive quantity are required");
            Objects.requireNonNull(billing); Objects.requireNonNull(payment);
        }
    }
    public record Result(List<ShopApiReportValidation> validations) {
        public Result { validations = List.copyOf(validations); }
        public boolean passed() { return validations.stream().allMatch(ShopApiReportValidation::passed); }
    }
    public interface Audit extends AutoCloseable {
        void observed(ApiExecutionResult execution);
        void requestFailed(ApiRequest request, Throwable failure);
        void validation(ShopApiReportValidation validation);
        void failed(Throwable failure);
        @Override void close();
    }
    private final Supplier<ShopApiClient> clients;
    private final Supplier<? extends Audit> audits;
    private final boolean requireLive;

    public ShopApiBusinessRun(Supplier<ShopApiClient> clients, Supplier<? extends Audit> audits, boolean requireLive) {
        this.clients = Objects.requireNonNull(clients);
        this.audits = Objects.requireNonNull(audits);
        this.requireLive = requireLive;
    }

    public Result execute(Inputs inputs, Supplier<ShopCredentials> credentials) {
        Objects.requireNonNull(inputs); Objects.requireNonNull(credentials);
        // Open audit before client/credentials/network. Java resource suppression retains the original failure.
        try (Audit audit = audits.get()) {
            try (ShopApiClient client = clients.get()) {
                var observations = new ArrayList<ApiExecutionResult>();
                client.observeExecutions(execution -> { observations.add(execution); audit.observed(execution); },
                        audit::requestFailed);
                return business(client, audit, inputs, credentials.get(), observations);
            } catch (RuntimeException | Error failure) {
                try { audit.failed(failure); } catch (RuntimeException | Error auditFailure) {
                    if (failure != auditFailure) failure.addSuppressed(auditFailure);
                }
                throw failure;
            }
        }
    }

    private Result business(ShopApiClient client, Audit audit, Inputs inputs, ShopCredentials credentials,
                            List<ApiExecutionResult> observations) {
        var validations = new ArrayList<ShopApiReportValidation>();
        var flow = new ShopApiScreenFlow(client);
        var login = client.login(credentials);
        add(audit, validations, "Authentication business contract",
                ShopApiAssertions.login(exchange(login.loginResult(), 1, "authentication")));
        if (!login.authenticated()) return new Result(validations);
        var catalog = flow.catalog().openProduct(inputs.productName());
        add(audit, validations, "Catalog business contract", ShopApiAssertions.product(
                exchange(catalog.search(), 2, "catalog"), exchange(catalog.detail(), 3, "catalog"),
                inputs.productName(), catalog.productId()));
        String category = categoryId(catalog.detail().response().body());
        var filtered = client.execute(client.requests().products(new ShopProductQuery(
                null, category, null, null, ShopProductSort.PRICE_ASC, 1)));
        add(audit, validations, "Live category and price-sort contract", ShopApiAssertions.categoryProductsSortedByPrice(
                exchange(filtered, 4, "catalog"), category));
        cart(client, flow, audit, inputs, catalog.productId(), validations);
        boolean provenance = !requireLive || observations.stream().allMatch(
                execution -> execution.provenance() == ApiTransportProvenance.LIVE_HTTP);
        add(audit, validations, "Execution invariants", List.of(
                new AtfAssertion("Filtered catalog HTTP status", "200", Integer.toString(filtered.response().status()),
                        ShopApiHttpStatus.OK.matches(filtered.response().status())),
                new AtfAssertion("Network provenance", requireLive ? "LIVE_HTTP" : "explicit component transport",
                        observations.stream().map(execution -> execution.provenance().name()).distinct().toList().toString(),
                        provenance)));
        return new Result(validations);
    }

    private static void cart(ShopApiClient client, ShopApiScreenFlow flow, Audit audit, Inputs inputs,
                             String productId, List<ShopApiReportValidation> validations) {
        var cart = new ShopApiCartScope(client, productId);
        try (cart) {
            var created = cart.create();
            var added = cart.add(inputs.quantity());
            var read = cart.read();
            int updatedQuantity = inputs.quantity() + 1;
            var updated = cart.updateQuantity(updatedQuantity);
            var readAfterUpdate = cart.read();
            var checkout = flow.checkout().confirm(inputs.billing().country(), inputs.billing().postalCode(),
                    inputs.billing().houseNumber(), inputs.payment());
            var removed = cart.remove();
            var readAfterRemoval = cart.read();
            add(audit, validations, "Cart add business contract", ShopApiAssertions.cart(
                    exchange(added, 6, "cart"), exchange(read, 7, "cart"), productId, inputs.quantity()));
            add(audit, validations, "Live cart mutation and cleanup contract", ShopApiAssertions.cartQuantityUpdateAndRemoval(
                    exchange(updated, 8, "cart"), exchange(readAfterUpdate, 9, "cart"), exchange(removed, 12, "cart"),
                    exchange(readAfterRemoval, 13, "cart"), cart.cartId(), productId, updatedQuantity));
            add(audit, validations, "Checkout business contract", ShopApiAssertions.payment(
                    exchange(checkout.payment(), 11, "checkout"), inputs.payment().wireValue()));
            var writes = List.of(created, added, updated, removed);
            add(audit, validations, "Cart execution invariants", List.of(
                    new AtfAssertion("Write attempts", "1 per non-idempotent write",
                            writes.stream().map(execution -> execution.attempts()).toList().toString(),
                            writes.stream().allMatch(execution -> execution.attempts() == 1)),
                    new AtfAssertion("Cart after removal HTTP status", "200", Integer.toString(readAfterRemoval.response().status()),
                            ShopApiHttpStatus.OK.matches(readAfterRemoval.response().status()))));
        }
    }

    private static void add(Audit audit, List<ShopApiReportValidation> validations, String title, List<AtfAssertion> rows) {
        var validation = new ShopApiReportValidation(title, rows);
        validations.add(validation);
        audit.validation(validation);
    }
    private static atf.shop.support.api.ShopApiNetworkJournal.Exchange exchange(
            ApiExecutionResult result, long sequence, String phase) {
        return ShopApiExecutionExchange.from(result, sequence, phase);
    }
    private static String categoryId(String body) {
        var json = ShopJsonEvidence.parse(body);
        String id = json == null ? "" : json.path(ShopApiJsonField.CATEGORY.wireValue())
                .path(ShopApiJsonField.ID.wireValue()).asText("");
        if (id.isBlank()) throw new IllegalStateException("Product detail did not expose a category id");
        return id;
    }
}

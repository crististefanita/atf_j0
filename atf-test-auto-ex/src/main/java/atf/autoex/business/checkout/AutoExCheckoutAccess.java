package atf.autoex.business.checkout;

import atf.autoex.business.cart.AutoExCartResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One independent owner/cart precondition observed after registration and before checkout. */
public record AutoExCheckoutAccess(String expectedCustomer, String observedCustomer, AutoExCartResult cart) {
    public AutoExCheckoutAccess {
        if (expectedCustomer == null || expectedCustomer.isBlank()) throw new IllegalArgumentException("owned customer is required");
        Objects.requireNonNull(cart, "cart");
    }
    public List<AutoExCartResult.Check> checks() {
        var checks = new ArrayList<AutoExCartResult.Check>();
        checks.add(new AutoExCartResult.Check("Owned customer", expectedCustomer,
                observedCustomer == null || observedCustomer.isBlank() ? "not observed" : observedCustomer,
                expectedCustomer.equals(observedCustomer)));
        checks.addAll(cart.checks());
        return List.copyOf(checks);
    }
    public boolean passed() { return checks().stream().allMatch(AutoExCartResult.Check::passed); }
}

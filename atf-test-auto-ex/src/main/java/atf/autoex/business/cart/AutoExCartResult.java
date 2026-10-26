package atf.autoex.business.cart;

import atf.autoex.support.checkout.AutoExOrderLine;

import java.util.List;
import java.util.Objects;

/** Pure comparison of expected values with one observed row; missing values fail explicitly. */
public record AutoExCartResult(AutoExOrderLine expected, AutoExOrderLine observed) {
    public AutoExCartResult {
        Objects.requireNonNull(expected, "expected");
    }

    public static AutoExCartResult compare(AutoExOrderLine expected, AutoExOrderLine observed) {
        Objects.requireNonNull(expected, "expected");
        return new AutoExCartResult(expected, observed);
    }

    public List<Check> checks() {
        return List.of(
                check("Product", expected.productName(), observed == null ? null : observed.productName()),
                check("Unit price", expected.unitPrice(), observed == null ? null : observed.unitPrice()),
                check("Quantity", expected.quantity(), observed == null ? null : observed.quantity()),
                check("Line total", expected.total(), observed == null ? null : observed.total()));
    }

    public boolean passed() { return checks().stream().allMatch(Check::passed); }

    private static Check check(String label, String expected, String actual) {
        return new Check(label, expected, actual == null ? "not observed" : actual,
                actual != null && !actual.isBlank() && expected.equals(actual));
    }

    public record Check(String label, String expected, String actual, boolean passed) { }
}

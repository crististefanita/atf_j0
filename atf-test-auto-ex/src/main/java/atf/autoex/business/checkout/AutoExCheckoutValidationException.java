package atf.autoex.business.checkout;

/** Keeps the complete failed business precondition independent of its report preview. */
public final class AutoExCheckoutValidationException extends AssertionError {
    private final AutoExCheckoutAccess result;
    public AutoExCheckoutValidationException(AutoExCheckoutAccess result) {
        super("Checkout requires the owned customer and the expected current cart");
        this.result = java.util.Objects.requireNonNull(result, "result");
    }
    public AutoExCheckoutAccess result() { return result; }
}

package atf.autoex.business.cart;

/** Retains the failed snapshot even when its evidence cannot be published. */
public final class AutoExCartValidationException extends IllegalStateException {
    private final AutoExCartResult result;

    public AutoExCartValidationException(AutoExCartResult result) {
        super("Cart validation failed: " + result.checks().stream().filter(check -> !check.passed())
                .map(AutoExCartResult.Check::label).toList());
        this.result = result;
    }

    public AutoExCartResult result() { return result; }
}

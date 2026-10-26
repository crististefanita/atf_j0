package atf.validation.evaluation;

/** Functional contract failure after the aggregate table has been published. */
public final class ApiValidationException extends AssertionError {
    public ApiValidationException(String message) {
        super(message);
    }
}

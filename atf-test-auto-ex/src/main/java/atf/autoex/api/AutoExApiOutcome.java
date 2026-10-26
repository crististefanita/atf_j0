package atf.autoex.api;

/** Documented JSON responseCode/message pairs; these codes are not HTTP transport statuses. */
public enum AutoExApiOutcome {
    METHOD_NOT_SUPPORTED(405, "This request method is not supported."),
    SEARCH_PARAMETER_MISSING(400, "Bad request, search_product parameter is missing in POST request."),
    LOGIN_PARAMETER_MISSING(400, "Bad request, email or password parameter is missing in POST request."),
    USER_EXISTS(200, "User exists!"),
    USER_NOT_FOUND(404, "User not found!"),
    USER_CREATED(201, "User created!"),
    USER_UPDATED(200, "User updated!"),
    ACCOUNT_DELETED(200, "Account deleted!");

    private final int responseCode;
    private final String message;

    AutoExApiOutcome(int responseCode, String message) {
        this.responseCode = responseCode;
        this.message = message;
    }

    public int responseCode() { return responseCode; }
    public String message() { return message; }
}

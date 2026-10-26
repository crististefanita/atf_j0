package atf.shop.api.contract;

/** HTTP status vocabulary used by the Practice Shop API contract tests. */
public enum ShopApiHttpStatus {
    OK(200),
    CREATED(201),
    NO_CONTENT(204),
    UNPROCESSABLE_ENTITY(422),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500);

    private final int code;

    ShopApiHttpStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public String wireValue() {
        return Integer.toString(code);
    }

    public boolean matches(int actual) {
        return code == actual;
    }

    public static boolean isSuccess(int actual) {
        return actual >= 200 && actual < 300;
    }
}

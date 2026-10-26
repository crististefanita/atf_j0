package atf.shop.api;

import atf.api.core.http.response.ApiExecutionResult;

/** Authenticated API precondition produced by the login screen boundary. */
public record ShopApiSession(String bearerToken, ApiExecutionResult loginResult) {
    public ShopApiSession {
        bearerToken = bearerToken == null ? "" : bearerToken;
    }

    public boolean authenticated() { return !bearerToken.isBlank(); }
}

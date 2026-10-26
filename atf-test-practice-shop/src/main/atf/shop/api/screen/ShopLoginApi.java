package atf.shop.api.screen;

import atf.shop.api.ShopApiClient;
import atf.shop.api.ShopApiSession;
import atf.shop.config.model.ShopCredentials;

/** Login-screen API precondition; it is executed once and reused by later screens. */
public final class ShopLoginApi {
    private final ShopApiClient client;

    public ShopLoginApi(ShopApiClient client) {
        this.client = client;
    }

    public ShopApiSession establish(ShopCredentials credentials) {
        ShopApiSession session = client.login(credentials);
        if (!session.authenticated()) {
            var loginResult = session.loginResult();
            var loginResponse = loginResult.response();
            throw new IllegalStateException("Practice Shop login did not return a bearer token; status="
                    + loginResponse.status());
        }
        return session;
    }
}

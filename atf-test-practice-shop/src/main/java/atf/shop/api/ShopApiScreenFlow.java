package atf.shop.api;

import atf.shop.api.screen.ShopCartApi;
import atf.shop.api.screen.ShopCatalogApi;
import atf.shop.api.screen.ShopCheckoutApi;
import atf.shop.api.screen.ShopLoginApi;

/** Composition root for the screen-by-screen API pyramid. */
public final class ShopApiScreenFlow {
    private final ShopApiClient client;

    public ShopApiScreenFlow(ShopApiClient client) { this.client = client; }

    public ShopLoginApi login() { return new ShopLoginApi(client); }
    public ShopCatalogApi catalog() { return new ShopCatalogApi(client); }
    public ShopCartApi cart() { return new ShopCartApi(client); }
    public ShopCheckoutApi checkout() { return new ShopCheckoutApi(client); }
}

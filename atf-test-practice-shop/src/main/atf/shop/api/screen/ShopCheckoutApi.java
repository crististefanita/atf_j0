package atf.shop.api.screen;

import atf.core.http.response.ApiExecutionResult;
import atf.shop.api.ShopApiClient;
import atf.shop.api.contract.ShopPaymentMethod;

/** Checkout-screen API boundary: advisory postcode lookup followed by payment. */
public final class ShopCheckoutApi {
    private final ShopApiClient client;

    public ShopCheckoutApi(ShopApiClient client) { this.client = client; }

    public CheckoutScreen confirm(String country, String postcode, String houseNumber,
                                  ShopPaymentMethod method) {
        ApiExecutionResult address = client.execute(
                client.requests().postcodeLookup(country, postcode, houseNumber));
        ApiExecutionResult payment = client.execute(client.requests().payment(method));
        return new CheckoutScreen(address, payment, method,
                new RequestedAddress(country, postcode, houseNumber), AddressLookupPolicy.ADVISORY);
    }

    public record CheckoutScreen(ApiExecutionResult addressLookup,
                                 ApiExecutionResult payment,
                                 ShopPaymentMethod method,
                                 RequestedAddress requestedAddress,
                                 AddressLookupPolicy addressLookupPolicy) { }

    public record RequestedAddress(String country, String postcode, String houseNumber) { }

    /** The lookup may enrich a form, but it never overwrites submitted billing values. */
    public enum AddressLookupPolicy { ADVISORY }
}

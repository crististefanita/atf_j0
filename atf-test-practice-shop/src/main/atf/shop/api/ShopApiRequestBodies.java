package atf.shop.api;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Typed JSON payloads shared by the Practice Shop request factory. */
public final class ShopApiRequestBodies {
    private ShopApiRequestBodies() { }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ShopLoginRequest(String email, String password) { }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ShopRegistrationRequest(String firstName, String lastName, String email,
                                          String password) { }

    /** Shared by cart add and quantity update, whose wire contracts are identical. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ShopCartItemRequest(String productId, int quantity) { }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ShopPaymentCheckRequest(String paymentMethod,
                                          Map<String, Object> paymentDetails) { }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ShopInvoiceRequest(String billingStreet, String billingCity,
                                     String billingState, String billingCountry,
                                     String billingPostalCode, String paymentMethod,
                                     Map<String, Object> paymentDetails, String cartId,
                                     String guestEmail, String guestFirstName,
                                     String guestLastName) { }
}

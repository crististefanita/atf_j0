package atf.autoex.support.validation;

/** Site-owned UI vocabulary, separate from API messages and generic report labels. */
public enum AutoExUiText {
    STORE_BANNER("Full-Fledged practice website"),
    ACCOUNT_CREATED("ACCOUNT CREATED!"),
    ACCOUNT_DELETED("ACCOUNT DELETED!"),
    CART_ADDED("Added!"),
    CART_ADDED_DETAIL("added to cart"),
    CART_EMPTY("Cart is empty!"),
    IN_STOCK("In Stock"),
    PAYMENT_PAGE("Payment"),
    ORDER_PLACED("Order Placed!"),
    ORDER_CONFIRMED("Your order has been confirmed!");

    private final String text;

    AutoExUiText(String text) { this.text = text; }

    public String text() { return text; }
}

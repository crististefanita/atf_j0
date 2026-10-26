package atf.shop.api.contract;

/** Query-parameter vocabulary for the Practice Shop API. */
public enum ShopApiQueryParameter {
    SEARCH("q"),
    COUNTRY("country"),
    POSTCODE("postcode"),
    HOUSE_NUMBER("house_number"),
    BRAND("by_brand"),
    CATEGORY("by_category"),
    RENTAL("is_rental"),
    PRICE_RANGE("between"),
    SORT("sort"),
    PAGE("page");

    private final String wireValue;

    ShopApiQueryParameter(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}

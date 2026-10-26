package atf.autoex.api;

/** Wire names for the consumer's account, authentication and search form contracts. */
public enum AutoExApiFormField {
    NAME("name"),
    EMAIL("email"),
    PASSWORD("password"),
    TITLE("title"),
    BIRTH_DATE("birth_date"),
    BIRTH_MONTH("birth_month"),
    BIRTH_YEAR("birth_year"),
    FIRST_NAME("firstname"),
    LAST_NAME("lastname"),
    COMPANY("company"),
    ADDRESS1("address1"),
    ADDRESS2("address2"),
    COUNTRY("country"),
    ZIPCODE("zipcode"),
    STATE("state"),
    CITY("city"),
    MOBILE_NUMBER("mobile_number"),
    SEARCH_PRODUCT("search_product");

    private final String value;

    AutoExApiFormField(String value) { this.value = value; }

    public String value() { return value; }
}

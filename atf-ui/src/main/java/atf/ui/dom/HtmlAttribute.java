package atf.ui.dom;

/** Canonical HTML and automation attributes used while composing CSS selectors. */
public enum HtmlAttribute {
    ID("id"),
    NAME("name"),
    TYPE("type"),
    VALUE("value"),
    HREF("href"),
    ALT("alt"),
    ROLE("role"),
    CONTENTEDITABLE("contenteditable"),
    DATA_TEST("data-test");

    private final String attributeName;

    HtmlAttribute(String attributeName) {
        this.attributeName = attributeName;
    }

    public String attributeName() {
        return attributeName;
    }

    public String equalsSelector(String value) {
        return selector("=", value);
    }

    public String startsWithSelector(String value) {
        return selector("^=", value);
    }

    private String selector(String operator, String value) {
        String escaped = CssStringEscaper.escapeQuotedValue(value);
        return "[" + attributeName + operator + "'" + escaped + "']";
    }
}

package atf.ui.dom;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Canonical HTML element names and small, safe CSS-selector composition helpers. */
public enum HtmlElement {
    BODY("body"),
    NAVIGATION("nav"),
    SPAN("span"),
    HEADING_1("h1"),
    INPUT("input"),
    TEXTAREA("textarea"),
    SELECT("select"),
    OPTION("option"),
    BUTTON("button"),
    ANCHOR("a"),
    IMAGE("img"),
    FORM("form"),
    TABLE("table"),
    FOOTER("footer");

    private static final Pattern ATTRIBUTE_NAME =
            Pattern.compile("[A-Za-z_:][A-Za-z0-9_.:-]*");
    private final String selector;

    HtmlElement(String selector) {
        this.selector = selector;
    }

    public String selector() {
        return selector;
    }

    public String attributeEquals(String name, String value) {
        return attributeSelector(name, "=", value);
    }

    public String attributeEquals(HtmlAttribute attribute, String value) {
        return selector + Objects.requireNonNull(attribute, "attribute").equalsSelector(value);
    }

    public String attributeStartsWith(HtmlAttribute attribute, String value) {
        return selector + Objects.requireNonNull(attribute, "attribute").startsWithSelector(value);
    }

    private String attributeSelector(String name, String operator, String value) {
        String attribute = Objects.requireNonNull(name, "name").trim();
        if (!ATTRIBUTE_NAME.matcher(attribute).matches()) {
            throw new IllegalArgumentException("Invalid HTML attribute name: " + name);
        }
        String escaped = CssStringEscaper.escapeQuotedValue(value);
        return selector + "[" + attribute + operator + "'" + escaped + "']";
    }

    public static String selectorList(HtmlElement... elements) {
        Objects.requireNonNull(elements, "elements");
        if (elements.length == 0) throw new IllegalArgumentException("elements must not be empty");
        return Arrays.stream(elements)
                .map(element -> Objects.requireNonNull(element, "element").selector())
                .collect(Collectors.joining(", "));
    }
}

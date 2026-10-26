package atf.shop.support;

/** A country option as exposed by the Practice Shop checkout control. */
public record ShopCountryOption(String value, String label, String text) {
    /** Matches either configured country name or the option's wire/display code. */
    public boolean matches(String configuredCountry) {
        return same(configuredCountry, value)
                || same(configuredCountry, label)
                || same(configuredCountry, text);
    }

    /** Returns the option label accepted by Playwright's label-based selector. */
    public String selectionLabel() {
        if (present(label)) return label.trim();
        if (present(text)) return text.trim();
        return "";
    }

    /** Keeps both the human label and country code visible in a failed assertion. */
    public String displayValue() {
        String name = present(label) ? label.trim() : (present(text) ? text.trim() : "");
        String code = value == null ? "" : value.trim();
        if (name.isBlank()) return code;
        return !code.isBlank() && !same(name, code) ? name + " (" + code + ")" : name;
    }

    private static boolean same(String left, String right) {
        return present(left) && present(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}

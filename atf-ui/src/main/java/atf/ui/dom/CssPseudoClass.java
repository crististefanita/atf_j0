package atf.ui.dom;

import java.util.Objects;

/** Canonical CSS pseudo-classes needed by reusable UI locator composition. */
public enum CssPseudoClass {
    CHECKED("checked");

    private final String cssName;

    CssPseudoClass(String cssName) {
        this.cssName = cssName;
    }

    public String applyTo(String selector) {
        String base = Objects.requireNonNull(selector, "selector").trim();
        if (base.isEmpty()) throw new IllegalArgumentException("selector must not be blank");
        return base + ":" + cssName;
    }
}

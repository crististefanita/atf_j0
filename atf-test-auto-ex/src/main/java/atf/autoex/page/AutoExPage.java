package atf.autoex.page;

import atf.autoex.ui.AutoExUiActions;
import java.util.Objects;

/** Small shared Playwright boundary; selectors remain owned by the locator vocabulary. */
abstract class AutoExPage {
    protected final AutoExUiActions ui;

    AutoExPage(AutoExUiActions ui) { this.ui = Objects.requireNonNull(ui, "ui"); }

    protected void click(String selector, String expected) { ui.click(selector, expected); }
    protected void clickFirst(String selector, String expected) { ui.clickFirst(selector, expected); }
    protected void clickLast(String selector, String expected) { ui.clickLast(selector, expected); }
    protected void fill(String selector, String value, String expected) { ui.fill(selector, value, expected); }
    protected String text(String selector, String expected) { return ui.text(selector, expected); }
    protected void select(String selector, String label, String expected) {
        ui.select(selector, label, expected);
    }
    protected boolean visible(String selector) { return ui.visible(selector); }
}

package atf.autoex.page;

import atf.autoex.support.journey.AutoExPurchaseSession;

/** Small shared Playwright boundary; selectors remain owned by the locator vocabulary. */
abstract class AutoExPage {
    protected final AutoExPurchaseSession run;

    AutoExPage(AutoExPurchaseSession run) { this.run = run; }

    protected void click(String selector, String expected) { run.click(selector, expected); }
    protected void clickFirst(String selector, String expected) { run.clickFirst(selector, expected); }
    protected void clickLast(String selector, String expected) { run.clickLast(selector, expected); }
    protected void fill(String selector, String value, String expected) { run.fill(selector, value, expected); }
    protected String text(String selector, String expected) { return run.text(selector, expected); }
    protected void select(String selector, String label, String expected) {
        run.select(selector, label, expected);
    }
    protected boolean visible(String selector) { return run.visible(selector); }
}

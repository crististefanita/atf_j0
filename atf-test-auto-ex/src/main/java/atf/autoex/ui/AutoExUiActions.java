package atf.autoex.ui;

/** The page-facing operation port; no API journal, configuration, lifecycle or publisher. */
public interface AutoExUiActions {
    void click(String selector, String expected);
    void clickFirst(String selector, String expected);
    void clickLast(String selector, String expected);
    void fill(String selector, String value, String expected);
    void waitForText(String selector, String expectedText, String expected);
    void waitForVisible(String selector);
    String text(String selector, String expected);
    String textFirst(String selector, String expected);
    void select(String selector, String label, String expected);
    boolean visible(String selector);
}

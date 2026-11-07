package atf.reporting.support.navigation;

import static org.junit.jupiter.api.Assertions.*;

/** Wiring guard complemented by real tab/hash/keyboard browser regressions. */
public final class NavigationScriptContract {
    private NavigationScriptContract() { }

    public static void assertTabBindings(String script) {
        assertAll(
                () -> assertTrue(script.contains("root.querySelectorAll(selector)"), "The shared selector must query the DOM"),
                () -> assertTrue(script.contains("const tabs=nodes('.tab')"), "Only actual tabs own activation"),
                () -> assertTrue(script.contains("panels=nodes('.tab-panel')"), "Activation targets the real panels"),
                () -> assertTrue(script.contains("tabs.forEach(tab=>tab.addEventListener('click'")),
                () -> assertTrue(script.contains("window.addEventListener('hashchange'")));
    }
}

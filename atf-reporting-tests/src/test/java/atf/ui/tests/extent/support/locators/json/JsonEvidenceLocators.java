package atf.ui.tests.extent.support.locators.json;

import com.microsoft.playwright.Locator;

/** Locators and deliberate artifact mutations stay separate from the independent assertion oracle. */
public final class JsonEvidenceLocators {
    public Locator body(Locator scenario, boolean fallback) {
        return scenario.locator(".response-json-body pre");
    }

    public void mutate(Locator body, String mutation) {
        body.nth(mutation.equals("rounded-number") ? 0 : 1).evaluate("(e, mutation) => {let s=e.textContent;"
                + "s=mutation==='rounded-number'?s.replace('9007199254740993.0','9007199254740992.0')"
                + ":s.replace(/\\\"seen\\\"\\s*:\\s*2,?/, '');"
                + "e.textContent=s;if(e.value!==undefined)e.value=s;}", mutation);
    }
}

package atf.ui.tests.extent.support.inspection.table;

import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.ElementHandle;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import com.microsoft.playwright.Locator;
import java.util.Map;
import java.util.Objects;

/** Restores the same published table after a deliberate oracle control, without rebuilding the report. */
final class PublishedEvidenceTableMutation implements AutoCloseable {
    private final ElementHandle viewport;
    private final String markup;
    private final String style;

    PublishedEvidenceTableMutation(Locator table) {
        viewport = new PublishedEvidenceTableLocators().viewport(table).elementHandle();
        markup = viewport.innerHTML();
        style = Objects.requireNonNullElse(viewport.getAttribute("style"), "");
    }

    @Override public void close() {
        BrowserResources.finish(() -> viewport.evaluate("(p,data)=>{p.innerHTML=data.html;"
                + "if(data.style)p.setAttribute('style',data.style);else p.removeAttribute('style');}",
                Map.of("html", markup, "style", style)), (AutoCloseable) () -> BrowserResources.dispose(viewport));
    }
}


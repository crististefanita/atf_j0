package atf.autoex.tests.support.geometry;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.PlaywrightUiSession;
import atf.core.ui.AtfUiActionRecord;
import com.microsoft.playwright.PlaywrightException;

import java.time.Duration;
import java.util.List;

/** Local browser fixture for the AutoEx configuration adapter; contains no assertions or business journey. */
public final class AutoExPointerGeometryFixture implements AutoCloseable {
    private final PlaywrightUiSession browser;
    private final PlaywrightActionSequence actions;

    public AutoExPointerGeometryFixture(UiBrowserConfiguration configured) {
        browser = PlaywrightUiSession.open(new UiBrowserConfiguration(configured.baseUrl(), configured.browser(), true,
                Duration.ofMillis(850), Duration.ofSeconds(5), configured.viewportWidth(), configured.viewportHeight(),
                configured.zoomPercent(), false));
        actions = browser.newActionSequence();
        String child = """
                <button id="activate" style="margin:20px;width:130px;height:45px"
                onclick="document.getElementById('clicks').textContent=Number(document.getElementById('clicks').textContent)+1;
                document.getElementById('trusted').textContent=event.isTrusted">Native target</button>
                <output id="clicks">0</output><output id="trusted"></output>
                """;
        String encoded = child.replace("&", "&amp;").replace("\"", "&quot;");
        try {
            browser.page().setContent("<iframe id='same' srcdoc=\"" + encoded + "\"></iframe>"
                    + "<iframe id='opaque' sandbox='allow-scripts' srcdoc=\"" + encoded + "\"></iframe>"
                    + "<div id='cover' style='display:none;position:fixed;inset:0;background:white;z-index:10'></div>");
        } catch (RuntimeException failure) {
            try { browser.close(); } catch (RuntimeException cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public void activate(String frame) {
        try {
            actions.click(new AtfLocator(browser.page().frameLocator("#" + frame).locator("#activate"),
                    "#" + frame + " >> #activate"), "one native activation");
        } catch (PlaywrightException refused) {
            throw new PointerRefused(refused);
        }
    }
    public void cover() { browser.page().evaluate("document.getElementById('cover').style.display='block'"); }
    public String clicks(String frame) { return value(frame, "#clicks"); }
    public String trusted(String frame) { return value(frame, "#trusted"); }
    private String value(String frame, String selector) { return browser.page().frameLocator("#" + frame).locator(selector).innerText(); }
    public int layoutWidth() { return browser.page().viewportSize().width; }
    public int layoutHeight() { return browser.page().viewportSize().height; }
    public double density() { return ((Number) browser.page().evaluate("devicePixelRatio")).doubleValue(); }
    public String rootZoom() { return (String) browser.page().evaluate("document.documentElement.style.zoom"); }
    public byte[] screenshot() { return browser.page().screenshot(); }
    public List<AtfUiActionRecord> records() { return actions.snapshot(); }
    @Override public void close() { browser.close(); }

    /** Only a native Playwright refusal is translated; fixture/programming errors cannot satisfy the negative test. */
    public static final class PointerRefused extends RuntimeException {
        private PointerRefused(PlaywrightException cause) { super("Native pointer refused", cause); }
    }
}


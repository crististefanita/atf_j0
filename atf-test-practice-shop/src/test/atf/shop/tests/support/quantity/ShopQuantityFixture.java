package atf.shop.tests.support.quantity;

import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopProductPage;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.testkit.lifecycle.FailureAwareCleanup;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/** Local product contract only; never reaches the public catalog or dispatches a purchase. */
public final class ShopQuantityFixture implements AutoCloseable {
    public enum State { AVAILABLE, LATE_AVAILABLE, OUT_OF_STOCK, LATE_OUT_OF_STOCK, UNRESOLVED }
    private final Playwright playwright = Playwright.create();
    private final Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    private final Page page = browser.newPage();
    private final ShopLocators locators = new ShopLocators();
    private final PlaywrightActionSequence actions = new PlaywrightActionSequence();

    public ShopQuantityFixture(State state) {
        try { prepare(state); }
        catch (RuntimeException | Error failure) {
            FailureAwareCleanup.closeAfterFailure(failure, playwright, browser, page);
            throw failure;
        }
    }

    private void prepare(State state) {
        page.setDefaultTimeout(500);
        page.setDefaultNavigationTimeout(3000);
        page.route("https://api.practicesoftwaretesting.com/**", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setContentType("application/json").setHeaders(java.util.Map.of("Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "POST,OPTIONS", "Access-Control-Allow-Headers", "content-type"))
                .setBody("{}")));
        page.route("http://quantity-fixture.test/**", route -> route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setContentType("text/html").setBody("<html></html>")));
        page.navigate("http://quantity-fixture.test/product/fixture-product");
        page.setContent("""
                <!doctype html><html><body>
                <h1 data-test='product-name'>Pliers</h1>
                <span data-test='unit-price'>$12.01</span>
                <p data-test='product-description'>Configured product description</p>
                <input data-test='quantity' type='number' min='1' max='99' value='1' disabled
                  oninput='window.fills++;this.value=Math.min(99,Math.max(1,Number(this.value)))'>
                <p data-test='out-of-stock' hidden>Out of stock</p>
                <button data-test='add-to-cart' disabled onclick="window.adds++;
                  window.addedQuantity=document.querySelector('input').value;
                  fetch('https://api.practicesoftwaretesting.com/carts/fixture-cart',{method:'POST',
                    headers:{'Content-Type':'application/json'},body:JSON.stringify({product_id:'fixture-product',quantity:Number(window.addedQuantity)})});
                  document.querySelector('output').textContent='Product added to shopping cart'">Add to cart</button>
                <output></output>
                <script>
                  window.fills=0;window.adds=0;
                  window.available=()=>{document.querySelector('input').disabled=false;
                    document.querySelector('button').disabled=false};
                  window.outOfStock=()=>{document.querySelector('input').disabled=true;
                    document.querySelector('button').disabled=true;document.querySelector('p[hidden]').hidden=false};
                </script>
                </body></html>
                """);
        page.waitForFunction("() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve(true))))",
                null, new Page.WaitForFunctionOptions().setTimeout(3000));
        switch (state) {
            case AVAILABLE -> page.evaluate("window.available()");
            case OUT_OF_STOCK -> page.evaluate("window.outOfStock()");
            case LATE_AVAILABLE -> page.evaluate("setTimeout(window.available,150)");
            case LATE_OUT_OF_STOCK -> page.evaluate("setTimeout(window.outOfStock,150)");
            case UNRESOLVED -> { }
        }
    }

    public ShopProductPage product() { return new ShopProductPage(page, locators, actions); }
    public Page page() { return page; }
    public ShopLocators locators() { return locators; }
    public PlaywrightActionSequence actions() { return actions; }
    public int fills() { return ((Number) page.evaluate("window.fills")).intValue(); }
    public int additions() { return ((Number) page.evaluate("window.adds")).intValue(); }
    public String addedQuantity() { return (String) page.evaluate("window.addedQuantity"); }
    public void removeStock() { page.evaluate("window.outOfStock()"); }

    @Override public void close() { page.close(); browser.close(); playwright.close(); }
}

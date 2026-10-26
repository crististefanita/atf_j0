package atf.shop.support.validation;

import atf.api.core.diagnostics.ThrowableDiagnostics;
import atf.api.reporting.contract.AtfAssertion;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.pages.ShopHomePage;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Objects;

import static atf.api.reporting.audit.ReportAuditChecks.check;

/** Focused, reusable checks for the catalog landing screen. */
public final class ShopHomeScreenValidator {
    private final ShopLocators locators;
    private final ShopPageValidator pages;

    public ShopHomeScreenValidator(ShopLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
        this.pages = new ShopPageValidator(locators);
    }

    public ShopPageValidationResult structure(Page page) {
        return pages.validateControls(page, ShopHomePage.class,
                ShopControl.HOME_NAVIGATION,
                ShopControl.HOME_BRAND,
                ShopControl.HOME_BANNER);
    }

    public List<AtfAssertion> structureStyle(Page page) {
        return List.of(
                style(page, ShopControl.HOME_NAVIGATION, "display", "flex"),
                style(page, ShopControl.HOME_BANNER, "visibility", "visible"),
                hasArea(page, ShopControl.HOME_BANNER));
    }

    public ShopPageValidationResult filters(Page page) {
        return pages.validateControls(page, ShopHomePage.class,
                ShopControl.HOME_SORT,
                ShopControl.HOME_PRICE_RANGE,
                ShopControl.SEARCH_QUERY,
                ShopControl.HOME_SEARCH_RESET,
                ShopControl.HOME_SEARCH_SUBMIT,
                ShopControl.HOME_CATEGORY_FILTERS,
                ShopControl.HOME_BRAND_FILTERS,
                ShopControl.HOME_ECO_FILTER);
    }

    public List<AtfAssertion> filterIntegrity(Page page) {
        return List.of(
                countAtLeast(page, ShopControl.HOME_PRICE_RANGE, 2),
                countAtLeast(page, ShopControl.HOME_CATEGORY_FILTERS, 1),
                countAtLeast(page, ShopControl.HOME_BRAND_FILTERS, 1),
                style(page, ShopControl.HOME_SORT, "display", "block"),
                style(page, ShopControl.SEARCH_QUERY, "visibility", "visible"));
    }

    public ShopPageValidationResult productGrid(Page page) {
        return pages.validateControls(page, ShopHomePage.class,
                ShopControl.HOME_PRODUCT_GRID,
                ShopControl.HOME_PRODUCT_NAMES,
                ShopControl.HOME_PRODUCT_IMAGES,
                ShopControl.HOME_PAGINATION,
                ShopControl.HOME_FOOTER);
    }

    public List<AtfAssertion> productGridIntegrity(Page page) {
        int cards = count(page, ShopControl.HOME_PRODUCT_GRID);
        int names = count(page, ShopControl.HOME_PRODUCT_NAMES);
        int images = count(page, ShopControl.HOME_PRODUCT_IMAGES);
        return List.of(
                check("Product cards", "> 0", Integer.toString(cards), cards > 0),
                check("Product card names", Integer.toString(cards), Integer.toString(names), cards == names),
                check("Product card images", Integer.toString(cards), Integer.toString(images), cards == images),
                countAtLeast(page, ShopControl.HOME_PAGINATION, 1),
                style(page, ShopControl.HOME_PRODUCT_GRID, "display", "flex"),
                style(page, ShopControl.HOME_FOOTER, "display", "block"));
    }

    private AtfAssertion countAtLeast(Page page, ShopControl control, int minimum) {
        int actual = count(page, control);
        return check(control.label() + " count", ">= " + minimum,
                Integer.toString(actual), actual >= minimum);
    }

    private int count(Page page, ShopControl control) {
        try { return locators.control(page, control).locator().count(); }
        catch (RuntimeException error) { return 0; }
    }

    private AtfAssertion style(Page page, ShopControl control,
                               String property, String expected) {
        try {
            Object observed = locators.control(page, control).locator().first()
                    .evaluate("element => getComputedStyle(element).getPropertyValue('" + property + "')");
            String actual = observed == null ? "" : observed.toString().trim();
            return check("Style · " + control.label() + " · " + property,
                    expected, actual, expected.equals(actual));
        } catch (RuntimeException error) {
            return check("Style · " + control.label() + " · " + property,
                    expected, ThrowableDiagnostics.unavailable(error), false);
        }
    }

    private AtfAssertion hasArea(Page page, ShopControl control) {
        try {
            var box = locators.control(page, control).locator().first().boundingBox();
            boolean valid = box != null && box.width > 0 && box.height > 0;
            String actual = box == null ? "none" : Math.round(box.width) + "x" + Math.round(box.height);
            return check(control.label() + " render area", "> 0 x > 0", actual, valid);
        } catch (RuntimeException error) {
            return check(control.label() + " render area", "> 0 x > 0",
                    ThrowableDiagnostics.unavailable(error), false);
        }
    }
}

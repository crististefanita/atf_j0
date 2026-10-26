package atf.shop.support.validation;

import atf.api.core.diagnostics.ThrowableDiagnostics;
import atf.api.reporting.contract.AtfAssertion;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import atf.shop.support.ShopCountryOption;
import atf.shop.support.ShopPageContract;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Executes annotation-driven page contracts through the locator boundary. */
public final class ShopPageValidator {
    private final ShopLocators locators;

    public ShopPageValidator(ShopLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ShopPageValidationResult validate(Page page, Class<?> pageObjectType) {
        return validate(page, pageObjectType, null);
    }

    public ShopPageValidationResult validate(Page page, Class<?> pageObjectType,
                                                      String expectedText) {
        ShopPageContract contract = contract(pageObjectType);
        return validateControls(page, pageObjectType, expectedText, contract.requiredControls());
    }

    /** Validates one semantic slice of a page contract for a focused checkpoint. */
    public ShopPageValidationResult validateControls(Page page, Class<?> pageObjectType,
                                                             ShopControl... controls) {
        return validateControls(page, pageObjectType, null, controls);
    }

    private ShopPageValidationResult validateControls(Page page, Class<?> pageObjectType,
                                                              String expectedText,
                                                              ShopControl... controls) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(pageObjectType, "pageObjectType");
        ShopPageContract contract = contract(pageObjectType);

        List<AtfAssertion> assertions = new ArrayList<>();
        String actualPath = pathOf(page.url());
        assertions.add(assertion("Page route", contract.route(), actualPath,
                routeMatches(contract.route(), actualPath)));
        ShopControl[] selected = controls == null || controls.length == 0
                ? contract.requiredControls() : controls;
        for (ShopControl control : selected) assertions.add(visible(control, page));
        if (!contract.bodyMarker().isBlank()) {
            assertions.add(text(ShopControl.PAGE_BODY, page, contract.bodyMarker(), contract.bodyMarker()));
        }
        if (expectedText != null && !expectedText.isBlank()) {
            assertions.add(text(ShopControl.PAGE_BODY, page, expectedText,
                    "page contains configured value"));
        }
        return new ShopPageValidationResult(contract.screen(), assertions);
    }

    private static ShopPageContract contract(Class<?> pageObjectType) {
        ShopPageContract contract = pageObjectType.getAnnotation(ShopPageContract.class);
        if (contract == null) throw new IllegalArgumentException(pageObjectType.getName()
                + " must declare @ShopPageContract");
        return contract;
    }

    public ShopPageValidationResult validateValues(Page page, Class<?> pageObjectType,
                                                           Map<ShopControl, String> expectedValues) {
        ShopPageValidationResult base = validate(page, pageObjectType);
        List<AtfAssertion> assertions = new ArrayList<>(base.assertions());
        if (expectedValues != null) {
            expectedValues.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                    .forEach(entry -> assertions.add(value(entry.getKey(), page, entry.getValue())));
        }
        return new ShopPageValidationResult(base.screen(), assertions);
    }

    /** Reports an application-generated field value without pretending it was configured by the test. */
    public ShopPageValidationResult validatePopulatedValues(Page page, Class<?> pageObjectType,
                                                                     Map<ShopControl, String> expectedValues,
                                                                     ShopControl... generatedControls) {
        ShopPageValidationResult base = validate(page, pageObjectType);
        List<AtfAssertion> assertions = new ArrayList<>(base.assertions());
        if (expectedValues != null) {
            expectedValues.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                    .forEach(entry -> assertions.add(value(entry.getKey(), page, entry.getValue())));
        }
        if (generatedControls != null) {
            for (ShopControl control : generatedControls) {
                try {
                    String actual = locators.control(page, control).locator().inputValue();
                    boolean populated = actual != null && !actual.isBlank();
                    assertions.add(assertion(control.label(), "non-empty; auto-filled by Practice Shop",
                            compact(actual), populated));
                } catch (RuntimeException error) {
                    assertions.add(assertion(control.label(), "non-empty; auto-filled by Practice Shop",
                            ThrowableDiagnostics.unavailable(error), false));
                }
            }
        }
        return new ShopPageValidationResult(base.screen(), assertions);
    }

    /** Convenience entry point for a single form field in a component checkpoint. */
    public ShopPageValidationResult validateValue(Page page, Class<?> pageObjectType,
                                                          ShopControl control,
                                                          String expectedValue) {
        return validateValues(page, pageObjectType, Map.of(control, expectedValue));
    }

    /** Validates one semantic control without forcing an unrelated page contract. */
    public AtfAssertion validateVisible(Page page, ShopControl control) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(control, "control");
        return visible(control, page);
    }

    private AtfAssertion visible(ShopControl control, Page page) {
        try {
            var locator = locators.control(page, control).locator();
            int count = locator.count();
            boolean visible = count > 0 && locator.first().isVisible();
            String actual = count == 0 ? "not present" : (visible ? "visible" : "not visible");
            return assertion(control.label(), "visible", actual, visible);
        } catch (RuntimeException error) {
            return assertion(control.label(), "visible",
                    ThrowableDiagnostics.unavailable(error), false);
        }
    }

    private AtfAssertion text(ShopControl control, Page page, String expected, String name) {
        try {
            String actual = locators.control(page, control).locator().innerText();
            return assertion(name, expected, compact(actual), actual.contains(expected));
        } catch (RuntimeException error) {
            return assertion(name, expected,
                    ThrowableDiagnostics.unavailable(error), false);
        }
    }

    private AtfAssertion value(ShopControl control, Page page, String expected) {
        try {
            if (control == ShopControl.COUNTRY) {
                var selected = locators.selectedCountry(page).locator();
                var option = new ShopCountryOption(selected.getAttribute("value"),
                        selected.getAttribute("label"), selected.innerText());
                return assertion(control.label(), expected, option.displayValue(), option.matches(expected));
            }
            String actual = locators.control(page, control).locator().inputValue();
            return assertion(control.label(), expected, actual, expected.equals(actual));
        } catch (RuntimeException error) {
            return assertion(control.label(), expected,
                    ThrowableDiagnostics.unavailable(error), false);
        }
    }

    private static AtfAssertion assertion(String name, String expected, String actual, boolean passed) {
        return new AtfAssertion(name, expected, actual, passed);
    }

    private static String pathOf(String url) {
        try { return URI.create(url).getPath(); }
        catch (RuntimeException ignored) { return url == null ? "" : url; }
    }

    private static boolean routeMatches(String expected, String actual) {
        if (expected.endsWith("/*")) return actual.startsWith(expected.substring(0, expected.length() - 1));
        return expected.equals(actual);
    }

    private static String compact(String value) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 177) + "...";
    }
}

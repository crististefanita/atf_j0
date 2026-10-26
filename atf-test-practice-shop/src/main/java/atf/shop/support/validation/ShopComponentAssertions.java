package atf.shop.support.validation;

import atf.api.core.diagnostics.ThrowableDiagnostics;
import atf.api.reporting.contract.AtfAssertion;
import atf.shop.locators.ShopControl;
import atf.shop.locators.ShopLocators;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static atf.api.reporting.audit.ReportAuditChecks.check;

/**
 * Small, renderer-neutral component checks used by purchase-screen validators.
 * The home validator keeps its catalog-specific cardinality and style checks
 * because that screen is intentionally richer and split into three slices.
 * It deliberately reports one compact row per component instead of creating a
 * checkpoint for every control. The screenshot remains the complete screen
 * evidence and the assertion row explains its render geometry.
 */
public final class ShopComponentAssertions {
    private final ShopLocators locators;

    public ShopComponentAssertions(ShopLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public List<AtfAssertion> rendered(Page page, ShopControl... controls) {
        List<AtfAssertion> assertions = new ArrayList<>();
        if (controls == null) return assertions;
        for (ShopControl control : controls) assertions.add(rendered(page, control));
        return List.copyOf(assertions);
    }

    public AtfAssertion rendered(Page page, ShopControl control) {
        try {
            var locator = locators.control(page, control).locator();
            int count = locator.count();
            var box = count == 0 ? null : locator.first().boundingBox();
            boolean valid = count > 0 && box != null && box.width > 0 && box.height > 0;
            String area = box == null ? "none" : Math.round(box.width) + "x" + Math.round(box.height);
            return check("Rendered component · " + control.label(),
                    "count >= 1 and area > 0", "count=" + count + "; area=" + area, valid);
        } catch (RuntimeException error) {
            return check("Rendered component · " + control.label(),
                    "count >= 1 and area > 0", ThrowableDiagnostics.unavailable(error), false);
        }
    }

    public List<AtfAssertion> interactive(Page page, ShopControl... controls) {
        List<AtfAssertion> assertions = new ArrayList<>();
        if (controls == null) return assertions;
        for (ShopControl control : controls) assertions.add(interactive(page, control));
        return List.copyOf(assertions);
    }

    public AtfAssertion interactive(Page page, ShopControl control) {
        try {
            var locator = locators.control(page, control).locator();
            int count = locator.count();
            boolean enabled = count > 0 && locator.first().isEnabled();
            return check("Interactive component · " + control.label(),
                    "enabled", count == 0 ? "not present" : Boolean.toString(enabled), enabled);
        } catch (RuntimeException error) {
            return check("Interactive component · " + control.label(),
                    "enabled", ThrowableDiagnostics.unavailable(error), false);
        }
    }
}

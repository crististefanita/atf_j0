package atf.shop.support.validation;

import atf.api.reporting.contract.AtfAssertion;
import atf.shop.config.ShopScreen;

import java.util.List;

/** Immutable result of a page contract evaluation. */
public record ShopPageValidationResult(ShopScreen screen,
                                               List<AtfAssertion> assertions) {
    public ShopPageValidationResult {
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
    }

    public boolean passed() { return assertions.stream().allMatch(AtfAssertion::passed); }

    public String summary() {
        long passed = assertions.stream().filter(AtfAssertion::passed).count();
        return passed + "/" + assertions.size() + " validations passed for " + screen;
    }
}

package atf.shop.support.api;

import atf.reporting.contract.AtfAssertion;

import java.util.List;
import java.util.Objects;

/** Named business validation published beside the API exchanges it explains. */
public record ShopApiReportValidation(String title, List<AtfAssertion> assertions,
                                      String operationIntent) {
    public ShopApiReportValidation(String title, List<AtfAssertion> assertions) {
        this(title, assertions, "");
    }

    public ShopApiReportValidation {
        title = Objects.requireNonNull(title, "title").trim();
        if (title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        assertions = List.copyOf(Objects.requireNonNull(assertions, "assertions"));
        if (assertions.isEmpty()) throw new IllegalArgumentException("assertions must not be empty");
        operationIntent = Objects.requireNonNull(operationIntent, "operationIntent").trim();
    }

    public boolean passed() {
        return assertions.stream().allMatch(AtfAssertion::passed);
    }
}

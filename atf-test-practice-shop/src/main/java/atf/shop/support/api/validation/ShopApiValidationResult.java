package atf.shop.support.api.validation;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.reporting.ui.validation.UiApiValidationEvidence;

import java.util.List;
import java.util.Objects;

/** Explicit mapping from a Shop API checkpoint's contract checks to captured exchanges. */
public record ShopApiValidationResult(List<ExchangeChecks> exchanges,
                                      List<AtfAssertion> correlations) {
    public ShopApiValidationResult {
        exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
        correlations = correlations == null ? List.of() : List.copyOf(correlations);
    }

    /** Catalog contract order: search status/query; detail identity/headers; then trace correlations. */
    public static ShopApiValidationResult product(List<ReportExecutionEvidence> evidence,
                                                   List<AtfAssertion> checks) {
        if (evidence != null && evidence.isEmpty() && checks != null && checks.isEmpty()) {
            return new ShopApiValidationResult(List.of(), List.of());
        }
        requireSize(evidence, 2, "product exchanges");
        requireSize(checks, 6, "product assertions");
        return new ShopApiValidationResult(List.of(
                group(evidence.get(0), checks.subList(0, 2)),
                group(evidence.get(1), checks.subList(2, 4))), checks.subList(4, 6));
    }

    /** Cart contract order: add request/response; read response; then trace correlations. */
    public static ShopApiValidationResult cart(List<ReportExecutionEvidence> evidence,
                                                List<AtfAssertion> checks) {
        if (evidence != null && evidence.isEmpty() && checks != null && checks.isEmpty()) {
            return new ShopApiValidationResult(List.of(), List.of());
        }
        requireSize(evidence, 2, "cart exchanges");
        requireSize(checks, 5, "cart assertions");
        return new ShopApiValidationResult(List.of(
                group(evidence.get(0), checks.subList(0, 2)),
                group(evidence.get(1), checks.subList(2, 3))), checks.subList(3, 5));
    }

    /** Payment request, response and content type all describe this one exchange. */
    public static ShopApiValidationResult payment(List<ReportExecutionEvidence> evidence,
                                                   List<AtfAssertion> checks) {
        if (evidence != null && evidence.isEmpty() && checks != null && checks.isEmpty()) {
            return new ShopApiValidationResult(List.of(), List.of());
        }
        requireSize(evidence, 1, "payment exchange");
        requireSize(checks, 4, "payment assertions");
        return new ShopApiValidationResult(List.of(group(evidence.getFirst(), checks.subList(0, 4))),
                List.of());
    }

    /** Order contract is bound to the created exchange; UI invoice matching is a cross-channel correlation. */
    public static ShopApiValidationResult order(List<ReportExecutionEvidence> evidence,
                                                 List<AtfAssertion> checks) {
        if (evidence != null && evidence.isEmpty() && checks != null && checks.isEmpty()) {
            return new ShopApiValidationResult(List.of(), List.of());
        }
        requireSize(evidence, 1, "order exchange");
        requireSize(checks, 5, "order assertions");
        return new ShopApiValidationResult(List.of(group(evidence.getFirst(),
                List.of(checks.get(0), checks.get(1), checks.get(3), checks.get(4)))),
                List.of(checks.get(2)));
    }

    public UiApiValidationEvidence reportingEvidence() {
        return new UiApiValidationEvidence(correlations, exchanges.stream()
                .map(exchange -> new UiApiValidationEvidence.Exchange(exchange.evidence(),
                        exchange.assertions()))
                .toList());
    }

    public List<AtfAssertion> allAssertions() {
        return java.util.stream.Stream.concat(exchanges.stream()
                        .flatMap(exchange -> exchange.assertions().stream()), correlations.stream())
                .toList();
    }

    private static ExchangeChecks group(ReportExecutionEvidence evidence, List<AtfAssertion> checks) {
        return new ExchangeChecks(evidence, checks);
    }

    private static void requireSize(List<?> values, int expected, String label) {
        Objects.requireNonNull(values, label);
        if (values.size() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " " + label + ", got " + values.size());
        }
    }

    public record ExchangeChecks(ReportExecutionEvidence evidence, List<AtfAssertion> assertions) {
        public ExchangeChecks {
            Objects.requireNonNull(evidence, "evidence");
            assertions = assertions == null ? List.of() : List.copyOf(assertions);
        }
    }
}

package atf.shop.support;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfGherkinKeyword;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.reporting.ui.validation.UiApiValidationEvidence;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/** Immutable page-level evidence contract emitted by the purchase flow. */
public record ShopPurchaseCheckpoint(
        AtfGherkinKeyword keyword,
        String title,
        Collection<AtfAssertion> uiAssertions,
        UiApiValidationEvidence apiValidation) {

    public ShopPurchaseCheckpoint(AtfGherkinKeyword keyword, String title,
                                          Collection<AtfAssertion> assertions) {
        this(keyword, title, assertions, new UiApiValidationEvidence(List.of(), List.of()));
    }

    public ShopPurchaseCheckpoint(AtfGherkinKeyword keyword, String title,
                                  Collection<AtfAssertion> uiAssertions,
                                  Collection<ReportExecutionEvidence> apiEvidence) {
        this(keyword, title, uiAssertions, new UiApiValidationEvidence(List.of(),
                apiEvidence == null ? List.of() : apiEvidence.stream()
                        .map(evidence -> new UiApiValidationEvidence.Exchange(evidence, List.of()))
                        .toList()));
    }

    public ShopPurchaseCheckpoint {
        keyword = keyword == null ? AtfGherkinKeyword.THEN : keyword;
        title = title == null || title.isBlank() ? "Practice Shop page is valid" : title.trim();
        uiAssertions = uiAssertions == null ? List.of() : List.copyOf(uiAssertions);
        apiValidation = apiValidation == null
                ? new UiApiValidationEvidence(List.of(), List.of()) : apiValidation;
    }

    /** UI-owned assertions only; API checks are carried by {@link #apiValidation()}. */
    public Collection<AtfAssertion> assertions() { return uiAssertions; }

    public Collection<ReportExecutionEvidence> apiEvidence() {
        return apiValidation.exchanges().stream()
                .map(UiApiValidationEvidence.Exchange::evidence).toList();
    }

    public ShopPurchaseCheckpoint withAdditionalAssertions(Collection<AtfAssertion> additional) {
        List<AtfAssertion> merged = new ArrayList<>(uiAssertions);
        if (additional != null) merged.addAll(additional);
        return new ShopPurchaseCheckpoint(keyword, title, merged, apiValidation);
    }

    public ShopPurchaseCheckpoint withApiEvidence(
            Collection<ReportExecutionEvidence> additional) {
        List<UiApiValidationEvidence.Exchange> merged = new ArrayList<>(apiValidation.exchanges());
        if (additional != null) additional.stream().map(evidence ->
                new UiApiValidationEvidence.Exchange(evidence, List.of())).forEach(merged::add);
        return new ShopPurchaseCheckpoint(keyword, title, uiAssertions,
                new UiApiValidationEvidence(apiValidation.assertions(), merged));
    }

    public ShopPurchaseCheckpoint withApiValidation(UiApiValidationEvidence validation) {
        return new ShopPurchaseCheckpoint(keyword, title, uiAssertions,
                validation == null ? new UiApiValidationEvidence(List.of(), List.of()) : validation);
    }

    public boolean passed() {
        return uiAssertions.stream().allMatch(AtfAssertion::passed)
                && apiValidation.status() != atf.api.reporting.contract.AtfReportStatus.FAIL;
    }
}

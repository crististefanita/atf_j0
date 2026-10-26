package atf.reporting.ui.contract;

import java.util.List;
import java.util.Objects;

/** Immutable browser-neutral facts observed for one rendered report tab. */
public record UiReportTabSnapshot(
        ReportUiContract.Tab tab,
        boolean tabVisible,
        boolean hasRenderedDimensions,
        int navigationTabCount,
        int ownerCount,
        int scenarioCount,
        int phaseCount,
        int validationCount,
        int setupValidationCount,
        int categoryItemCount,
        boolean categoryListPresent,
        int failureItemCount,
        boolean failureEmptyState,
        int openDisclosureCount,
        int uiValidationNodeCount,
        boolean styleValid,
        List<String> styleViolations,
        boolean uiValidationValid,
        List<String> uiValidationViolations) {
    public UiReportTabSnapshot {
        tab = Objects.requireNonNull(tab, "tab");
        if (navigationTabCount < 0 || ownerCount < 0 || scenarioCount < 0
                || phaseCount < 0 || validationCount < 0 || setupValidationCount < 0
                || categoryItemCount < 0 || failureItemCount < 0 || openDisclosureCount < 0
                || uiValidationNodeCount < 0) {
            throw new IllegalArgumentException("UI report snapshot counts must be non-negative");
        }
        styleViolations = List.copyOf(styleViolations == null ? List.of() : styleViolations);
        uiValidationViolations = List.copyOf(
                uiValidationViolations == null ? List.of() : uiValidationViolations);
    }
}

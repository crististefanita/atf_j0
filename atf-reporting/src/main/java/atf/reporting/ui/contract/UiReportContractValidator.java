package atf.reporting.ui.contract;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure verdict for one report tab. Browser adapters only build the snapshot;
 * this validator owns no Playwright, DOM extraction or test fixture state.
 */
public final class UiReportContractValidator {
    public ValidationResult validate(UiReportTabSnapshot snapshot,
                                     UiReportContractSpecView spec) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(spec, "spec");
        List<AtfAssertion> assertions = new ArrayList<>();
        if (spec.has(UiReportCheck.STRUCTURE)) {
            assertions.add(ReportAuditChecks.check("Tab visible", "visible",
                    String.valueOf(snapshot.tabVisible()), snapshot.tabVisible()));
            assertions.add(ReportAuditChecks.check("Tab dimensions", "positive",
                    String.valueOf(snapshot.hasRenderedDimensions()), snapshot.hasRenderedDimensions()));
            assertions.add(ReportAuditChecks.check("Navigation tabs", "4",
                    String.valueOf(snapshot.navigationTabCount()), snapshot.navigationTabCount() == 4));
            switch (snapshot.tab()) {
                case OVERVIEW -> assertions.add(ReportAuditChecks.check("Overview surface", "> 0 cards",
                        "rendered", snapshot.hasRenderedDimensions()));
                case TESTS -> {
                    assertions.add(ReportAuditChecks.check("UI test owners", "> 0",
                            String.valueOf(snapshot.ownerCount()), snapshot.ownerCount() > 0));
                    assertions.add(ReportAuditChecks.check("UI phases", "> 0",
                            String.valueOf(snapshot.phaseCount()), snapshot.phaseCount() > 0));
                    assertions.add(ReportAuditChecks.check("UI validation evidence", "> 0",
                            String.valueOf(snapshot.validationCount()), snapshot.validationCount() > 0));
                }
                case CATEGORIES -> assertions.add(ReportAuditChecks.check("Category surface", "present",
                        String.valueOf(snapshot.categoryListPresent()), snapshot.categoryListPresent()));
                case FAILS -> assertions.add(ReportAuditChecks.check("Fails surface", "empty or actionable",
                        snapshot.failureItemCount() + " rows / empty=" + snapshot.failureEmptyState(),
                        snapshot.failureItemCount() > 0 || snapshot.failureEmptyState()));
            }
        }
        if (spec.has(UiReportCheck.STYLE)) {
            assertions.add(ReportAuditChecks.check("Rendered style", "no violations",
                    snapshot.styleViolations().isEmpty() ? "valid" : snapshot.styleViolations().toString(),
                    snapshot.styleValid()));
        }
        if (spec.has(UiReportCheck.SETUP_EVIDENCE)
                && snapshot.tab() == ReportUiContract.Tab.TESTS) {
            assertions.add(ReportAuditChecks.check("Setup evidence", "0 UI validation cards",
                    String.valueOf(snapshot.setupValidationCount()),
                    snapshot.setupValidationCount() == 0));
        }
        if (spec.has(UiReportCheck.COLLAPSED_BY_DEFAULT)) {
            assertions.add(ReportAuditChecks.check("Collapsed by default", "0 open disclosures",
                    String.valueOf(snapshot.openDisclosureCount()), snapshot.openDisclosureCount() == 0));
        }
        if (spec.has(UiReportCheck.CROSS_TAB)) {
            assertions.add(ReportAuditChecks.check("Cross-tab navigation", "all four tabs",
                    String.valueOf(snapshot.navigationTabCount()), snapshot.navigationTabCount() == 4));
        }
        if ((spec.has(UiReportCheck.SCREEN_VALIDATION)
                || spec.has(UiReportCheck.INLINE_SCREENSHOT))
                && snapshot.tab() == ReportUiContract.Tab.TESTS) {
            assertions.add(ReportAuditChecks.check("UI validation", "valid evidence contract",
                    snapshot.screenValidationViolations().isEmpty()
                            ? "valid" : snapshot.screenValidationViolations().toString(),
                    snapshot.screenValidationValid()));
        }
        if (spec.has(UiReportCheck.FAILS_PROJECTION)
                && snapshot.tab() == ReportUiContract.Tab.FAILS) {
            assertions.add(ReportAuditChecks.check("Fails projection", "empty state or failure rows",
                    snapshot.failureItemCount() + " / " + snapshot.failureEmptyState(),
                    snapshot.failureItemCount() > 0 || snapshot.failureEmptyState()));
        }
        return ValidationResult.of(assertions);
    }

}

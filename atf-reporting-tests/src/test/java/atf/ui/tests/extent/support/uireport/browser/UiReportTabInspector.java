package atf.ui.tests.extent.support.uireport.browser;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.contract.UiReportCheck;
import atf.reporting.ui.contract.UiReportTabSnapshot;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.ReportAuditSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/** Browser adapter for the UI-report contract; selectors stay in the facade. */
public final class UiReportTabInspector {
    private final UiReportContractFixture fixture;
    private final ReportUiLocators locators;
    private final UiReportStyleValidator styles;

    public UiReportTabInspector(UiReportContractFixture fixture) {
        this.fixture = Objects.requireNonNull(fixture, "fixture");
        this.locators = fixture.locators();
        this.styles = new UiReportStyleValidator(locators);
    }

    public UiReportTabSnapshot inspect(ReportUiContract.Tab tab) {
        Page page = fixture.openTab(tab);
        Locator view = locators.tabView(page, tab);
        boolean visible = view.count() == 1 && view.isVisible();
        var box = visible ? view.boundingBox() : null;
        int openDisclosures = locators.openDisclosures(view).count();
        int navigationTabs = 0;
        for (ReportUiContract.Tab candidate : ReportUiContract.Tab.values()) {
            if (locators.navTab(page, candidate).count() == 1) navigationTabs++;
        }

        int owners = 0;
        int scenarios = 0;
        int phases = 0;
        int validations = 0;
        int setupValidations = 0;
        int screenNodes = 0;
        int categories = 0;
        boolean categoryList = false;
        int failures = 0;
        boolean emptyFailures = false;
        boolean screenValid = true;
        List<String> screenViolations = List.of();

        if (tab == ReportUiContract.Tab.TESTS) {
            owners = locators.testOwners(view).count();
            if (owners > 0) {
                Locator detail = fixture.navigation().openFirstOwner(page);
                if (detail.count() > 0 && detail.isVisible()) {
                    fixture.navigation().expandTestStructure(detail);
                    scenarios = locators.scenarios(detail).count();
                    phases = locators.phases(detail).count();
                    validations = locators.validationTables(detail).count();
                    Locator setup = locators.visiblePhase(detail,
                            ReportUiContract.Phase.SETUP);
                    setupValidations = setup.count() == 0 ? 0 : locators.validationNodes(setup).count();
                }
            }
            screenNodes = locators.screenValidationNodes(page).count();
            if (fixture.spec().has(UiReportCheck.SCREEN_VALIDATION)
                    || fixture.spec().has(UiReportCheck.INLINE_SCREENSHOT)) {
                ReportAuditSnapshot.ScreenValidation screen = fixture.screenValidation().inspect(page);
                screenValid = screen.inlineScreenshot() && screen.zonesAreDirectSiblings()
                        && screen.zonesStacked() && screen.noDecorativeSeparators();
                screenViolations = screen.violations();
            }
        } else if (tab == ReportUiContract.Tab.CATEGORIES) {
            categoryList = locators.categoryList(page).count() == 1;
            categories = locators.categoryItems(page).count();
        } else if (tab == ReportUiContract.Tab.FAILS) {
            failures = locators.failureItems(view).count();
            emptyFailures = locators.failureEmptyState(view).count() > 0;
        }

        UiReportStyleValidator.Result style = styles.inspect(page, tab);
        List<String> renderViolations = new ArrayList<>(style.violations());
        renderViolations.addAll(fixture.context().browserDiagnostics().violations());
        return new UiReportTabSnapshot(tab, visible, box != null && box.width > 0 && box.height > 0,
                navigationTabs, owners, scenarios, phases, validations, setupValidations,
                categories, categoryList,
                failures, emptyFailures, openDisclosures, screenNodes, renderViolations.isEmpty(),
                renderViolations, screenValid, screenViolations);
    }
}

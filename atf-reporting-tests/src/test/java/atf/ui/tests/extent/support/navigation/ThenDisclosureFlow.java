package atf.ui.tests.extent.support.navigation;
import atf.reporting.ui.contract.ReportUiContract;

import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Browser flow for the optional TEARDOWN/THEN disclosure contract. */
public final class ThenDisclosureFlow {
    private static final String THEN_TITLE =
            "THEN the existing report audit completed and published a separate UI artifact";
    private static final String UI_VALIDATION_TITLE = "UI validation";
    private final ReportUiLocators locators;

    public ThenDisclosureFlow() {
        this(new ReportUiLocators());
    }

    public ThenDisclosureFlow(ReportUiLocators locators) {
        this.locators = locators;
    }

    public ThenSnapshot inspectThenDisclosure(Page page) {
        openFirstOwner(page);
        Locator detail = visibleDetail(page);
        Locator thenNode = locators.nodeTitleByText(detail, THEN_TITLE);
        Locator thenCard = locators.cardForNode(thenNode);
        Locator thenHeader = locators.nodeHeader(thenCard);
        String thenStyle = headerStyle(thenHeader);
        String renderedText = detail.textContent();
        int nestedUiValidations = locators.nodeTitleByText(thenCard, UI_VALIDATION_TITLE).count();
        return new ThenSnapshot(thenNode.count() > 0, thenCard.getAttribute("data-atf-node-role"),
                thenCard.getAttribute("data-atf-node-open") != null, thenStyle.endsWith("|true"),
                nestedUiValidations, renderedText.contains(THEN_TITLE));
    }

    public PhaseSnapshot inspectWithoutTeardown(Page page) {
        openFirstOwner(page);
        Locator detail = visibleDetail(page);
        Locator phases = locators.phases(detail);
        String renderedText = detail.textContent();
        return new PhaseSnapshot(phases.count(), renderedText.contains(ReportUiContract.Phase.SETUP.displayName()),
                renderedText.contains(ReportUiContract.Phase.TEST_BODY.displayName()),
                renderedText.contains(ReportUiContract.Phase.TEARDOWN.displayName()));
    }

    private void openFirstOwner(Page page) {
        locators.testsView(page).waitFor();
        locators.firstTestOwnerAction(page).locator().click();
    }

    private Locator visibleDetail(Page page) {
        Locator detail = locators.visibleTestDetail(page);
        detail.waitFor();
        return detail;
    }

    private String headerStyle(Locator header) {
        if (header.count() == 0) return "missing";
        return header.evaluate("element => { const s=getComputedStyle(element), p=getComputedStyle(element, '::before'); "
                + "return [s.backgroundColor,s.borderColor,s.borderRadius,"
                + "p.content !== 'none' && p.content !== 'normal' && p.content !== ''].join('|'); }").toString();
    }

    public record ThenSnapshot(boolean rendered, String role, boolean initialized,
                               boolean disclosureGlyph, int nestedUiValidations,
                               boolean titleVisible) { }

    public record PhaseSnapshot(int phaseCount, boolean setupVisible,
                                boolean testBodyVisible, boolean teardownVisible) { }
}

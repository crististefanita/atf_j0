package atf.ui.tests.extent.support.api;
import atf.ui.tests.extent.support.audit.api.ApiReportContractValidation;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** UI navigation and disclosure actions for an API Extent artifact. */
public final class ApiReportUiFlow {
    private final UiAuditFlow audit;
    private final ReportUiLocators locators;

    public ApiReportUiFlow(UiAuditFlow audit) {
        this(audit, new ReportUiLocators());
    }

    public ApiReportUiFlow(UiAuditFlow audit, ReportUiLocators locators) {
        this.audit = Objects.requireNonNull(audit, "audit");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ReportUiLocators locators() {
        return locators;
    }

    public Page openTests() {
        return audit.openTab(ReportUiContract.Tab.TESTS.hash());
    }

    /**
     * Records the tab invariant at the boundary of every owner audit after
     * the first one.  The first owner inherits the explicit checkpoint from
     * {@link #openTests()}; later owner validations must remain independently
     * repeatable from their own UI validation evidence.
     */
    public void confirmTestsTabVisible(Page page, String ownerName) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(ownerName, "ownerName");
        Locator tests = locators.testsView(page);
        if (!tests.isVisible()) {
            throw new IllegalStateException("Tests tab is not visible before auditing owner: " + ownerName);
        }
        audit.actions().observation("Tab visible", ReportUiContract.Tab.TESTS.hash(),
                "selected Tests tab remains visible before owner audit",
                ReportUiContract.Tab.TESTS.hash() + " / " + ownerName);
    }

    /** Returns every API owner rendered in the current Tests view. */
    public List<String> ownerNames(Page page) {
        Objects.requireNonNull(page, "page");
        List<String> result = new ArrayList<>();
        Locator owners = locators.testOwners(locators.testsView(page));
        for (int index = 0; index < owners.count(); index++) {
            Locator owner = owners.nth(index);
            String name = audit.actions().getText(locators.testOwnerNameAction(owner),
                    "owner name is readable from the Tests tab");
            if (!name.isBlank()) result.add(name);
        }
        return List.copyOf(result);
    }

    public Owner openOwner(Page page, String ownerName) {
        Objects.requireNonNull(page, "page");
        Locator tests = locators.testsView(page);
        Locator owner = locators.testOwnerItemByName(tests, ownerName);
        PlaywrightActionSequence actions = audit.actions();
        String observedOwner = actions.getText(locators.testOwnerNameAction(owner),
                "selected owner name equals setup-selected API family");
        if (!ownerName.equals(observedOwner)) {
            throw new AssertionError("Expected owner '" + ownerName + "' but Tests tab rendered '"
                    + observedOwner + "'");
        }
        actions.click(locators.testOwnerItemAction(page, ownerName),
                "owner detail for '" + ownerName + "' is opened");
        // Extent keeps a copy of the owner content inside the hidden owner
        // card and projects the selected copy into a page-level detail panel.
        // The hidden copy is structurally complete but is not the DOM surface
        // the user can inspect. Always bind the audit to that selected panel.
        Locator detail = locators.visibleTestDetail(page);
        actions.waitForVisible(detail, ".test-content-detail:visible");
        actions.waitForVisible(locators.scenarios(detail).first(),
                ".test-content-detail:visible [data-atf-scenario-id]");
        return new Owner(ownerName, detail, page);
    }

    /**
     * Expands every structural card in scenario order. Scenario panels are
     * mutually exclusive in the report runtime, so the traversal deliberately
     * re-opens each scenario instead of relying on one global visible subtree.
     */
    public void expandAll(Locator detail) {
        Locator scenarios = locators.scenarios(detail);
        for (int scenarioIndex = 0; scenarioIndex < scenarios.count(); scenarioIndex++) {
            Locator scenario = scenarios.nth(scenarioIndex);
            if (!scenario.isVisible()) continue;
            expandScenario(scenario);
        }
    }

    /** Expands one selected scenario without changing its siblings' disclosure state. */
    public void expandScenario(Locator scenario) {
        expandCard(scenario);
        expandScenarioChildren(scenario);
    }

    public void expandRequestEvidence(Locator request) {
        if (request.count() > 0 && request.isVisible()
                && !"true".equals(request.getAttribute("data-atf-node-open"))) {
            audit.actions().click(locators.requestNodeToggleAction(request),
                    "expand request '" + locators.requestTitle(request) + "'");
        }
        expand(locators.requestEvidenceDisclosure(request),
                locators.requestEvidenceDisclosureAction(request),
                "open request evidence for '" + locators.requestTitle(request) + "'");
        expand(locators.responseEvidenceDisclosure(request),
                locators.responseEvidenceDisclosureAction(request),
                "open response evidence for '" + locators.requestTitle(request) + "'");
        expand(locators.executionEvidenceDisclosure(request),
                locators.executionEvidenceDisclosureAction(request),
                "open execution evidence for '" + locators.requestTitle(request) + "'");
    }

    public int expandOwnerRequests(Owner owner) {
        int count = 0;
        Locator scenarios = locators.scenarios(owner.detail());
        for (int scenarioIndex = 0; scenarioIndex < scenarios.count(); scenarioIndex++) {
            Locator scenario = scenarios.nth(scenarioIndex);
            if (!scenario.isVisible()) continue;
            expandCard(scenario);
            Locator phases = locators.visiblePhases(scenario);
            for (int phaseIndex = 0; phaseIndex < phases.count(); phaseIndex++) {
                Locator phase = phases.nth(phaseIndex);
                if (!phase.isVisible()) continue;
                expandCard(phase);
                Locator requests = locators.visibleRequestsInPhase(phase);
                for (int requestIndex = 0; requestIndex < requests.count(); requestIndex++) {
                    Locator request = requests.nth(requestIndex);
                    if (!request.isVisible()) continue;
                    audit.actions().getText(locators.requestNodeToggleAction(request),
                            "request method and path are readable before expansion");
                    expandCard(request);
                    expandValidationCards(request);
                    expandRequestEvidence(request);
                    expandRequestChildren(request);
                    count++;
                }
            }
        }
        return count;
    }

    /** Publishes the semantic audit trail without bloating the assertion summary. */
    public void recordOwnerContractAudit(Owner owner, int expandedRequests,
                                         ApiReportContractValidation.Result result) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(result, "result");
        int scenarios = locators.scenarios(owner.detail()).count();
        int phases = locators.phases(owner.detail()).count();
        audit.actions().observation("Owner selected",
                ReportUiContract.Tab.TESTS.hash() + " / " + owner.name(),
                "owner detail → scenario → TEST BODY is visible",
                "scenarios=" + scenarios + ", phases=" + phases + ", owner=" + owner.name());
        audit.actions().observation("Request coverage",
                owner.name() + " / TEST BODY / request cards",
                "expanded request count equals inspected request count",
                "expanded=" + expandedRequests + ", inspected=" + result.requestCount());
        audit.actions().observation("Routes audited",
                owner.name() + " / METHOD + path",
                "every rendered request is traceable to one mock-backed API route",
                result.routeSummary());
        audit.actions().observation("Mock contract",
                owner.name() + " / status + headers + auth + query + JSON",
                "mock YAML and rendered evidence reconcile without violations",
                result.requestCount() + " requests; "
                        + (result.violations().isEmpty() ? "no violations" : result.violations()));
        audit.actions().observation("Evidence channels",
                owner.name() + " / request + response + execution evidence",
                "each request is replayable from all three evidence disclosures",
                "request cards expanded=" + expandedRequests);
    }

    private void expandScenarioChildren(Locator scenario) {
        Locator phases = locators.visiblePhases(scenario);
        for (int phaseIndex = 0; phaseIndex < phases.count(); phaseIndex++) {
            Locator phase = phases.nth(phaseIndex);
            if (!phase.isVisible()) continue;
            expandCard(phase);
            Locator requests = locators.visibleRequestsInPhase(phase);
            for (int requestIndex = 0; requestIndex < requests.count(); requestIndex++) {
                Locator request = requests.nth(requestIndex);
                if (!request.isVisible()) continue;
                expandCard(request);
                expandValidationCards(request);
            }
        }
    }

    private void expandValidationCards(Locator request) {
        Locator validations = locators.validationCards(request);
        for (int index = 0; index < validations.count(); index++) {
            Locator validation = validations.nth(index);
            if (validation.isVisible()) expandCard(validation);
        }
    }

    /** Opens renderer-owned child nodes such as the request's Network aggregate card. */
    private void expandRequestChildren(Locator request) {
        Locator headers = locators.nodeHeaders(request);
        for (int index = 0; index < headers.count(); index++) {
            Locator header = headers.nth(index);
            if (!header.isVisible()) continue;
            Locator child = locators.cardForNode(header);
            if (child.isVisible()) expandCard(child);
        }
    }

    private boolean expandCard(Locator card) {
        Locator header = locators.nodeHeader(card);
        if (header.count() == 0 || !header.isVisible()
                || "true".equals(header.getAttribute("aria-expanded"))) return false;
        // Visibility was already established above.  Do not publish a prose
        // description as a Playwright action target: replay evidence must
        // always contain the effective locator or a semantic observation.
        audit.actions().observation("Card header visible", locators.nodeDescription(header),
                "structural card header is visible before expansion", "visible=true");
        audit.actions().focus(locators.nodeHeaderAction(header),
                "card header is focused before expansion");
        audit.actions().click(locators.nodeHeaderAction(header),
                "expand " + locators.nodeDescription(header));
        audit.actions().waitForAttribute(ownerPage(card), locators.nodeCardAction(card),
                locators.nodeOpenAttribute(card), "true",
                locators.nodeDescription(header) + " is fully expanded");
        return true;
    }

    private Page ownerPage(Locator card) {
        return card.page();
    }

    private void expand(Locator control, AtfLocator action, String expected) {
        if (control.count() == 0) return;
        Locator firstControl = control.first();
        var firstAction = new AtfLocator(action.locator().first(),
                action.selector() + " >> nth=0");
        if (!firstControl.isVisible()) return;
        if (!"true".equals(firstControl.getAttribute("aria-expanded"))) {
            audit.actions().focus(firstAction, "disclosure is focused before opening");
            audit.actions().click(firstAction, expected);
            audit.actions().waitForDisclosureOpen(firstAction.locator().page(), firstAction,
                    expected + " is ready");
        }
    }

    public record Owner(String name, Locator detail, Page page) {
        public Owner {
            name = Objects.requireNonNull(name, "name");
            detail = Objects.requireNonNull(detail, "detail");
            page = Objects.requireNonNull(page, "page");
        }
    }
}

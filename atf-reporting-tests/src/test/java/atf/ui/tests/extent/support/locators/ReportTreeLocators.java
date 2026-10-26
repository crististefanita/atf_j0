package atf.ui.tests.extent.support.locators;

import atf.reporting.theme.tokens.AtfReportCssSelectors;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Locators for test owners, filters and the hierarchical report tree. */
final class ReportTreeLocators {
    private static final String TESTS_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.TEST_VIEW);
    private static final String TEST_LIST = AtfReportCssSelectors.Component.TEST_LIST.attributeSelector();
    private static final String TEST_CONTENT_DETAIL =
            AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector();
    private static final String TEST_ITEM = AtfReportCssSelectors.Component.TEST_ITEM.attributeSelector();
    private static final String TEST_LIST_TOOLS =
            AtfReportCssSelectors.Component.TEST_LIST_TOOLS.attributeSelector();
    private static final String TEST_DETAIL = AtfReportCssSelectors.Component.TEST_DETAIL.attributeSelector();
    private static final String TEST_OWNERS = ReportUiContract.TEST_OWNER_SELECTOR;
    private static final String VISIBLE_TEST_DETAIL = ReportLocatorSupport.VISIBLE_TEST_DETAIL;
    private static final String STATUS_TOGGLE = ReportLocatorSupport.idSelector("status-toggle");
    private static final String TAG_TOGGLE = ReportLocatorSupport.idSelector("tag-toggle");
    private static final String TAG_FILTER = "[data-atf-filter-bound='true']";
    private static final String PHASE_NODE = "[data-atf-node-role='phase']";
    private static final String FAILURE_BADGE = "[class~='card-toolbar'] [class~='badge-danger'], "
            + "[class~='card-toolbar'] [class~='fail-bg']";
    private static final String PHASE_DIRECT_BODY = ":scope > [class~='card-body']";
    private static final String PHASE_FAILURE_DISCLOSURE = "details[class~='failure-evidence']";
    private static final String PHASE_FAILURE_TRACE = PHASE_FAILURE_DISCLOSURE
            + " [class~='helm-trace-table']";
    private static final String PHASE_FAILURE_CAUSE = PHASE_FAILURE_TRACE + " tbody tr";
    private static final String CLOSED_EVIDENCE_BODY = "details[class~='step-detail']:not([open]) > :not(summary):visible,"
            + "details[class~='atf-ui-action-sequence']:not([open]) > :not(summary):visible,"
            + "details[class~='atf-ui-observations']:not([open]) > :not(summary):visible";
    private static final String SEMANTIC_EVIDENCE_DISCLOSURE = "details[class~='step-detail'][class~='request-evidence'],"
            + "details[class~='step-detail'][class~='response-evidence'],"
            + "details[class~='step-detail'][class~='execution-evidence'],"
            + "details[class~='step-detail'][class~='assertion-evidence']";
    private static final String REQUEST_NODE =
            "[data-atf-node-role='request']:has(> [class~='collapse'] [class~='execution-table'])";

    private final ReportPageLocators pages;

    ReportTreeLocators(ReportPageLocators pages) {
        this.pages = pages;
    }

    Locator testOwners(Page page) { return pages.testsView(page).locator(TEST_OWNERS); }
    Locator testOwners(Locator tests) { return tests.locator(TEST_OWNERS); }
    Locator testOwnerNames(Locator tests) {
        return tests.locator(TEST_LIST + " " + TEST_ITEM + " " + classToken("name"));
    }
    Locator testOwnerName(Locator owner) { return owner.locator(TEST_DETAIL + " " + classToken("name")); }
    Locator testOwnerByName(Locator tests, String className) {
        return testOwnerNames(tests).filter(new Locator.FilterOptions().setHasText(className)).first();
    }
    Locator testOwnerItemByName(Locator tests, String className) {
        return testOwners(tests).filter(new Locator.FilterOptions().setHasText(className)).first();
    }
    Locator ownerStatusBadge(Locator owner) {
        return owner.locator(":scope > " + classToken("test-detail") + " "
                + classToken("badge") + classToken("log")).first();
    }
    Locator nodeStatusBadge(Locator card) {
        return card.locator(":scope > " + classToken("card-header") + " > "
                + classToken("card-toolbar") + " " + classToken("badge") + classToken("log")).first();
    }
    Locator testOwnerItem(Page page, int zeroBasedIndex) {
        return testOwners(pages.testsView(page)).nth(ReportLocatorSupport.requireIndex(zeroBasedIndex));
    }
    AtfLocator testOwnerItemAction(Page page, int zeroBasedIndex) {
        int index = ReportLocatorSupport.requireIndex(zeroBasedIndex);
        return new AtfLocator(testOwnerItem(page, index),
                TESTS_VIEW + " " + TEST_OWNERS + ":nth-of-type(" + (index + 1) + ")");
    }
    AtfLocator firstTestOwnerAction(Page page) { return testOwnerItemAction(page, 0); }
    AtfLocator testOwnerNameAction(Page page, int zeroBasedIndex) {
        int index = ReportLocatorSupport.requireIndex(zeroBasedIndex);
        return new AtfLocator(testOwnerName(testOwnerItem(page, index)),
                TESTS_VIEW + " " + TEST_OWNERS + ":nth-of-type(" + (index + 1) + ") "
                        + classToken("test-detail") + " " + classToken("name"));
    }
    AtfLocator testOwnerNameAction(Locator owner) {
        String testId = ReportLocatorSupport.requiredAttribute(owner, "test-id", "API owner");
        return new AtfLocator(testOwnerName(owner),
                TESTS_VIEW + " " + TEST_OWNERS + "[test-id='"
                        + ReportLocatorSupport.cssAttribute(testId) + "'] " + classToken("test-detail")
                        + " " + classToken("name"));
    }
    AtfLocator testOwnerItemAction(Page page, String className) {
        Locator owner = testOwnerItemByName(pages.testsView(page), className);
        String testId = ReportLocatorSupport.requiredAttribute(owner, "test-id", "API owner " + className);
        return new AtfLocator(owner,
                TESTS_VIEW + " " + TEST_LIST + " " + TEST_ITEM + "[test-id='"
                        + ReportLocatorSupport.cssAttribute(testId) + "']");
    }
    Locator visibleTestOwners(Locator tests) {
        return tests.locator(AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector() + ":visible"
                + " " + TEST_LIST + " " + TEST_ITEM);
    }

    Locator testStatusMenu(Locator tests) { return tests.locator(STATUS_TOGGLE); }
    Locator testTagMenu(Locator tests) { return tests.locator(TAG_TOGGLE); }
    Locator testStatusMenu(Page page) { return testStatusMenu(pages.testsView(page)); }
    Locator testTagMenu(Page page) { return testTagMenu(pages.testsView(page)); }
    Locator testStatusTrigger(Page page) {
        return pages.testsView(page).locator(TEST_LIST_TOOLS + " " + classToken("dropdown")
                + ":nth-child(1) " + classToken("dropdown-toggle"));
    }
    Locator testTagTrigger(Page page) {
        return pages.testsView(page).locator(TEST_LIST_TOOLS + " " + classToken("dropdown")
                + ":nth-child(2) " + classToken("dropdown-toggle"));
    }
    AtfLocator testStatusTriggerAction(Page page) {
        return new AtfLocator(testStatusTrigger(page),
                TESTS_VIEW + " " + TEST_LIST_TOOLS + " " + classToken("dropdown")
                        + ":nth-child(1) " + classToken("dropdown-toggle"));
    }
    AtfLocator testTagTriggerAction(Page page) {
        return new AtfLocator(testTagTrigger(page),
                TESTS_VIEW + " " + TEST_LIST_TOOLS + " " + classToken("dropdown")
                        + ":nth-child(2) " + classToken("dropdown-toggle"));
    }
    Locator statusFilterOptions(Locator menu) { return menu.locator(classToken("dropdown-item") + "[status]"); }
    Locator filterOptions(Locator menu) { return menu.locator(classToken("dropdown-item")); }
    Locator statusFilterOption(Page page, String status) {
        return testStatusMenu(page).locator("[status='" + ReportLocatorSupport.cssAttribute(status) + "']");
    }
    AtfLocator statusFilterAction(Page page, String status) {
        String safeStatus = ReportLocatorSupport.cssAttribute(status);
        return new AtfLocator(statusFilterOption(page, safeStatus),
                STATUS_TOGGLE + " [status='" + safeStatus + "']");
    }
    AtfLocator clearStatusFilterAction(Page page) { return statusFilterAction(page, "clear"); }
    Locator tagFilters(Page page) { return testTagMenu(page).locator(TAG_FILTER); }
    Locator activeTagFilters(Page page) { return tagFilters(page).locator("[aria-pressed='true']"); }
    AtfLocator tagFilterAction(Page page, int zeroBasedIndex) {
        int index = ReportLocatorSupport.requireIndex(zeroBasedIndex);
        return new AtfLocator(tagFilters(page).nth(index),
                TAG_TOGGLE + " " + TAG_FILTER + ":nth-of-type(" + (index + 1) + ")");
    }
    AtfLocator activeTagFilterAction(Page page) {
        return new AtfLocator(activeTagFilters(page).first(),
                TAG_TOGGLE + " " + TAG_FILTER + "[aria-pressed='true']");
    }

    Locator statusBadges(Locator scope) { return scope.locator("h5" + classToken("test-status") + ":visible"); }
    Locator skippedOutcomeNodes(Locator scope) {
        return scope.locator("[data-atf-scenario-id] > " + classToken("card-header") + " "
                        + classToken("card-title") + " " + classToken("node"))
                .filter(new Locator.FilterOptions().setHasText("body skipped"));
    }
    Locator nodeHeaders(Locator scope) {
        return scope.locator("div" + classToken("card-header") + "[role='button']");
    }
    Locator nodeTitles(Locator scope) {
        return scope.locator(classToken("card-title") + " " + classToken("node"));
    }
    Locator nodeTitleByText(Locator scope, String text) {
        return nodeTitles(scope).filter(new Locator.FilterOptions().setHasText(text)).first();
    }
    String nodeTitle(Locator card) {
        return card.locator(":scope > " + classToken("card-header") + " > "
                + classToken("card-title") + " > " + classToken("node")).innerText().trim();
    }
    Locator nodeHeader(Locator card) {
        return card.locator(":scope > " + classToken("card-header") + "[role='button']");
    }
    Locator boundScenarioHeader(Locator scenario) {
        return scenario.locator(":scope > " + classToken("card-header")
                + "[data-atf-scenario-collapse-bound='true']");
    }
    Locator boundPhaseHeader(Locator phase) {
        return phase.locator(":scope > " + classToken("card-header")
                + "[data-atf-phase-header-bound='true']");
    }
    Locator nodeControl(Locator header) { return header.locator(classToken("node")); }
    Locator nodeHeader(Locator scope, String text) {
        return nodeHeaders(scope).filter(new Locator.FilterOptions().setHasText(text));
    }
    Locator nodeLinks(Locator scope) { return scope.locator("a" + classToken("node")); }
    Locator visibleTestDetail(Page page) { return pages.testsView(page).locator(VISIBLE_TEST_DETAIL).last(); }
    Locator detailForOwner(Locator owner) {
        return owner.locator(":scope > " + classToken("test-contents") + ":visible").first();
    }
    Locator scenarios(Locator detail) { return detail.locator("[data-atf-scenario-id]"); }
    Locator visibleScenarios(Locator detail) { return detail.locator("[data-atf-scenario-id]:visible"); }
    Locator scenarioCards(Page page) { return scenarios(pages.testContentDetail(page)); }
    Locator phases(Locator detail) { return detail.locator(PHASE_NODE); }
    Locator visiblePhases(Locator detail) { return detail.locator(PHASE_NODE + ":visible"); }
    Locator scenarioAccordion(Locator scenario) { return scenario.locator(":scope > " + classToken("accordion")); }
    Locator phaseAccordion(Locator phase) { return phase.locator(":scope > " + classToken("accordion")); }
    Locator failedScenarioBadge(Locator scenario) {
        return scenario.locator(":scope > " + classToken("card-header") + " " + FAILURE_BADGE);
    }
    Locator failedPhaseBadge(Locator phase) {
        return phase.locator(":scope > " + classToken("card-header") + " " + FAILURE_BADGE);
    }
    Locator failedRequestBadge(Locator request) {
        return request.locator(":scope > " + classToken("card-header") + " " + FAILURE_BADGE);
    }
    Locator phaseDirectBody(Locator phase) {
        return phase.locator(PHASE_DIRECT_BODY + ", :scope > div:not(" + classToken("card-header")
                + "):not(" + classToken("accordion") + ")");
    }
    Locator phaseFailureDisclosures(Locator phase) { return phase.locator(PHASE_FAILURE_DISCLOSURE); }
    Locator phaseFailureTraceTables(Locator phase) { return phase.locator(PHASE_FAILURE_TRACE); }
    Locator phaseFailureCauseValues(Locator phase) {
        return phase.locator(PHASE_FAILURE_CAUSE)
                .filter(new Locator.FilterOptions().setHasText("FAILURE CAUSE"));
    }
    Locator phaseFailureFieldValues(Locator phase, String field) {
        return phase.locator(PHASE_FAILURE_TRACE + " tbody tr")
                .filter(new Locator.FilterOptions().setHasText(field)).locator("td").nth(1);
    }
    Locator phaseFailureDisclosureSummary(Locator disclosure) {
        return disclosure.locator(":scope > summary");
    }
    AtfLocator phaseFailureDisclosureAction(Locator phase) {
        Locator disclosure = phaseFailureDisclosures(phase).first();
        String selector = phaseCardSelector(phase) + " " + PHASE_FAILURE_DISCLOSURE + " > summary";
        return new AtfLocator(phaseFailureDisclosureSummary(disclosure), selector);
    }
    Locator expandedScenarioHeaders(Locator scope) {
        return scope.locator("[data-atf-scenario-id]:visible > " + classToken("card-header")
                + "[role='button'][aria-expanded='true']");
    }
    Locator expandedPhaseHeaders(Locator scope) {
        return scope.locator("[data-atf-node-role='phase']:visible > " + classToken("card-header")
                + "[role='button'][aria-expanded='true']");
    }
    Locator expandedRequestOrValidationHeaders(Locator scope) {
        return scope.locator("[data-atf-node-role='request']:visible > " + classToken("card-header")
                + "[role='button'][aria-expanded='true'],"
                + "[data-atf-node-role='validation']:visible > " + classToken("card-header")
                + "[role='button'][aria-expanded='true']");
    }
    Locator openEvidenceDisclosures(Locator scope) { return scope.locator("details[open]:visible"); }
    Locator visibleClosedEvidenceBodies(Locator scope) { return scope.locator(CLOSED_EVIDENCE_BODY); }
    Locator semanticEvidenceDisclosures(Locator scope) { return scope.locator(SEMANTIC_EVIDENCE_DISCLOSURE); }
    Locator phase(Locator scope, ReportUiContract.Phase phase) {
        return scope.locator(ReportUiContract.phaseSelector(phase));
    }
    Locator visiblePhase(Locator scope, ReportUiContract.Phase phase) {
        return scope.locator(ReportUiContract.phaseSelector(phase) + ":visible");
    }
    Locator evidenceDisclosures(Locator scope) { return scope.locator("details"); }
    Locator nodeControls(Locator scope) {
        return scope.locator("[data-atf-node-role] > " + classToken("card-header") + " "
                + classToken("node"));
    }
    Locator requests(Locator scope) { return scope.locator(REQUEST_NODE); }
    Locator requestsInPhase(Locator phase) {
        return phase.locator(":scope > " + classToken("accordion")
                + " > [data-atf-node-role='request']");
    }
    Locator visibleRequestsInPhase(Locator phase) {
        return phase.locator(":scope > " + classToken("accordion")
                + " > [data-atf-node-role='request']:visible");
    }
    Locator cardForNode(Locator node) {
        return node.locator("xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' card ')][1]");
    }
    AtfLocator nodeHeaderAction(Locator header) {
        String nodeId = ReportLocatorSupport.nodeId(header);
        return new AtfLocator(header, ReportLocatorSupport.nodeHeaderSelector(nodeId));
    }
    AtfLocator nodeCardAction(Locator card) {
        Locator header = nodeHeader(card);
        String nodeId = ReportLocatorSupport.nodeId(header);
        return new AtfLocator(card, ReportLocatorSupport.nodeScopedSelector(nodeId, "").trim());
    }
    String nodeOpenAttribute(Locator card) {
        String role = card.getAttribute("data-atf-node-role");
        return "phase".equals(role) ? "data-atf-phase-open"
                : "scenario".equals(role) || card.getAttribute("data-atf-scenario-id") != null
                ? "data-atf-scenario-open" : "data-atf-node-open";
    }
    AtfLocator scenarioNodeAction(Locator scenario) { return nodeHeaderAction(nodeHeader(scenario)); }
    AtfLocator scenarioStateTarget(Locator scenario) {
        String id = ReportLocatorSupport.requiredAttribute(scenario, "data-atf-scenario-id", "scenario");
        return new AtfLocator(scenario, TESTS_VIEW + " " + VISIBLE_TEST_DETAIL
                + " " + classToken("card") + "[data-atf-scenario-id='"
                + ReportLocatorSupport.cssAttribute(id) + "']");
    }
    AtfLocator phaseNodeAction(Locator phase) { return nodeHeaderAction(nodeHeader(phase)); }
    AtfLocator phaseStateTarget(Locator phase) {
        return new AtfLocator(phase, phaseCardSelector(phase));
    }
    String nodeDescription(Locator header) {
        Locator card = ReportLocatorSupport.cardForHeader(header);
        String role = card.getAttribute("data-atf-node-role");
        String title = card.locator(":scope > " + classToken("card-header") + " > "
                + classToken("card-title") + " > " + classToken("node")).innerText().trim();
        String nodeId = ReportLocatorSupport.nodeId(header);
        String kind = role == null || role.isBlank() ? "scenario" : role;
        return kind + " '" + title + "' (node id=" + nodeId + ")";
    }

    private String phaseCardSelector(Locator phase) {
        return ReportLocatorSupport.nodeScopedSelector(
                ReportLocatorSupport.nodeId(nodeHeader(phase)), "").trim();
    }

    private static String classToken(String className) {
        return ReportLocatorSupport.classSelector(className);
    }
}


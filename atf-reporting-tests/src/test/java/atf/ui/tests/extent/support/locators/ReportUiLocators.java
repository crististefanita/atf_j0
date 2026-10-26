package atf.ui.tests.extent.support.locators;

import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Map;

/**
 * Stable public locator facade for the rendered ATF report.
 *
 * <p>The facade deliberately keeps the existing consumer API stable while
 * semantic selector ownership lives in page, tree, evidence, category,
 * failure and ui-validation ports.</p>
 */
public final class ReportUiLocators {
    private final ReportPageLocators pages = new ReportPageLocators();
    private final ReportEvidenceLocators evidence = new ReportEvidenceLocators();
    private final ReportTreeLocators tree = new ReportTreeLocators(pages);
    private final ReportUiValidationLocators screen =
            new ReportUiValidationLocators(pages, evidence);
    private final ReportCategoryLocators categories = new ReportCategoryLocators();
    private final ReportFailureLocators failures = new ReportFailureLocators();

    public Locator exactText(Locator scope, String text) { return pages.exactText(scope, text); }
    public Locator exactLink(Locator scope, String text) { return pages.exactLink(scope, text); }

    public Locator testsView(Page page) { return pages.testsView(page); }
    public Locator runtimeRoot(Page page) { return pages.runtimeRoot(page); }
    public Locator decorativeSeparators(Page page) { return pages.decorativeSeparators(page); }
    public Locator images(Page page) { return pages.images(page); }
    public Locator navTab(Page page, ReportUiContract.Tab tab) { return pages.navTab(page, tab); }
    public AtfLocator navTabAction(Page page, ReportUiContract.Tab tab) {
        return pages.navTabAction(page, tab);
    }
    public Locator overviewView(Page page) { return pages.overviewView(page); }
    public Locator categoryView(Page page) { return pages.categoryView(page); }
    public Locator failsView(Page page) { return pages.failsView(page); }
    public Locator tabView(Page page, ReportUiContract.Tab tab) { return pages.tabView(page, tab); }
    public Locator tabViewByHash(Page page, String hash) { return pages.tabViewByHash(page, hash); }
    public ReportUiContract.Tab tabByHash(String hash) { return pages.tabByHash(hash); }
    public Locator testsList(Page page) { return pages.testsList(page); }
    public Locator visibleDisclosureHeaders(Page page, ReportUiContract.Tab tab) {
        return pages.visibleDisclosureHeaders(page, tab);
    }
    public Locator disclosureHeaderNodes(Locator header) {
        return pages.disclosureHeaderNodes(header);
    }
    public Locator openDisclosures(Locator scope) { return pages.openDisclosures(scope); }
    public Locator allDisclosures(Locator scope) { return pages.allDisclosures(scope); }
    public Locator collapsedReportPanels(Locator scope) { return pages.collapsedReportPanels(scope); }
    public Locator overviewCard(Page page) { return pages.overviewCard(page); }
    public Locator categoryList(Page page) { return pages.categoryList(page); }
    public Locator categoryList(Locator category) { return pages.categoryList(category); }
    public Locator testContentDetail(Page page) { return pages.testContentDetail(page); }
    public Locator categoryContentDetail(Page page) { return pages.categoryContentDetail(page); }
    public Locator overviewTables(Locator overview) { return pages.overviewTables(overview); }
    public Locator overviewSection(Locator overview, String selector) {
        return pages.overviewSection(overview, selector);
    }
    public Locator overviewMetrics(Locator overview) { return pages.overviewMetrics(overview); }
    public Locator overviewTagAuditTables(Locator overview) {
        return pages.overviewTagAuditTables(overview);
    }
    public Locator overviewExecutionMetadata(Locator overview) {
        return pages.overviewExecutionMetadata(overview);
    }
    public Locator overviewSecondaryZones(Locator overview) {
        return pages.overviewSecondaryZones(overview);
    }
    public Locator overviewSecondaryZone(Locator overview, ReportUiContract.OverviewSection zone) {
        return pages.overviewSecondaryZone(overview, zone);
    }
    public Locator overviewSecondaryZoneBody(Locator overview, ReportUiContract.OverviewSection zone) {
        return pages.overviewSecondaryZoneBody(overview, zone);
    }
    public Locator overviewOpenSecondaryZones(Locator overview) {
        return pages.overviewOpenSecondaryZones(overview);
    }
    public Locator overviewVisibleClosedSecondaryBodies(Locator overview) {
        return pages.overviewVisibleClosedSecondaryBodies(overview);
    }
    public Locator overviewPackageRows(Locator overview) {
        return pages.overviewPackageRows(overview);
    }
    public Locator testPackageNodes(Page page) {
        return pages.testPackageNodes(page);
    }
    public AtfLocator overviewExecutionMetadataAction(Page page) {
        return pages.overviewExecutionMetadataAction(page);
    }
    public AtfLocator overviewSecondaryZoneAction(Page page, ReportUiContract.OverviewSection zone) {
        return pages.overviewSecondaryZoneAction(page, zone);
    }
    public Locator overviewAuditRows(Locator overview) { return pages.overviewAuditRows(overview); }
    public Locator metricLabel(Locator metric) { return pages.metricLabel(metric); }
    public Locator metricValue(Locator metric) { return pages.metricValue(metric); }
    public Locator tableHeaders(Locator table) { return pages.tableHeaders(table); }
    public Locator tableRows(Locator table) { return pages.tableRows(table); }
    public Locator tableCells(Locator row) { return pages.tableCells(row); }
    public Locator overviewCategoryCells(Locator overview) {
        return pages.overviewCategoryCells(overview);
    }

    public Locator uiValidationNodes(Page page) { return screen.uiValidationNodes(page); }
    public Locator visibleUiValidationNode(Page page) {
        return screen.visibleUiValidationNode(page);
    }
    public Locator visibleUiValidationNodeWithScreenshot(Page page) {
        return screen.visibleUiValidationNodeWithScreenshot(page);
    }
    public AtfLocator uiValidationNodeAction(Page page) {
        return screen.uiValidationNodeAction(page);
    }
    public Locator uiValidationCard(Locator node) { return screen.uiValidationCard(node); }

    public Locator testOwners(Page page) { return tree.testOwners(page); }
    public Locator testOwners(Locator tests) { return tree.testOwners(tests); }
    public Locator testOwnerNames(Locator tests) { return tree.testOwnerNames(tests); }
    public Locator testOwnerName(Locator owner) { return tree.testOwnerName(owner); }
    public Locator testOwnerByName(Locator tests, String className) {
        return tree.testOwnerByName(tests, className);
    }
    public Locator testOwnerItemByName(Locator tests, String className) {
        return tree.testOwnerItemByName(tests, className);
    }
    public Locator ownerStatusBadge(Locator owner) { return tree.ownerStatusBadge(owner); }
    public Locator nodeStatusBadge(Locator card) { return tree.nodeStatusBadge(card); }
    public Locator testOwnerItem(Page page, int zeroBasedIndex) {
        return tree.testOwnerItem(page, zeroBasedIndex);
    }
    public AtfLocator testOwnerItemAction(Page page, int zeroBasedIndex) {
        return tree.testOwnerItemAction(page, zeroBasedIndex);
    }
    public AtfLocator firstTestOwnerAction(Page page) { return tree.firstTestOwnerAction(page); }
    public AtfLocator testOwnerNameAction(Page page, int zeroBasedIndex) {
        return tree.testOwnerNameAction(page, zeroBasedIndex);
    }
    public AtfLocator testOwnerNameAction(Locator owner) { return tree.testOwnerNameAction(owner); }
    public AtfLocator testOwnerItemAction(Page page, String className) {
        return tree.testOwnerItemAction(page, className);
    }
    public Locator visibleTestOwners(Locator tests) { return tree.visibleTestOwners(tests); }
    public Locator testStatusMenu(Locator tests) { return tree.testStatusMenu(tests); }
    public Locator testTagMenu(Locator tests) { return tree.testTagMenu(tests); }
    public Locator testStatusMenu(Page page) { return tree.testStatusMenu(page); }
    public Locator testTagMenu(Page page) { return tree.testTagMenu(page); }
    public Locator testStatusTrigger(Page page) { return tree.testStatusTrigger(page); }
    public Locator testTagTrigger(Page page) { return tree.testTagTrigger(page); }
    public AtfLocator testStatusTriggerAction(Page page) { return tree.testStatusTriggerAction(page); }
    public AtfLocator testTagTriggerAction(Page page) { return tree.testTagTriggerAction(page); }
    public Locator statusFilterOptions(Locator menu) { return tree.statusFilterOptions(menu); }
    public Locator filterOptions(Locator menu) { return tree.filterOptions(menu); }
    public Locator statusFilterOption(Page page, String status) {
        return tree.statusFilterOption(page, status);
    }
    public AtfLocator statusFilterAction(Page page, String status) {
        return tree.statusFilterAction(page, status);
    }
    public AtfLocator clearStatusFilterAction(Page page) { return tree.clearStatusFilterAction(page); }
    public Locator tagFilters(Page page) { return tree.tagFilters(page); }
    public Locator activeTagFilters(Page page) { return tree.activeTagFilters(page); }
    public AtfLocator tagFilterAction(Page page, int zeroBasedIndex) {
        return tree.tagFilterAction(page, zeroBasedIndex);
    }
    public AtfLocator activeTagFilterAction(Page page) { return tree.activeTagFilterAction(page); }
    public Locator statusBadges(Locator scope) { return tree.statusBadges(scope); }
    public Locator skippedOutcomeNodes(Locator scope) { return tree.skippedOutcomeNodes(scope); }
    public Locator nodeHeaders(Locator scope) { return tree.nodeHeaders(scope); }
    public Locator nodeTitles(Locator scope) { return tree.nodeTitles(scope); }
    public Locator nodeTitleByText(Locator scope, String text) {
        return tree.nodeTitleByText(scope, text);
    }
    public String nodeTitle(Locator card) { return tree.nodeTitle(card); }
    public Locator nodeHeader(Locator card) { return tree.nodeHeader(card); }
    public Locator boundScenarioHeader(Locator scenario) { return tree.boundScenarioHeader(scenario); }
    public Locator boundPhaseHeader(Locator phase) { return tree.boundPhaseHeader(phase); }
    public Locator nodeControl(Locator header) { return tree.nodeControl(header); }
    public Locator nodeHeader(Locator scope, String text) { return tree.nodeHeader(scope, text); }
    public Locator nodeLinks(Locator scope) { return tree.nodeLinks(scope); }
    public Locator visibleTestDetail(Page page) { return tree.visibleTestDetail(page); }
    public Locator detailForOwner(Locator owner) { return tree.detailForOwner(owner); }
    public Locator scenarios(Locator detail) { return tree.scenarios(detail); }
    public Locator visibleScenarios(Locator detail) { return tree.visibleScenarios(detail); }
    public Locator scenarioCards(Page page) { return tree.scenarioCards(page); }
    public Locator phases(Locator detail) { return tree.phases(detail); }
    public Locator visiblePhases(Locator detail) { return tree.visiblePhases(detail); }
    public Locator scenarioAccordion(Locator scenario) { return tree.scenarioAccordion(scenario); }
    public Locator phaseAccordion(Locator phase) { return tree.phaseAccordion(phase); }
    public Locator failedScenarioBadge(Locator scenario) { return tree.failedScenarioBadge(scenario); }
    public Locator failedPhaseBadge(Locator phase) { return tree.failedPhaseBadge(phase); }
    public Locator failedRequestBadge(Locator request) { return tree.failedRequestBadge(request); }
    public Locator phaseDirectBody(Locator phase) { return tree.phaseDirectBody(phase); }
    public Locator phaseFailureDisclosures(Locator phase) { return tree.phaseFailureDisclosures(phase); }
    public Locator phaseFailureTraceTables(Locator phase) { return tree.phaseFailureTraceTables(phase); }
    public Locator phaseFailureCauseValues(Locator phase) { return tree.phaseFailureCauseValues(phase); }
    public Locator phaseFailureFieldValues(Locator phase, String field) {
        return tree.phaseFailureFieldValues(phase, field);
    }
    public Locator phaseFailureDisclosureSummary(Locator disclosure) {
        return tree.phaseFailureDisclosureSummary(disclosure);
    }
    public AtfLocator phaseFailureDisclosureAction(Locator phase) {
        return tree.phaseFailureDisclosureAction(phase);
    }
    public Locator expandedScenarioHeaders(Locator scope) { return tree.expandedScenarioHeaders(scope); }
    public Locator expandedPhaseHeaders(Locator scope) { return tree.expandedPhaseHeaders(scope); }
    public Locator expandedRequestOrValidationHeaders(Locator scope) {
        return tree.expandedRequestOrValidationHeaders(scope);
    }
    public Locator openEvidenceDisclosures(Locator scope) { return tree.openEvidenceDisclosures(scope); }
    public Locator visibleClosedEvidenceBodies(Locator scope) {
        return tree.visibleClosedEvidenceBodies(scope);
    }
    public Locator semanticEvidenceDisclosures(Locator scope) {
        return tree.semanticEvidenceDisclosures(scope);
    }
    public Locator phase(Locator scope, ReportUiContract.Phase phase) { return tree.phase(scope, phase); }
    public Locator visiblePhase(Locator scope, ReportUiContract.Phase phase) {
        return tree.visiblePhase(scope, phase);
    }
    public Locator evidenceDisclosures(Locator scope) { return tree.evidenceDisclosures(scope); }
    public Locator nodeControls(Locator scope) { return tree.nodeControls(scope); }
    public Locator requests(Locator scope) { return tree.requests(scope); }
    public Locator requestsInPhase(Locator phase) { return tree.requestsInPhase(phase); }
    public Locator visibleRequestsInPhase(Locator phase) { return tree.visibleRequestsInPhase(phase); }
    public AtfLocator nodeHeaderAction(Locator header) { return tree.nodeHeaderAction(header); }
    public AtfLocator nodeCardAction(Locator card) { return tree.nodeCardAction(card); }
    public String nodeOpenAttribute(Locator card) { return tree.nodeOpenAttribute(card); }
    public AtfLocator scenarioNodeAction(Locator scenario) { return tree.scenarioNodeAction(scenario); }
    public AtfLocator scenarioStateTarget(Locator scenario) { return tree.scenarioStateTarget(scenario); }
    public AtfLocator phaseNodeAction(Locator phase) { return tree.phaseNodeAction(phase); }
    public AtfLocator phaseStateTarget(Locator phase) { return tree.phaseStateTarget(phase); }
    public String nodeDescription(Locator header) { return tree.nodeDescription(header); }
    public Locator cardForNode(Locator node) { return tree.cardForNode(node); }

    public Locator validationTables(Locator scope) { return screen.validationTables(scope); }
    public Locator validationCards(Locator scope) { return screen.validationCards(scope); }
    public Locator validationNodes(Locator scope) { return screen.validationNodes(scope); }
    public Locator visibleValidationCards(Locator scope) { return screen.visibleValidationCards(scope); }
    public Locator firstVisibleActionSequence(Locator scope) { return screen.firstVisibleActionSequence(scope); }
    public Locator validationCardForEvidence(Locator evidenceLocator) {
        return screen.validationCardForEvidence(evidenceLocator);
    }
    public Locator firstCompleteUiValidationCard(Locator scope) {
        return screen.firstCompleteUiValidationCard(scope);
    }
    public Locator uiValidationActionZones(Locator scope) { return screen.uiValidationActionZones(scope); }
    public Locator uiValidationObservationZones(Locator scope) {
        return screen.uiValidationObservationZones(scope);
    }
    public Locator uiValidationAssertionZones(Locator scope) {
        return screen.uiValidationAssertionZones(scope);
    }
    public Locator uiValidationZones(Locator scope) { return screen.uiValidationZones(scope); }
    public Locator uiValidationZoneRows(Locator scope) { return screen.uiValidationZoneRows(scope); }
    public Locator uiValidationActionSequences(Locator card) {
        return screen.uiValidationActionSequences(card);
    }
    public Locator uiValidationObservations(Locator card) {
        return screen.uiValidationObservations(card);
    }
    public Locator uiValidationAssertionTables(Locator card) {
        return screen.uiValidationAssertionTables(card);
    }
    public Locator uiValidationEvidenceTables(Locator card) {
        return screen.uiValidationEvidenceTables(card);
    }
    public Locator setupEvidence(Locator scope) { return screen.setupEvidence(scope); }
    public Locator setupEvidenceRows(Locator evidenceLocator) {
        return screen.setupEvidenceRows(evidenceLocator);
    }
    public Locator evidenceEventStatuses(Locator evidenceLocator) {
        return screen.evidenceEventStatuses(evidenceLocator);
    }
    public Locator failedEvidenceEventStatuses(Locator evidenceLocator) {
        return screen.failedEvidenceEventStatuses(evidenceLocator);
    }
    public Locator uiFailureEvidenceFor(Locator evidenceLocator) {
        return screen.uiFailureEvidenceFor(evidenceLocator);
    }
    public Locator uiValidationScreenshotLinks(Locator card) {
        return screen.uiValidationScreenshotLinks(card);
    }
    public Locator uiValidationScreenshotLabels(Locator card) {
        return screen.uiValidationScreenshotLabels(card);
    }
    public Locator uiValidationSeparators(Locator scope) {
        return screen.uiValidationSeparators(scope);
    }

    public Locator requestDetails(Locator request) { return evidence.requestDetails(request); }
    public Locator validationForOwner(Locator scope, ReportEvidenceOwner owner) {
        return evidence.validationForOwner(scope, owner);
    }
    public Locator httpEvidenceDisclosures(Locator scope) { return evidence.httpEvidenceDisclosures(scope); }
    public Locator responseDetails(Locator request) { return evidence.responseDetails(request); }
    public Locator executionEvidence(Locator request) { return evidence.executionEvidence(request); }
    public Locator requestTable(Locator scope) { return evidence.requestTable(scope); }
    public Locator responseTable(Locator scope) { return evidence.responseTable(scope); }
    public Locator executionTable(Locator scope) { return evidence.executionTable(scope); }
    public Locator visibleRequestTable(Locator scope) { return evidence.visibleRequestTable(scope); }
    public Locator visibleResponseTable(Locator scope) { return evidence.visibleResponseTable(scope); }
    public Locator visibleExecutionTable(Locator scope) { return evidence.visibleExecutionTable(scope); }
    public Locator retryHistoryTable(Locator scope) { return evidence.retryHistoryTable(scope); }
    public Locator retryHistoryRows(Locator table) { return evidence.retryHistoryRows(table); }
    public Locator retryHistoryCells(Locator row) { return evidence.retryHistoryCells(row); }
    public Locator tableColumns(Locator table) { return evidence.tableColumns(table); }
    public Locator requestNodeByName(Locator scope, String requestName) {
        return evidence.requestNodeByName(scope, requestName);
    }
    public Locator requestNodeHeader(Locator request) { return evidence.requestNodeHeader(request); }
    public Locator requestNodeToggle(Locator request) { return evidence.requestNodeToggle(request); }
    public AtfLocator requestNodeToggleAction(Locator request) {
        return evidence.requestNodeToggleAction(request);
    }
    public String requestTitle(Locator request) { return evidence.requestTitle(request); }
    public String requestUrl(Locator request) { return evidence.requestUrl(request); }
    public Map<String, String> requestFields(Locator request) { return evidence.requestFields(request); }
    public Map<String, String> responseFields(Locator request) { return evidence.responseFields(request); }
    public Map<String, String> executionFields(Locator request) { return evidence.executionFields(request); }
    public int responseStatus(Locator request) { return evidence.responseStatus(request); }
    public String responseUrl(Locator request) { return evidence.responseUrl(request); }
    public Locator requestJsonBodies(Locator request) { return evidence.requestJsonBodies(request); }
    public Locator responseJsonBodies(Locator request) { return evidence.responseJsonBodies(request); }
    public Locator requestJsonBody(Locator scope) { return evidence.requestJsonBody(scope); }
    public Locator requestJsonBodyElements(Locator scope) { return evidence.requestJsonBodyElements(scope); }
    public Locator responseJsonBody(Locator scope) { return evidence.responseJsonBody(scope); }
    public Locator responseJsonBodyElements(Locator scope) { return evidence.responseJsonBodyElements(scope); }
    public Locator evidenceDisclosure(Locator scope, String text) { return exactText(scope, text); }
    public Locator requestEvidenceDisclosure(Locator request) {
        return evidence.requestEvidenceDisclosure(request);
    }
    public Locator responseEvidenceDisclosure(Locator request) {
        return evidence.responseEvidenceDisclosure(request);
    }
    public Locator executionEvidenceDisclosure(Locator request) {
        return evidence.executionEvidenceDisclosure(request);
    }
    public void moveRequestEvidenceToOwner(Locator owner) { evidence.moveRequestEvidenceToOwner(owner); }
    public void moveResponseEvidenceToOwner(Locator owner) { evidence.moveResponseEvidenceToOwner(owner); }
    public AtfLocator requestEvidenceDisclosureAction(Locator request) {
        return evidence.requestEvidenceDisclosureAction(request);
    }
    public AtfLocator responseEvidenceDisclosureAction(Locator request) {
        return evidence.responseEvidenceDisclosureAction(request);
    }
    public AtfLocator executionEvidenceDisclosureAction(Locator request) {
        return evidence.executionEvidenceDisclosureAction(request);
    }
    public Locator parentDisclosure(Locator summary) { return evidence.parentDisclosure(summary); }
    public Locator durationBadge(Locator node) { return evidence.durationBadge(node); }
    public Locator actionSequences(Locator scope) { return evidence.actionSequences(scope); }
    public Locator screenshotLinks(Locator scope) { return evidence.screenshotLinks(scope); }
    public Locator base64ScreenshotLinks(Locator scope) { return evidence.base64ScreenshotLinks(scope); }
    public Locator dataUriScreenshotLinks(Locator scope) { return evidence.dataUriScreenshotLinks(scope); }
    public Locator featherlightScreenshotLinks(Locator scope) {
        return evidence.featherlightScreenshotLinks(scope);
    }
    public Locator nestedScreenshotImages(Locator scope) { return evidence.nestedScreenshotImages(scope); }
    public Locator inlineScreenshotImages(Locator screenshotLink) {
        return evidence.inlineScreenshotImages(screenshotLink);
    }
    public Locator duplicateScreenshotMarkers(Locator scope) {
        return evidence.duplicateScreenshotMarkers(scope);
    }
    public Locator actionRows(Locator sequence) { return evidence.actionRows(sequence); }
    public Locator failedActionRows(Locator sequence) { return evidence.failedActionRows(sequence); }
    public Locator failedObservationRows(Locator observations) { return evidence.failedObservationRows(observations); }
    public Locator failedAssertionRows(Locator table) { return evidence.failedAssertionRows(table); }
    public Locator sequenceSummary(Locator sequence) { return evidence.sequenceSummary(sequence); }
    public Locator actionRowCells(Locator row) { return evidence.actionRowCells(row); }
    public Locator actionIdentityCell(Locator row) { return evidence.actionIdentityCell(row); }
    public Locator actionOperation(Locator identityCell) { return evidence.actionOperation(identityCell); }
    public Locator actionNumber(Locator identityCell) { return evidence.actionNumber(identityCell); }
    public Locator actionTime(Locator row) { return evidence.actionTime(row); }
    public Locator disclosureSummary(Locator disclosure) { return evidence.disclosureSummary(disclosure); }
    public Locator visibleDisclosure(Locator scope) { return evidence.visibleDisclosure(scope); }
    public Locator nodeAncestors(Locator scope) { return evidence.nodeAncestors(scope); }
    public Locator lightbox(Page page) { return evidence.lightbox(page); }
    public Locator lightboxImage(Locator lightbox) { return evidence.lightboxImage(lightbox); }
    public Locator lightboxClose(Locator lightbox) { return evidence.lightboxClose(lightbox); }

    public Locator categoryItems(Page page) { return categories.items(page); }
    public Locator categoryItem(Locator items, int index) { return categories.item(items, index); }
    public Locator categoryItemNames(Locator items) { return categories.itemNames(items); }
    public Locator categoryItemName(Locator item) { return categories.itemName(item); }
    public Locator categoryItemDuration(Locator item) { return categories.itemDuration(item); }
    public Locator categoryStatusMenu(Locator category) { return categories.statusMenu(category); }
    public Locator categoryTagMenu(Locator category) { return categories.tagMenu(category); }
    public Locator categoryStatusFilterTrigger(Locator category) { return categories.statusFilterTrigger(category); }
    public Locator categoryTagFilterTrigger(Locator category) { return categories.tagFilterTrigger(category); }
    public Locator categoryAggregateStatuses(Locator category) { return categories.aggregateStatuses(category); }
    public Locator categoryStatusOption(Locator menu, String status) {
        return categories.statusOption(menu, status);
    }
    public Locator categoryStatusOptions(Locator category) { return categories.statusOptions(category); }
    public AtfLocator categoryStatusFilterTriggerAction(Page page) {
        return categories.statusFilterTriggerAction(page);
    }
    public AtfLocator categoryStatusOptionAction(Page page, String status) {
        return categories.statusOptionAction(page, status);
    }
    public Locator categoryTagOptions(Locator category) { return categories.tagOptions(category); }
    public Locator clearCategoryFilter(Locator categoryOrMenu) { return categories.clearFilter(categoryOrMenu); }
    public Locator categoryVisibleItems(Locator category) { return categories.visibleItems(category); }
    public Locator visibleCategoryDetail(Page page) { return categories.visibleDetail(page); }
    public Locator categoryTitle(Locator detail) { return categories.title(detail); }
    public Locator categoryLinkedTests(Locator detail) { return categories.linkedTests(detail); }
    public Locator categoryStatusRows(Locator detail) { return categories.statusRows(detail); }
    public AtfLocator categoryItemAction(Page page, int zeroBasedIndex) {
        return categories.itemAction(page, zeroBasedIndex);
    }
    public AtfLocator visibleCategoryDetailAnchor(Page page) { return categories.visibleDetailAnchor(page); }
    public AtfLocator firstCategoryItemAction(Page page) { return categories.firstItemAction(page); }
    public Locator categorySurface(Page page) { return categories.surface(page); }
    public Locator categoryContent(Page page) { return categories.content(page); }
    public Locator categoryContent(Locator category) { return categories.content(category); }
    public AtfLocator firstCategoryLinkAction(Page page) { return categories.firstLinkAction(page); }
    public String categoryItemSelector(int zeroBasedIndex) { return categories.itemSelector(zeroBasedIndex); }

    public Locator failureNames(Locator fails) { return failures.names(fails); }
    public Locator failureItems(Locator fails) { return failures.items(fails); }
    public Locator failureItemByName(Locator fails, String name) { return failures.itemByName(fails, name); }
    public Locator failureListSurface(Locator fails) { return failures.listSurface(fails); }
    public Locator failureDetailSurface(Locator fails) { return failures.detailSurface(fails); }
    public Locator failureOpenLinks(Locator fails) { return failures.openLinks(fails); }
    public Locator failureDetail(Locator fails) { return failures.detail(fails); }
    public Locator failureDetailLabels(Locator fails) { return failures.detailLabels(fails); }
    public Locator failureDetailTitle(Locator fails) { return failures.detailTitle(fails); }
    public Locator failureDetailOpenLink(Locator fails) { return failures.detailOpenLink(fails); }
    public Map<String, Object> failureLayout(Locator fails) { return failures.layout(fails); }
    public AtfLocator failureDetailAction(Locator fails) {
        return new AtfLocator(failureDetail(fails),
                ReportLocatorSupport.idSelector("atf-fails-view") + " "
                        + ReportLocatorSupport.idSelector("atf-failure-detail"));
    }
    public Locator failureCauseValues(Locator fails) { return failures.causeValues(fails); }
    public Locator failureDetailPrimaryCauses(Locator fails) { return failures.detailPrimaryCauses(fails); }
    public Locator failureDetailSecondaryCauses(Locator fails) { return failures.detailSecondaryCauses(fails); }
    public Locator failureEmptyState(Locator fails) { return failures.emptyState(fails); }
    public AtfLocator failureItemAction(Page page, int zeroBasedIndex) {
        return failures.itemAction(page, zeroBasedIndex);
    }
    public AtfLocator failureItemActionByName(Page page, String name) {
        return failures.itemActionByName(page, name);
    }
    public AtfLocator firstFailureOpenAction(Page page) { return failures.firstOpenAction(page); }
    public AtfLocator failureDetailOpenAction(Page page) { return failures.detailOpenAction(page); }
    public Locator primaryFailureDetails(Locator fails) { return failures.primaryDetails(fails); }
}

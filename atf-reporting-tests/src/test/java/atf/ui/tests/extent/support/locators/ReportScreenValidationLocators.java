package atf.ui.tests.extent.support.locators;

import atf.reporting.theme.tokens.AtfReportCssSelectors;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Locators for screen-validation evidence and its three semantic zones. */
final class ReportScreenValidationLocators {
    private static final String TEST_CONTENT_DETAIL =
            AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector();
    private static final String SCREEN_VALIDATION_CARD = "[data-atf-node-role='validation']";
    private static final String ACTION_ZONE = "details[class~='atf-ui-action-sequence']";
    private static final String OBSERVATION_ZONE = "details[class~='atf-ui-observations']";
    private static final String ASSERTION_ZONE = "details[class~='assertion-evidence']";
    private static final String SCREEN_VALIDATION_ZONE = ACTION_ZONE + "," + OBSERVATION_ZONE + "," + ASSERTION_ZONE;
    private static final String EVIDENCE_TABLES =
            "table.atf-ui-action-table,table.atf-ui-observation-table,"
                    + "table.helm-validation-table,table.atf-setup-evidence-table";
    private static final String SETUP_EVIDENCE = "[class~='atf-setup-evidence'][data-atf-evidence-type='report-setup']";

    private final ReportPageLocators pages;
    private final ReportEvidenceLocators evidence;

    ReportScreenValidationLocators(ReportPageLocators pages, ReportEvidenceLocators evidence) {
        this.pages = pages;
        this.evidence = evidence;
    }

    Locator screenValidationNodes(Page page) {
        Locator typed = pages.testsView(page).locator(TEST_CONTENT_DETAIL + " "
                + SCREEN_VALIDATION_CARD + ":has(" + ACTION_ZONE + ")"
                + " > [class~='card-header'] [class~='card-title'] [class~='node']");
        if (typed.count() > 0) return typed;
        Locator candidates = pages.testsView(page).locator(TEST_CONTENT_DETAIL + " "
                + "[class~='card-title'] a[class~='node']");
        Locator uiValidation = candidates.filter(
                new Locator.FilterOptions().setHasText("UI validation"));
        return uiValidation;
    }
    Locator visibleScreenValidationNode(Page page) {
        Locator nodes = screenValidationNodes(page);
        for (Locator node : nodes.all()) if (node.isVisible()) return node;
        return nodes.first();
    }
    Locator visibleScreenValidationNodeWithScreenshot(Page page) {
        Locator nodes = screenValidationNodes(page);
        for (Locator node : nodes.all()) {
            if (node.isVisible() && screenValidationScreenshotLinks(screenValidationCard(node)).count() > 0) {
                return node;
            }
        }
        return visibleScreenValidationNode(page);
    }
    AtfLocator screenValidationNodeAction(Page page) {
        Locator node = visibleScreenValidationNodeWithScreenshot(page);
        String id = ReportLocatorSupport.requiredAttribute(node, "id", "UI validation node");
        return new AtfLocator(node, ReportLocatorSupport.TESTS_VIEW + " " + TEST_CONTENT_DETAIL
                + " [class~='card-title'] [class~='node'][id='"
                + ReportLocatorSupport.cssAttribute(id) + "']");
    }
    Locator screenValidationCard(Locator node) {
        return node.locator("xpath=..").locator("xpath=..").locator("xpath=..");
    }
    Locator validationTables(Locator scope) {
        return scope.locator("table[class~='helm-validation-table']");
    }
    Locator validationCards(Locator scope) {
        return scope.locator(":scope > [class~='accordion'] > [data-atf-node-role='validation']");
    }
    Locator validationNodes(Locator scope) {
        return scope.locator(ReportUiContract.nodeRoleSelector(ReportUiContract.NodeRole.VALIDATION));
    }
    Locator visibleValidationCards(Locator scope) { return scope.locator(SCREEN_VALIDATION_CARD + ":visible"); }
    Locator firstVisibleActionSequence(Locator scope) {
        Locator candidates = evidence.actionSequences(scope);
        for (int index = 0; index < candidates.count(); index++) {
            Locator candidate = candidates.nth(index);
            if (candidate.isVisible()) return candidate;
        }
        return candidates.first();
    }
    Locator validationCardForEvidence(Locator evidenceLocator) {
        Locator validation = evidenceLocator.locator(
                "xpath=ancestor::*[@data-atf-node-role='validation'][1]");
        if (validation.count() > 0) return validation;
        return evidenceLocator.locator("xpath=ancestor::div[contains(concat(' ', "
                + "normalize-space(@class), ' '), ' card ')][1]");
    }
    Locator firstCompleteScreenValidationCard(Locator scope) {
        Locator typed = scope.locator("div[class~='card'][data-atf-node-role='validation']:visible:has("
                + ACTION_ZONE + "):has(" + OBSERVATION_ZONE + "):has(" + ASSERTION_ZONE + ")").first();
        if (typed.count() > 0) return typed;
        return scope.locator("div[class~='card']:visible:has(" + ACTION_ZONE + "):has("
                + OBSERVATION_ZONE + "):has(" + ASSERTION_ZONE + ")").first();
    }
    Locator screenValidationActionZones(Locator scope) { return scope.locator(ACTION_ZONE); }
    Locator screenValidationObservationZones(Locator scope) { return scope.locator(OBSERVATION_ZONE); }
    Locator screenValidationAssertionZones(Locator scope) { return scope.locator(ASSERTION_ZONE); }
    Locator screenValidationZones(Locator scope) { return scope.locator(SCREEN_VALIDATION_ZONE); }
    Locator screenValidationZoneRows(Locator scope) {
        return scope.locator(":scope > [class~='collapse'] [class~='card-body'] > table > tbody > "
                + "tr[class~='event-row']");
    }
    Locator screenValidationActionSequences(Locator card) { return card.locator(ACTION_ZONE); }
    Locator screenValidationObservations(Locator card) { return card.locator(OBSERVATION_ZONE); }
    Locator screenValidationAssertionTables(Locator card) {
        return card.locator("table[class~='helm-validation-table']");
    }
    Locator screenValidationEvidenceTables(Locator card) {
        return card.locator(EVIDENCE_TABLES);
    }
    Locator setupEvidence(Locator scope) { return scope.locator(SETUP_EVIDENCE); }
    Locator setupEvidenceRows(Locator evidenceLocator) {
        return evidenceLocator.locator("table[class~='atf-setup-evidence-table'] tbody tr");
    }
    Locator evidenceEventStatuses(Locator evidenceLocator) {
        Locator event = evidenceLocator.locator(
                "xpath=ancestor::tr[contains(concat(' ', normalize-space(@class), ' '), ' event-row ')][1]");
        return event.locator(":scope > td:first-child [class~='badge']");
    }
    Locator failedEvidenceEventStatuses(Locator evidenceLocator) {
        return evidenceEventStatuses(evidenceLocator).filter(
                new Locator.FilterOptions().setHasText("Fail"));
    }
    Locator uiFailureEvidenceFor(Locator evidenceLocator) {
        return evidenceLocator.locator(
                        "xpath=ancestor::*[@data-atf-node-role='request' or @data-atf-node-role='validation'][1]")
                .locator("details[class~='atf-ui-failure-evidence'][data-atf-evidence-status='fail']");
    }
    Locator screenValidationScreenshotLinks(Locator card) {
        // Repeated visual states are rendered as inline links carrying the
        // canonical screenshot reference. They remain user-facing links, even
        // when the Base64 payload is intentionally deduplicated.
        return evidence.screenshotLinks(screenValidationActionSequences(card));
    }
    Locator screenValidationScreenshotLabels(Locator card) {
        return screenValidationScreenshotLinks(card).locator("span[class~='badge-gradient-primary']");
    }
    Locator screenValidationSeparators(Locator scope) {
        return scope.locator("hr, [class~='atf-ui-zone-separator']");
    }
}


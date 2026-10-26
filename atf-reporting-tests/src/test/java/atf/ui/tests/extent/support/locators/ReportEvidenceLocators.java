package atf.ui.tests.extent.support.locators;
import atf.api.reporting.ui.contract.ReportUiContract;
import atf.api.reporting.api.contract.ApiReportEvidenceField;
import atf.api.reporting.theme.tokens.AtfReportCssSelectors;

import atf.api.ui.playwright.AtfLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.LinkedHashMap;
import java.util.Map;

/** Locator boundary for request/response and UI evidence disclosures. */
final class ReportEvidenceLocators {
    private static final String FAILURE_MARKER = "❌";
    private static final String TESTS_VIEW = ReportLocatorSupport.idSelector(
            AtfReportCssSelectors.Surface.TEST_VIEW);
    private static final String TEST_CONTENT_DETAIL =
            AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector();
    private static final String VISIBLE_TEST_CONTENT_DETAIL =
            AtfReportCssSelectors.Component.TEST_CONTENT_DETAIL.attributeSelector() + ":visible";
    private static final String RESPONSE_TABLE = "details" + classToken("response-evidence") + " "
            + elementClass("table", "helm-trace-table");
    private static final String HTTP_EVIDENCE_DISCLOSURES = "details" + classToken("step-detail")
            + classToken("request-evidence") + ", details" + classToken("step-detail")
            + classToken("response-evidence");
    Locator httpEvidenceDisclosures(Locator scope) { return scope.locator(HTTP_EVIDENCE_DISCLOSURES); }
    Locator requestDetails(Locator request) { return request.locator(elementClass("table", "helm-request-table")); }
    Locator responseDetails(Locator request) { return request.locator(RESPONSE_TABLE); }
    Locator executionEvidence(Locator request) { return request.locator(elementClass("table", "execution-table")); }
    Locator requestTable(Locator scope) { return scope.locator(elementClass("table", "helm-request-table")); }
    Locator responseTable(Locator scope) { return scope.locator(RESPONSE_TABLE); }
    Locator executionTable(Locator scope) { return scope.locator(elementClass("table", "execution-table")); }
    Locator visibleRequestTable(Locator scope) { return scope.locator(elementClass("table", "helm-request-table") + ":visible"); }
    Locator visibleResponseTable(Locator scope) { return scope.locator(RESPONSE_TABLE + ":visible"); }
    Locator visibleExecutionTable(Locator scope) { return scope.locator(elementClass("table", "execution-table") + ":visible"); }
    Locator retryHistoryTable(Locator scope) { return scope.locator(elementClass("table", "retry-history-table") + ":visible"); }
    Locator retryHistoryRows(Locator table) { return table.locator("tbody tr"); }
    Locator retryHistoryCells(Locator row) { return row.locator("td"); }
    Locator tableColumns(Locator table) { return table.locator("col"); }

    Locator requestNodeByName(Locator scope, String requestName) {
        return scope.locator(VISIBLE_TEST_CONTENT_DETAIL + " "
                        + "[data-atf-node-role='phase'][data-atf-phase-name='test-body'] "
                        + "[data-atf-node-role='request']")
                .filter(new Locator.FilterOptions().setHasText(requestName)).first();
    }

    Locator requestNodeHeader(Locator request) {
        return request.locator(":scope > " + classToken("card-header"));
    }
    Locator requestNodeToggle(Locator request) {
        return request.locator(":scope > " + classToken("card-header") + " " + classToken("node"));
    }
    AtfLocator requestNodeToggleAction(Locator request) {
        return new AtfLocator(requestNodeToggle(request),
                nodeScopedSelector(nodeId(requestNodeHeader(request)), "> "
                        + classToken("card-header") + " " + classToken("node")));
    }
    String requestTitle(Locator request) { return requestNodeToggle(request).innerText().trim(); }

    String requestUrl(Locator request) { return cellValue(requestDetails(request), ApiReportEvidenceField.REQUEST_URL); }

    Map<String, String> requestFields(Locator request) {
        return fieldValues(requestDetails(request));
    }

    Map<String, String> responseFields(Locator request) {
        return fieldValues(responseDetails(request));
    }

    Map<String, String> executionFields(Locator request) {
        return fieldValues(executionEvidence(request));
    }

    int responseStatus(Locator request) {
        int status = statusFromRows(responseDetails(request).locator("tbody tr"), ApiReportEvidenceField.HTTP_STATUS);
        return status >= 0 ? status : statusFromRows(executionEvidence(request).locator("tbody tr"),
                ApiReportEvidenceField.RESPONSE_STATUS);
    }

    String responseUrl(Locator request) { return cellValue(executionEvidence(request), ApiReportEvidenceField.RESPONSE_URL); }

    Locator requestJsonBodies(Locator request) { return request.locator(classToken(ReportUiContract.JSON_REQUEST_CLASS)); }
    Locator validationForOwner(Locator scope, atf.api.reporting.contract.evidence.ReportEvidenceOwner owner) {
        return scope.locator("div[data-atf-node-role='validation']:has([data-atf-evidence-owner='"
                + owner.attributeValue() + "'])").first();
    }
    Locator responseJsonBodies(Locator request) { return request.locator(classToken(ReportUiContract.JSON_RESPONSE_CLASS)); }
    Locator requestJsonBody(Locator scope) {
        return requestJsonBodyElements(scope).first();
    }
    Locator requestJsonBodyElements(Locator scope) {
        return scope.locator(classToken(ReportUiContract.JSON_REQUEST_CLASS) + " "
                + elementClass("pre", "json-code") + ", "
                + classToken(ReportUiContract.JSON_REQUEST_CLASS) + " "
                + elementClass("pre", "code-block") + ", "
                + classToken(ReportUiContract.JSON_REQUEST_CLASS) + " "
                + elementClass("textarea", "code-block"));
    }
    Locator responseJsonBody(Locator scope) {
        return responseJsonBodyElements(scope).first();
    }
    Locator responseJsonBodyElements(Locator scope) {
        return scope.locator(classToken(ReportUiContract.JSON_RESPONSE_CLASS) + " "
                + elementClass("pre", "json-code") + ", "
                + classToken(ReportUiContract.JSON_RESPONSE_CLASS) + " "
                + elementClass("pre", "code-block") + ", "
                + classToken(ReportUiContract.JSON_RESPONSE_CLASS) + " "
                + elementClass("textarea", "code-block"));
    }

    Locator requestEvidenceDisclosure(Locator request) {
        return request.locator("details" + classToken("request-evidence") + " > summary");
    }
    Locator responseEvidenceDisclosure(Locator request) {
        return request.locator("details" + classToken("response-evidence") + " > summary");
    }
    Locator executionEvidenceDisclosure(Locator request) {
        return request.locator("details" + classToken("execution-evidence") + " > summary");
    }
    void moveRequestEvidenceToOwner(Locator owner) {
        moveDisclosureToOwner(owner, "request-evidence");
    }
    void moveResponseEvidenceToOwner(Locator owner) {
        moveDisclosureToOwner(owner, "response-evidence");
    }
    AtfLocator requestEvidenceDisclosureAction(Locator request) {
        return disclosureAction(request, requestEvidenceDisclosure(request),
                "details" + classToken("request-evidence") + " > summary");
    }
    AtfLocator responseEvidenceDisclosureAction(Locator request) {
        return disclosureAction(request, responseEvidenceDisclosure(request),
                "details" + classToken("response-evidence") + " > summary");
    }
    AtfLocator executionEvidenceDisclosureAction(Locator request) {
        return disclosureAction(request, executionEvidenceDisclosure(request),
                "details" + classToken("execution-evidence") + " > summary");
    }
    Locator parentDisclosure(Locator summary) { return summary.locator("xpath=.."); }
    Locator durationBadge(Locator node) {
        return node.locator(classToken("node-info") + " " + classToken("badge-default")).first();
    }

    Locator actionSequences(Locator scope) { return scope.locator(classToken("atf-ui-action-sequence")); }
    Locator screenshotLinks(Locator scope) {
        return scope.locator("a" + classToken("atf-ui-action-screenshot-link"));
    }
    Locator base64ScreenshotLinks(Locator scope) {
        return scope.locator("a" + classToken("atf-ui-action-screenshot-link")
                + "[data-atf-screenshot=\"base64\"]");
    }
    Locator dataUriScreenshotLinks(Locator scope) {
        return scope.locator("a" + classToken("atf-ui-action-screenshot-link")
                + "[href^=\"data:image/png;base64,\"]");
    }
    Locator featherlightScreenshotLinks(Locator scope) {
        return scope.locator("a" + classToken("atf-ui-action-screenshot-link")
                + "[data-featherlight=\"image\"]");
    }
    Locator nestedScreenshotImages(Locator scope) {
        return scope.locator("a" + classToken("atf-ui-action-screenshot-link") + " img");
    }
    Locator inlineScreenshotImages(Locator screenshotLink) { return screenshotLink.locator("img"); }
    Locator duplicateScreenshotMarkers(Locator scope) {
        return scope.locator(classToken("atf-ui-screenshot-duplicate")
                + "[data-atf-screenshot-ref]");
    }
    Locator actionRows(Locator sequence) { return sequence.locator("tbody tr"); }
    Locator failedActionRows(Locator sequence) {
        return actionRows(sequence).filter(new Locator.FilterOptions().setHasText(FAILURE_MARKER));
    }
    Locator failedObservationRows(Locator observations) {
        return actionRows(observations).filter(new Locator.FilterOptions().setHasText(FAILURE_MARKER));
    }
    Locator failedAssertionRows(Locator table) {
        return table.locator("tbody tr").filter(new Locator.FilterOptions().setHasText(FAILURE_MARKER));
    }
    Locator sequenceSummary(Locator sequence) { return sequence.locator(":scope > summary"); }
    Locator actionRowCells(Locator row) { return row.locator("td"); }
    Locator actionIdentityCell(Locator row) {
        return row.locator("td" + classToken("atf-ui-action-identity"));
    }
    Locator actionOperation(Locator identityCell) {
        return identityCell.locator(classToken("atf-ui-action-operation"));
    }
    Locator actionNumber(Locator identityCell) {
        return identityCell.locator(ReportUiContract.ACTION_NUMBER_SELECTOR);
    }
    Locator actionTime(Locator row) {
        return row.locator("td " + classToken("atf-ui-action-time"));
    }
    Locator disclosureSummary(Locator disclosure) { return disclosure.locator(":scope > summary"); }
    Locator visibleDisclosure(Locator scope) { return scope.locator("details:visible").first(); }
    Locator nodeAncestors(Locator scope) { return scope.locator("xpath=ancestor::*[@data-atf-node-role]"); }
    Locator lightbox(Page page) { return page.locator(classToken("featherlight") + ":visible"); }
    Locator lightboxImage(Locator lightbox) {
        return lightbox.locator("img" + classToken("featherlight-image") + ":visible");
    }
    Locator lightboxClose(Locator lightbox) {
        return lightbox.locator(classToken("featherlight-close-icon"));
    }

    private AtfLocator disclosureAction(Locator request, Locator disclosure, String suffix) {
        return new AtfLocator(disclosure,
                nodeScopedSelector(nodeId(requestNodeHeader(request)), suffix));
    }

    private void moveDisclosureToOwner(Locator owner, String disclosureClass) {
        owner.evaluate("(owner, className) => {"
                + "const request = owner.querySelector('[data-atf-node-role=request]');"
                + "const disclosure = request && request.querySelector('details.' + className);"
                + "if (!disclosure) throw new Error(className + ' fixture is missing');"
                + "owner.append(disclosure);"
                + "}", disclosureClass);
    }

    private String cellValue(Locator table, ApiReportEvidenceField field) {
        Locator rows = table.locator("tbody tr");
        for (int index = 0; index < rows.count(); index++) {
            Locator row = rows.nth(index);
            if (field.matches(row.locator("td").first().innerText())) {
                return row.locator("td").nth(1).innerText().trim();
            }
        }
        return "";
    }

    private Map<String, String> fieldValues(Locator table) {
        Map<String, String> values = new LinkedHashMap<>();
        Locator rows = table.locator("tbody tr");
        for (int index = 0; index < rows.count(); index++) {
            Locator cells = rows.nth(index).locator("td");
            if (cells.count() < 2) continue;
            String field = cells.first().innerText().trim();
            if (!field.isBlank()) values.put(field, cells.nth(1).innerText().trim());
        }
        return Map.copyOf(values);
    }

    private int statusFromRows(Locator rows, ApiReportEvidenceField field) {
        for (int index = 0; index < rows.count(); index++) {
            Locator row = rows.nth(index);
            if (!field.matches(row.locator("td").first().innerText())) continue;
            try {
                return Integer.parseInt(row.locator("td").nth(1).innerText().trim());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static String nodeId(Locator header) {
        Locator node = header.locator("xpath=..")
                .locator(":scope > " + classToken("card-header") + " > "
                        + classToken("card-title") + " > " + classToken("node"));
        String value = node.getAttribute("id");
        if (value == null || value.isBlank()) throw new IllegalStateException("report node id is missing");
        return value;
    }

    private static String nodeScopedSelector(String nodeId, String suffix) {
        String safeNodeId = ReportLocatorSupport.cssAttribute(nodeId);
        return TESTS_VIEW + " " + VISIBLE_TEST_CONTENT_DETAIL + " div" + classToken("card")
                + ":has(> " + classToken("card-header") + " > " + classToken("card-title") + " > "
                + classToken("node") + "[id='"
                + safeNodeId + "']) " + suffix;
    }

    private static String classToken(String className) {
        return ReportLocatorSupport.classSelector(className);
    }

    private static String elementClass(String element, String className) {
        return ReportLocatorSupport.classSelector(element, className);
    }
}

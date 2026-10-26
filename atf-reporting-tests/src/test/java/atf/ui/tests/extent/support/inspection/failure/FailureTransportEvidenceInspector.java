package atf.ui.tests.extent.support.inspection.failure;

import atf.ui.dom.HtmlElement;
import atf.reporting.audit.RetryEvidenceValidator;
import atf.ui.playwright.AtfLocator;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;

import atf.core.http.request.HttpMethod;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.reporting.audit.RetryEvidenceSnapshot;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Audits transport evidence on failed write requests only.  It deliberately
 * stays separate from the phase-display inspector: request fields, JSON and
 * retry reconciliation are transport facts, not lifecycle geometry.
 */
public final class FailureTransportEvidenceInspector {
    private final UiAuditFlow auditFlow;
    private final ReportUiLocators locators;
    private final RetryEvidenceValidator retryValidator = new RetryEvidenceValidator();

    public FailureTransportEvidenceInspector(UiAuditFlow auditFlow, ReportUiLocators locators) {
        this.auditFlow = Objects.requireNonNull(auditFlow, "auditFlow");
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public Observation inspect(Page page, Locator phase) {
        List<String> violations = new ArrayList<>();
        int writeFailures = 0;
        int metadataComplete = 0;
        int jsonComplete = 0;
        int retryAudited = 0;
        int retryValid = 0;

        Locator requests = locators.requestsInPhase(phase);
        for (int index = 0; index < requests.count(); index++) {
            Locator request = requests.nth(index);
            if (!request.isVisible() || locators.failedRequestBadge(request).count() == 0) continue;
            String title = locators.requestTitle(request);
            String method = method(title);
            if (!method.equals(HttpMethod.POST.value()) && !method.equals(HttpMethod.PUT.value())) continue;
            writeFailures++;
            openRequest(page, request);

            openDisclosure(page, request, locators.requestEvidenceDisclosure(request),
                    locators.requestEvidenceDisclosureAction(request), "request details");
            openDisclosure(page, request, locators.responseEvidenceDisclosure(request),
                    locators.responseEvidenceDisclosureAction(request), "response details");
            openDisclosure(page, request, locators.executionEvidenceDisclosure(request),
                    locators.executionEvidenceDisclosureAction(request), "execution evidence");

            Map<String, String> requestFields = normalized(locators.requestFields(request));
            Map<String, String> responseFields = normalized(locators.responseFields(request));
            Map<String, String> executionFields = normalized(locators.executionFields(request));
            List<String> missing = missing(requestFields, responseFields, executionFields);
            if (missing.isEmpty()) metadataComplete++;
            else violations.add(title + " missing transport metadata: " + String.join(", ", missing));

            String requestJson = body(locators.requestJsonBody(request));
            String responseJson = body(locators.responseJsonBody(request));
            if (hasJsonDocument(requestJson) && hasJsonDocument(responseJson)) jsonComplete++;
            else violations.add(title + " must expose request and response JSON bodies");

            RetryEvidenceSnapshot retry = retrySnapshot(request, responseFields, executionFields);
            if (retry.hasRetry()) {
                retryAudited++;
                List<String> retryViolations = retryValidator.validate(retry);
                if (retryViolations.isEmpty()) retryValid++;
                else retryViolations.forEach(violation -> violations.add(title + ": " + violation));
            }
            closeRequest(request);
        }
        return new Observation(writeFailures, metadataComplete, jsonComplete,
                retryAudited, retryValid, violations);
    }

    private void openRequest(Page page, Locator request) {
        if (!"true".equals(request.getAttribute("data-atf-node-open"))) {
            auditFlow.actions().click(locators.requestNodeToggleAction(request),
                    "open failed " + method(locators.requestTitle(request)) + " request evidence");
            if (!"true".equals(request.getAttribute("data-atf-node-open"))) {
                throw new AssertionError("failed request did not open: " + locators.requestTitle(request));
            }
        }
    }

    private void closeRequest(Locator request) {
        if ("true".equals(request.getAttribute("data-atf-node-open"))) {
            auditFlow.actions().click(locators.requestNodeToggleAction(request),
                    "close failed " + method(locators.requestTitle(request)) + " request evidence");
        }
    }

    private void openDisclosure(Page page, Locator request, Locator summary,
                                AtfLocator action, String name) {
        if (summary.count() == 0) return;
        Locator disclosure = locators.parentDisclosure(summary);
        if (disclosure.getAttribute("open") == null) {
            auditFlow.actions().click(action, name + " is expanded");
            auditFlow.actions().waitForDisclosureOpen(page, action, name + " is available");
        }
    }

    private RetryEvidenceSnapshot retrySnapshot(Locator request,
                                                Map<String, String> responseFields,
                                                Map<String, String> executionFields) {
        int attempts = integer(executionFields.get("ATTEMPTS"), 0);
        int retryEvents = integer(executionFields.get("RETRY.EVENTS"), 0);
        int finalStatus = integer(responseFields.get("HTTP STATUS"),
                integer(executionFields.get("RESPONSE.STATUS"), -1));
        List<RetryEvidenceSnapshot.Attempt> history = new ArrayList<>();
        Locator table = locators.retryHistoryTable(request);
        Locator rows = locators.retryHistoryRows(table);
        for (int index = 0; index < rows.count(); index++) {
            Locator cells = locators.retryHistoryCells(rows.nth(index));
            if (cells.count() < 5) continue;
            history.add(new RetryEvidenceSnapshot.Attempt(
                    integer(cells.nth(0).innerText(), index + 1),
                    integer(cells.nth(1).innerText(), -1),
                    cells.nth(3).innerText().trim()));
        }
        return new RetryEvidenceSnapshot(attempts, retryEvents, finalStatus, history);
    }

    private static List<String> missing(Map<String, String> request,
                                        Map<String, String> response,
                                        Map<String, String> execution) {
        List<String> result = new ArrayList<>();
        require(request, result, "URL");
        require(request, result, "AUTH PROFILE");
        require(request, result, "AUTH TYPE");
        require(request, result, "REQUEST HEADERS");
        require(response, result, "HTTP STATUS");
        require(response, result, "CONTENT-TYPE");
        require(execution, result, "ATTEMPTS");
        require(execution, result, "RETRY.EVENTS");
        require(execution, result, "RESPONSE.STATUS");
        require(execution, result, "RESPONSE.URL");
        return result;
    }

    private static void require(Map<String, String> fields, List<String> missing, String key) {
        if (!fields.containsKey(key) || fields.get(key).isBlank()) missing.add(key);
    }

    private static Map<String, String> normalized(Map<String, String> values) {
        return values.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                entry -> entry.getKey().trim().toUpperCase(Locale.ROOT),
                entry -> entry.getValue() == null ? "" : entry.getValue().trim(),
                (first, ignored) -> first));
    }

    private static String body(Locator locator) {
        if (locator.count() == 0) return "";
        String tag = locator.evaluate("element => element.tagName").toString();
        return HtmlElement.TEXTAREA.selector().equalsIgnoreCase(tag)
                ? locator.inputValue() : locator.innerText();
    }

    private static boolean hasJsonDocument(String value) {
        String text = value == null ? "" : value.trim();
        return !text.isBlank() && !"null".equals(text);
    }

    private static int integer(String value, int fallback) {
        try { return Integer.parseInt(value == null ? "" : value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String method(String title) {
        return title == null || title.isBlank() ? "" : title.trim().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
    }

    public record Observation(int writeFailureCount, int metadataCompleteCount,
                              int jsonCompleteCount, int retryAuditedCount,
                              int retryValidCount, List<String> violations) {
        public Observation {
            if (writeFailureCount < 0 || metadataCompleteCount < 0 || jsonCompleteCount < 0
                    || retryAuditedCount < 0 || retryValidCount < 0) {
                throw new IllegalArgumentException("transport audit counts must be non-negative");
            }
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        public boolean valid() {
            return writeFailureCount > 0
                    && metadataCompleteCount == writeFailureCount
                    && jsonCompleteCount == writeFailureCount
                    && retryAuditedCount == retryValidCount
                    && violations.isEmpty();
        }
    }
}

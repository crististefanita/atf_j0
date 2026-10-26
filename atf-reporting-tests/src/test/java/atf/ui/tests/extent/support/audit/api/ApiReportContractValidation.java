package atf.ui.tests.extent.support.audit.api;
import atf.ui.tests.extent.support.audit.mock.MockReportContractAuditor;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.data.UiTestData;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Read-only semantic validation of API request cards rendered by Extent.
 * Selectors stay in {@link ReportUiLocators}; this class only turns DOM
 * evidence into contract results and never performs navigation.
 */
public final class ApiReportContractValidation {
    private static final String LIFECYCLE_OWNER = UiTestData.LIFECYCLE_OWNER;
    private final ReportUiLocators locators;
    private final ApiJsonEvidenceAuditor jsonAuditor;
    private final MockReportContractAuditor mockAuditor;

    public ApiReportContractValidation(ReportUiLocators locators,
                                       MockReportContractAuditor mockAuditor) {
        this.locators = Objects.requireNonNull(locators, "locators");
        this.jsonAuditor = new ApiJsonEvidenceAuditor();
        this.mockAuditor = Objects.requireNonNull(mockAuditor, "mockAuditor");
    }

    public ApiReportContractValidation(atf.ui.tests.extent.support.api.ApiReportUiFlow flow,
                                       MockReportContractAuditor mockAuditor) {
        this(Objects.requireNonNull(flow, "flow").locators(), mockAuditor);
    }

    public Result inspect(ApiReportUiFlow.Owner owner) {
        List<String> violations = new ArrayList<>();
        List<RequestAudit> requestsAudited = new ArrayList<>();
        int requestCount = 0;
        Locator scenarios = locators.scenarios(owner.detail());
        for (int scenarioIndex = 0; scenarioIndex < scenarios.count(); scenarioIndex++) {
            Locator scenario = scenarios.nth(scenarioIndex);
            for (int phaseIndex = 0; phaseIndex < locators.phases(scenario).count(); phaseIndex++) {
                Locator phase = locators.phases(scenario).nth(phaseIndex);
                Locator requests = locators.requestsInPhase(phase);
                for (int requestIndex = 0; requestIndex < requests.count(); requestIndex++) {
                    Locator request = requests.nth(requestIndex);
                    requestCount++;
                    String title = locators.requestTitle(request);
                    String url = locators.requestUrl(request);
                    ApiJsonEvidenceAuditor.Result json = jsonAuditor.inspect(request);
                    MockReportContractAuditor.Result mock = mockAuditor.inspect(
                            title, url,
                            locators.responseStatus(request), json,
                            new MockReportContractAuditor.RenderedEvidence(
                                    locators.requestFields(request),
                                    locators.responseFields(request),
                                    locators.executionFields(request)));
                    if (!json.compliant()) {
                        violations.add(owner.name() + " / " + title
                                + " JSON: " + json.violations());
                    }
                    if (!mock.compliant()) {
                        violations.add(owner.name() + " / " + title
                                + " mock: " + mock.violations());
                    }
                    requestsAudited.add(new RequestAudit(title, url,
                            locators.responseStatus(request), json.requestHasPayload(),
                            json.responseHasPayload(), json.compliant() && mock.compliant()));
                }
            }
        }
        if (requestCount == 0) {
            violations.add(owner.name() + " rendered no visible API request cards");
        }
        return new Result(owner.name(), requestCount, requestsAudited, violations,
                locators.skippedOutcomeNodes(owner.detail()).count());
    }

    public List<AtfAssertion> assertions(Result result) {
        return result.validation().assertions();
    }

    public record Result(String ownerName, int requestCount, List<RequestAudit> requestsAudited,
                         List<String> violations, int skippedOutcomeCount) {
        public Result(String ownerName, int requestCount, List<String> violations) {
            this(ownerName, requestCount, List.of(), violations, 0);
        }

        public Result(String ownerName, int requestCount, List<RequestAudit> requestsAudited,
                      List<String> violations) {
            this(ownerName, requestCount, requestsAudited, violations, 0);
        }

        public Result {
            ownerName = Objects.requireNonNull(ownerName, "ownerName");
            requestsAudited = List.copyOf(Objects.requireNonNull(requestsAudited, "requestsAudited"));
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
            if (skippedOutcomeCount < 0) {
                throw new IllegalArgumentException("skippedOutcomeCount must not be negative");
            }
        }

        public boolean compliant() { return requestCount > 0 && violations.isEmpty(); }

        public ValidationResult validation() {
            List<AtfAssertion> checks = new ArrayList<>();
            checks.add(ReportAuditChecks.check(
                    ownerName + " API requests",
                    "all rendered requests match mock status and JSON trees",
                    requestCount + " requests; "
                            + (violations.isEmpty() ? "no violations" : violations),
                    compliant()));
            if (LIFECYCLE_OWNER.equals(ownerName)) {
                checks.add(ReportAuditChecks.check(
                        "API lifecycle skip outcome",
                        "exactly one rendered TEST BODY SKIP branch",
                        skippedOutcomeCount == 1
                                ? "TEST BODY status SKIP (1 rendered branch)"
                                : "TEST BODY SKIP branches=" + skippedOutcomeCount,
                        skippedOutcomeCount == 1));
            }
            return ValidationResult.of(checks);
        }

        public String routeSummary() {
            return requestsAudited.stream().map(RequestAudit::summary).toList().toString();
        }
    }

    public record RequestAudit(String title, String url, int status,
                               boolean requestHasPayload, boolean responseHasPayload,
                               boolean compliant) {
        public String summary() {
            String identity = (title == null || title.isBlank()) ? "<untitled request>" : title;
            return identity + " -> HTTP " + status
                    + " [requestJson=" + (requestHasPayload ? "present" : "null")
                    + ", responseJson=" + (responseHasPayload ? "present" : "null")
                    + ", " + (compliant
                            ? AtfReportStatus.PASS.displayLabel()
                            : AtfReportStatus.FAIL.displayLabel()) + "]";
        }
    }
}

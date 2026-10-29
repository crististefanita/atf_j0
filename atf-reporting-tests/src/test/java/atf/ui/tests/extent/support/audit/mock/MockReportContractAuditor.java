package atf.ui.tests.extent.support.audit.mock;

import atf.mock.AtfMockConfiguration;
import atf.mock.MockRoute;
import atf.reporting.api.contract.ApiReportContractAuditor;
import atf.reporting.api.contract.ApiReportContractCatalog;
import atf.reporting.api.contract.ApiReportContractInput;
import atf.reporting.api.contract.ApiReportRouteContract;
import atf.ui.tests.extent.support.audit.api.ApiJsonEvidenceAuditor;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Test-only adapter from the ATF mock catalog and Playwright evidence to the
 * renderer-neutral reporting contract engine.
 *
 * <p>No contract rule lives here. Keeping this bridge in the UI test source
 * set prevents Playwright or atf-mock from leaking into the reporting
 * library, while preserving the current UI audit flow.</p>
 */
public final class MockReportContractAuditor {
    private final Map<ApiReportRouteContract, MockRoute> sourceRoutes = new IdentityHashMap<>();
    private final ApiReportContractAuditor delegate;

    public MockReportContractAuditor(AtfMockConfiguration mock) {
        List<ApiReportRouteContract> contracts = Objects.requireNonNull(mock, "mock").routes().stream()
                .map(route -> {
                    var contract = toContract(route);
                    sourceRoutes.put(contract, route);
                    return contract;
                }).toList();
        ApiReportContractCatalog catalog = () -> contracts;
        this.delegate = new ApiReportContractAuditor(catalog);
    }

    public Result inspect(String requestTitle, int actualStatus,
                          ApiJsonEvidenceAuditor.Result evidence) {
        return inspect(requestTitle, null, actualStatus, evidence);
    }

    public Result inspect(String requestTitle, String requestUrl, int actualStatus,
                          ApiJsonEvidenceAuditor.Result evidence) {
        return inspect(requestTitle, requestUrl, actualStatus, evidence, RenderedEvidence.empty());
    }

    public Result inspect(String requestTitle, String requestUrl, int actualStatus,
                          ApiJsonEvidenceAuditor.Result evidence, RenderedEvidence rendered) {
        ApiJsonEvidenceAuditor.Result json = evidence == null
                ? new ApiJsonEvidenceAuditor.Result("", "", List.of()) : evidence;
        RenderedEvidence safeRendered = rendered == null ? RenderedEvidence.empty() : rendered;
        ApiReportContractAuditor.Result result = delegate.inspect(new ApiReportContractInput(
                requestTitle, requestUrl, actualStatus,
                json.requestBody(), json.responseBody(), json.violations(),
                safeRendered.requestFields(), safeRendered.responseFields(), safeRendered.executionFields(),
                json.requestAvailability(), json.responseAvailability()));
        Optional<MockRoute> route = result.route().map(contract -> Objects.requireNonNull(sourceRoutes.get(contract),
                "Resolved contract has no source route"));
        return new Result(requestTitle, route, result.violations());
    }

    private static ApiReportRouteContract toContract(MockRoute route) {
        return new ApiReportRouteContract(route.method(), route.path(), route.status(), route.body(),
                route.headers(), route.authProfile(), route.queryParameters(), route.requiredHeaders(),
                route.requestBodyMode(), route.expectedRequestBody());
    }

    public record Result(String requestTitle, Optional<MockRoute> route, List<String> violations) {
        public Result {
            requestTitle = requestTitle == null ? "" : requestTitle;
            route = route == null ? Optional.empty() : route;
            violations = violations == null ? List.of() : List.copyOf(violations);
        }

        public boolean compliant() {
            return violations.isEmpty();
        }
    }

    public record RenderedEvidence(Map<String, String> requestFields,
                                   Map<String, String> responseFields,
                                   Map<String, String> executionFields) {
        public RenderedEvidence {
            requestFields = requestFields == null ? Map.of() : Map.copyOf(requestFields);
            responseFields = responseFields == null ? Map.of() : Map.copyOf(responseFields);
            executionFields = executionFields == null ? Map.of() : Map.copyOf(executionFields);
        }

        public static RenderedEvidence empty() {
            return new RenderedEvidence(Map.of(), Map.of(), Map.of());
        }
    }
}

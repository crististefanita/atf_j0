package atf.ui.tests.extent.component.plan;
import atf.core.testing.api.ApiScenarioSelection;
import atf.reporting.ui.audit.plan.ReportProjection;
import atf.reporting.api.ReportScenarioSelection;
import atf.reporting.contract.AtfReportStatus;
import atf.ui.tests.extent.support.data.UiTestData;

import atf.mock.AtfMockConfiguration;
import atf.mock.MockRoute;
import atf.core.http.request.HttpMethod;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Renderer-neutral expected projection for one API collection.
 *
 * <p>The browser observes the report; it never becomes the source of the
 * expected owner or route set. Those expectations are derived here from the
 * selected API families and the mock catalog.</p>
 */
public record ApiScenarioManifest(List<String> families,
                                  List<String> expectedOwnerNames,
                                  List<RouteExpectation> routes,
                                  List<ApiScenarioSelection> selectedTests) {
    public ApiScenarioManifest {
        families = List.copyOf(Objects.requireNonNull(families, "families"));
        expectedOwnerNames = List.copyOf(Objects.requireNonNull(expectedOwnerNames,
                "expectedOwnerNames"));
        routes = List.copyOf(Objects.requireNonNull(routes, "routes"));
        selectedTests = List.copyOf(Objects.requireNonNull(selectedTests, "selectedTests"));
    }

    public static ApiScenarioManifest from(Collection<String> selectedFamilies,
                                            AtfMockConfiguration mock) {
        Objects.requireNonNull(selectedFamilies, "selectedFamilies");
        Objects.requireNonNull(mock, "mock");
        List<String> families = selectedFamilies.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (families.isEmpty()) {
            throw new IllegalArgumentException("At least one API family is required for the manifest");
        }
        List<String> owners = families.stream()
                .map(UiTestData.ApiFamily::fromKey)
                .map(optional -> optional.orElseThrow(() ->
                        new IllegalArgumentException("Unknown API family in manifest: " + families)))
                .map(UiTestData.ApiFamily::ownerName)
                .toList();
        return new ApiScenarioManifest(families, owners,
                mock.routes().stream().map(RouteExpectation::from).toList(), List.of());
    }

    public static ApiScenarioManifest from(Collection<String> selectedFamilies,
                                            AtfMockConfiguration mock,
                                            Collection<ApiScenarioSelection> selectedTests) {
        ApiScenarioManifest base = from(selectedFamilies, mock);
        return new ApiScenarioManifest(base.families(), base.expectedOwnerNames(),
                base.routes(), List.copyOf(Objects.requireNonNull(selectedTests, "selectedTests")));
    }

    public long writeRouteCount() {
        return routes.stream().filter(RouteExpectation::isJsonWrite).count();
    }

    public ReportProjection expectedProjection() {
        return ReportProjection.from(selectedTests.stream().map(selection ->
                new ReportScenarioSelection(selection.ownerType(), selection.method(), selection.family(),
                        selection.reportOwner(), selection.scenarioTitle(), selection.reportScenarioTitles(),
                        selection.reportScenarioStatuses().stream()
                                .map(status -> AtfReportStatus.valueOf(status.name())).toList())).toList());
    }

    public boolean writeRoutesHaveJsonResponses() {
        return routes.stream().filter(RouteExpectation::isJsonWrite)
                .allMatch(route -> route.responseBody() != null
                        && !route.responseBody().isBlank());
    }

    public record RouteExpectation(String method, String path, String identity,
                                   int responseStatus, String responseBody,
                                   Map<String, String> responseHeaders,
                                   String requestBodyMode,
                                   String expectedRequestBody) {
        public RouteExpectation {
            method = Objects.requireNonNull(method, "method");
            path = Objects.requireNonNull(path, "path");
            identity = Objects.requireNonNull(identity, "identity");
            responseBody = responseBody == null ? "" : responseBody;
            responseHeaders = Map.copyOf(Objects.requireNonNull(responseHeaders,
                    "responseHeaders"));
            requestBodyMode = Objects.requireNonNull(requestBodyMode, "requestBodyMode");
        }

        static RouteExpectation from(MockRoute route) {
            return new RouteExpectation(route.method(), route.path(), route.key(),
                    route.status(), route.body(), route.headers(),
                    route.requestBodyMode(), route.expectedRequestBody());
        }

        public boolean isJsonWrite() {
            return HttpMethod.POST.value().equals(method) || HttpMethod.PUT.value().equals(method);
        }
    }
}

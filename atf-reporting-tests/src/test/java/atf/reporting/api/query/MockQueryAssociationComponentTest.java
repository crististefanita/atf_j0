package atf.reporting.api.query;

import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.mock.*;
import atf.ui.tests.extent.support.audit.api.ApiJsonEvidenceAuditor;
import atf.ui.tests.extent.support.audit.mock.MockReportContractAuditor;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The returned source is the selected catalog object, never a route found by its display label. */
@AtfComponentTest
class MockQueryAssociationComponentTest {
    @Test void prefixNamesDoNotLoseTheSourceRoute() {
        var route = route(ApiQueryParameters.fromMap(Map.of("a", "z", "a-", "b")), "{}");
        var result = inspect(List.of(route), "a=z&a-=b", "{}");
        assertTrue(result.compliant(), result.violations()::toString); assertSame(route, result.route().orElseThrow());
    }

    @Test void collidingLabelsCannotReassignTheAuditedExchange() {
        var first = route(ApiQueryParameters.fromMap(Map.of("a", "1&b=2")), "{\"owner\":\"first\"}");
        var second = route(ApiQueryParameters.fromMap(Map.of("a", "1", "b", "2")), "{\"owner\":\"second\"}");
        assertEquals(first.key(), second.key(), "Legacy display labels remain compatible and are not identity");
        for (var routes : List.of(List.of(first, second), List.of(second, first))) {
            var result = inspect(routes, "a=1&b=2", "{\"owner\":\"second\"}");
            assertTrue(result.compliant(), result.violations()::toString); assertSame(second, result.route().orElseThrow());
            assertSame(first, inspect(routes, "a=1%26b%3D2", "{\"owner\":\"first\"}").route().orElseThrow());
        }
    }

    @Test void repeatedQueryPairsRemainRequiredAcrossTheRealMockAdapter() {
        var source = route(ApiQueryParameters.of(new ApiQueryParameter("tag", "red"), new ApiQueryParameter("tag", "blue")), "{}");
        var wrong = inspect(List.of(source), "tag=blue", "{}");
        assertFalse(wrong.compliant()); assertSame(source, wrong.route().orElseThrow());
        var correct = inspect(List.of(source), "tag=blue&tag=red&empty=", "{}");
        assertTrue(correct.compliant(), correct.violations()::toString); assertSame(source, correct.route().orElseThrow());
    }

    @Test void anUnknownOperationDoesNotBorrowAnUnrelatedSource() {
        var source = route(ApiQueryParameters.empty(), "{}");
        var auditor = auditor(List.of(source));
        var result = auditor.inspect("GET /other", "https://query.test/other", 200,
                new ApiJsonEvidenceAuditor.Result("", "{}", List.of()));
        assertFalse(result.compliant()); assertTrue(result.route().isEmpty());
    }

    private static MockReportContractAuditor.Result inspect(List<MockRoute> routes, String query, String body) {
        return auditor(routes).inspect("GET /query", "https://query.test/query?" + query, 200,
                new ApiJsonEvidenceAuditor.Result("", body, List.of()));
    }
    private static MockReportContractAuditor auditor(List<MockRoute> routes) {
        return new MockReportContractAuditor(new AtfMockConfiguration(routes,
                new MockRoute("*", "/*", 500, "{}", Map.of(), Duration.ZERO)));
    }
    private static MockRoute route(ApiQueryParameters query, String body) {
        return new MockRoute("GET", "/query", 200, body, Map.of(), Duration.ZERO, "*", query, Map.of());
    }
}

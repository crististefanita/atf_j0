package atf.reporting.api.query;

import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.reporting.api.*;
import atf.reporting.api.contract.*;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Literal independent query vectors; no renderer or production canonicalizer supplies the expected result. */
@AtfComponentTest
class ReportQueryContractComponentTest {
    private static final ReportQueryParameters REQUIRED = new ReportQueryParameters(List.of(
            new ReportQueryParameter("tag", "red"), new ReportQueryParameter("tag", "blue")));

    @Test void foreignPairCollectionSurvivesConversionWithoutCallingTheLossyView() {
        var source = ApiQueryParameters.of(new ApiQueryParameter("tag", "red"), new ApiQueryParameter("tag", "blue"));
        var actual = ReportQueryParameters.from(source);
        assertEquals(REQUIRED.values(), actual.values());
        assertEquals("tag=red&tag=blue", actual.display());
        assertEquals(Map.of("tag", "blue"), actual.asMap(), "Only the explicit compatibility view collapses names");
        assertThrows(UnsupportedOperationException.class, () -> actual.values().clear());
    }

    @Test void repeatedIdenticalPairsNeedIndependentOccurrences() {
        var pair = new ReportQueryParameter("tag", "red");
        var one = new ReportQueryParameters(List.of(pair));
        var two = new ReportQueryParameters(List.of(pair, pair));
        assertFalse(one.containsAll(two)); assertTrue(two.containsAll(one));
        assertEquals(2, ReportQueryParameters.from(ApiQueryParameters.of(
                new ApiQueryParameter("tag", "red"), new ApiQueryParameter("tag", "red"))).values().size());
    }

    @ParameterizedTest @ValueSource(strings = {"", "https://query.test/query", "https://query.test/query?tag=blue",
            "https://query.test/query?tag=red", "https://query.test/query?different=observed", "http://[invalid"})
    void incompleteOrUnavailableQueryFailsWithOrWithoutRenderedMetadata(String url) {
        for (var fields : List.of(Map.<String, String>of(), Map.of("AUTH PROFILE", "none"))) {
            var input = new ApiReportContractInput("GET /query", url, 200, "", "{}", List.of(), fields, Map.of(), Map.of());
            var result = auditor(REQUIRED).inspect(input);
            assertFalse(result.compliant(), url);
            assertTrue(result.route().isPresent(), "The diagnostic candidate remains inspectable");
            assertEquals(1, result.violations().stream().filter(v -> v.contains("expected query tag")).count(),
                    "Query failure is published once, independently of metadata availability");
        }
    }

    @ParameterizedTest @ValueSource(strings = {"tag=red&tag=blue", "tag=blue&tag=red&extra=allowed", "tag=red&&tag=blue&"})
    void completeQueryPermutationsAndAdditionalParametersRemainEligible(String query) {
        assertTrue(auditor(REQUIRED).inspect(ApiReportContractInput.of("GET /query",
                "https://query.test/query?" + query, 200, "", "{}")).compliant());
    }

    @Test void matchingStatusCannotHideContradictoryQuery() {
        var expected = new ReportQueryParameters(List.of(new ReportQueryParameter("required", "wanted")));
        for (int status : List.of(200, 500)) {
            var route = route(expected, status);
            var result = new ApiReportContractAuditor(() -> List.of(route)).inspect(ApiReportContractInput.of(
                    "GET /query", "https://query.test/query?different=observed", status, "", "{}"));
            assertFalse(result.compliant()); assertEquals(route, result.route().orElseThrow());
        }
    }

    @Test void queryFreeLegacyContractsDoNotRequireAnInventedRequestUrl() {
        for (String url : List.of("", "http://[invalid")) {
            assertTrue(auditor(ReportQueryParameters.empty()).inspect(ApiReportContractInput.of(
                    "GET /query", url, 200, "", "{}")).compliant());
        }
    }

    @Test void encodedDelimitersUnicodeAndEmptyValuesKeepTheirLiteralMeaning() {
        var actual = ReportQueryParameters.fromUri(URI.create("https://query.test/query?key=a%26b%3Dc&empty=&plus=%2B&space=+&label=%C8%99"));
        assertEquals(List.of(new ReportQueryParameter("key", "a&b=c"), new ReportQueryParameter("empty", ""),
                new ReportQueryParameter("plus", "+"), new ReportQueryParameter("space", " "),
                new ReportQueryParameter("label", "ș")), actual.values());
    }

    @Test void aBrokenPairAccessorCannotSilentlyFallBackToAnApparentlyValidMap() {
        var source = new BrokenPairs();
        var error = assertThrows(ReportModelAdapterException.class, () -> ReportQueryParameters.from(source));
        assertSame(source.failure, error.getCause()); assertEquals("values", error.field());
        assertEquals(0, source.mapReads.get());
        assertEquals(Map.of("legacy", "value"), ReportQueryParameters.from(new LegacyMap()).asMap());
    }

    public static final class BrokenPairs {
        final RuntimeException failure = new IllegalStateException("controlled pairs unavailable");
        final AtomicInteger mapReads = new AtomicInteger();
        public List<?> values() { throw failure; }
        public Map<String, String> asMap() { mapReads.incrementAndGet(); return Map.of("tag", "blue"); }
    }
    public static final class LegacyMap { public Map<String, String> asMap() { return Map.of("legacy", "value"); } }

    private static ApiReportContractAuditor auditor(ReportQueryParameters query) {
        return new ApiReportContractAuditor(() -> List.of(route(query, 200)));
    }
    private static ApiReportRouteContract route(ReportQueryParameters query, int status) {
        return new ApiReportRouteContract("GET", "/query", status, "{}", Map.of(), "*", query, Map.of(), "json", null);
    }
}

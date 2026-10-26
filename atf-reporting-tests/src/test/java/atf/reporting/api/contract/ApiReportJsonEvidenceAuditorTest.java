package atf.reporting.api.contract;
import atf.testkit.AtfUnitTest;

import atf.core.http.request.HttpMethod;
import atf.reporting.support.ApiReportTestUrls;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static atf.reporting.support.MockApiReportContractFixtures.ACCEPTED_BODY;
import static atf.reporting.support.MockApiReportContractFixtures.catalog;
import static atf.reporting.support.MockApiReportContractFixtures.responseTreePostRoute;
import static atf.reporting.support.MockApiReportContractFixtures.url;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ApiReportJsonEvidenceAuditorTest {
    @Test
    void matchesConfiguredTreesWithoutDependingOnFormatting() {
        var auditor = new ApiReportJsonEvidenceAuditor(catalog(responseTreePostRoute()));
        String html = """
                <textarea readonly class='code-block'>
                  { &quot;job&quot; : { &quot;state&quot; : &quot;accepted&quot; },
                    &quot;items&quot; : [1, 2] }
                </textarea>
                <pre class="code-block">not an API response</pre>
                """;

        var result = auditor.inspectHtml(html);

        assertAll(
                () -> assertTrue(result.compliant(), () -> result.violations().toString()),
                () -> assertTrue(result.renderedJsonDocuments() >= 1),
                () -> assertTrue(auditor.compareResponse(HttpMethod.POST.value(), url("/demo"), ACCEPTED_BODY)
                        .isEmpty()));
    }

    @Test
    void reportsMissingAndDriftedJsonAsActionableViolations() {
        var auditor = new ApiReportJsonEvidenceAuditor(catalog(responseTreePostRoute()));

        var missing = auditor.inspectHtml("<textarea class='code-block'>{\"other\":true}</textarea>");
        var drift = auditor.compareResponse(HttpMethod.POST.value(), url("/demo"), "{\"job\":{\"state\":\"rejected\"}}");

        assertAll(
                () -> assertTrue(!missing.compliant()),
                () -> assertTrue(missing.violations().getFirst().contains("configured response JSON")),
                () -> assertTrue(drift.getFirst().contains("does not match the catalog")));
    }

    @Test
    void keepsTheCatalogIndependentFromTheRenderedArtifact() {
        var auditor = new ApiReportJsonEvidenceAuditor(() -> java.util.List.of(
                new ApiReportRouteContract(HttpMethod.GET.value(), "/unavailable", 200,
                        "{\"ok\":true}", Map.of(), "*", null, Map.of(), "JSON", null)));

        var result = auditor.compareResponse(HttpMethod.GET.value(),
                ApiReportTestUrls.mock(ApiReportTestUrls.GENERIC_OTHER), "{}");

        assertTrue(result.getFirst().contains("no matching API report contract"));
    }
}

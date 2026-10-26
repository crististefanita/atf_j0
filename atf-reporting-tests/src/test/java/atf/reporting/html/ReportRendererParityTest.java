package atf.reporting.html;

import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.markup.ExtentReportCategoryMarkup;
import atf.reporting.html.HtmlReportCategoryView;
import atf.reporting.html.HtmlReportFailsView;
import atf.reporting.html.HtmlReportSemanticDocument;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the shared semantic tokens used by fallback and Extent projections. */
@AtfComponentTest
class ReportRendererParityTest {
    @Test
    void fallbackAndExtentFailureSurfacesKeepTheSameActionableTokens() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("ParityTests", "failure parity", List.of("api"));
        var request = scenario.addPhase("TEST BODY").addRequest("read", "GET", "/users/1");
        var validation = request.addValidation("Validate response");
        validation.assertions = List.of(new AtfAssertion("HTTP status", "200", "500", false));
        validation.status = AtfReportStatus.FAIL;
        request.complete(AtfReportStatus.FAIL);
        scenario.complete(AtfReportStatus.FAIL);

        String fallbackFails = new HtmlReportFailsView().render(document.snapshot());
        String extentFailure = ReportAssetComposer.extentFailureProjectionScript();

        assertAll(
                () -> assertTrue(fallbackFails.contains("class=\"fail-row\"")),
                () -> assertTrue(fallbackFails.contains("status-fail")),
                () -> assertTrue(extentFailure.contains("atf-failure fail-row")),
                () -> assertTrue(extentFailure.contains("status status-fail")),
                () -> assertTrue(extentFailure.contains("atf-empty-state")));
    }

    @Test
    void categoryFallbackAndExtentShellExposeTheSameEmptyStateVocabulary() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("ParityTests", "category parity", List.of());
        scenario.complete(AtfReportStatus.PASS);

        String fallbackCategory = new HtmlReportCategoryView().render(document.snapshot());
        String extentCategory = ExtentReportCategoryMarkup.emptyView();

        assertAll(
                () -> assertTrue(fallbackCategory.contains("class=\"tag-list\"")),
                () -> assertTrue(extentCategory.contains("atf-empty-state")),
                () -> assertTrue(extentCategory.contains("No category annotations")));
    }
}

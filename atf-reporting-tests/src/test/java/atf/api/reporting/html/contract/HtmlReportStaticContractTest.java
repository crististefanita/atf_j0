package atf.api.reporting.html.contract;
import atf.api.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class HtmlReportStaticContractTest {
    @Test
    void emptyBodyChannelsRequireExplicitCaptureMetadataRatherThanInventedJson() {
        assertTrue(HtmlReportStaticContract.evaluate(
                "<div data-atf-request-body-availability=\"ABSENT\" "
                        + "data-atf-response-body-availability=\"ABSENT\"></div>")
                .satisfies("json evidence channels"));
        assertFalse(HtmlReportStaticContract.evaluate("<div>No bodies</div>")
                .satisfies("json evidence channels"));
    }
    @Test
    void exposesNamedFallbackChecksAndActionableViolations() {
        var evaluation = HtmlReportStaticContract.evaluate(
                "<!doctype html><nav class=\"tabs\"><a data-tab=\"tests\"></a>"
                        + "<a data-tab=\"fails\"></a></nav>");

        assertFalse(evaluation.isCompliant());
        assertTrue(evaluation.satisfies("doctype"));
        assertTrue(evaluation.violations().contains("semantic phase shell"));
        assertTrue(evaluation.violations().contains("request validation roles"));
    }
}

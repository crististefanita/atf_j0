package atf.reporting.evidence.markup;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportingPreviewBudgetTest {
    @Test void boundsTheWholePreviewIncludingTheEllipsis() {
        for (int limit : new int[]{1, 2, 18, 240}) {
            String displayed = ExtentReportNodeSupport.display("x".repeat(400), new ReportingSettings(limit));
            assertEquals(limit, displayed.length());
            assertEquals("x".repeat(limit - 1) + "…", displayed);
        }
    }

    @Test void preservesNullEmptyAndExactBoundaryValues() {
        var settings = ReportingSettings.defaults();
        assertEquals("", ExtentReportNodeSupport.display(null, settings));
        assertEquals("", ExtentReportNodeSupport.display("", settings));
        String exact = "<>&\"".repeat(60);
        assertEquals(exact, ExtentReportNodeSupport.display(exact, settings));
    }

    @Test void keepsTheJsonBodyBudgetIndependent() {
        String body = "x".repeat(400);
        assertEquals(body, ExtentReportNodeSupport.displayBody(body, new ReportingSettings(1, 0)));
        assertTrue(ExtentReportNodeSupport.displayBody(body, new ReportingSettings(1, 18))
                .startsWith("x".repeat(18) + "… [truncated; showing first 18 of "));
    }
}

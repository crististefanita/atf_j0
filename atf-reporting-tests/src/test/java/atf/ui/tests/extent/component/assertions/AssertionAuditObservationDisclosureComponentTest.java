package atf.ui.tests.extent.component.assertions;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.inspection.assertions.AssertionAuditObservationDisclosureProbe;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@AtfComponentTest
@ReportBrowserTest
class AssertionAuditObservationDisclosureComponentTest {
    @Test
    void phaseObservationStaysIndependentFromBulkTreeExpansion() throws Exception {
        String publishedReport = System.getProperty("atf.reporting.observationReport");
        if (publishedReport == null || publishedReport.isBlank()) AssertionAuditObservationDisclosureProbe.verify();
        else AssertionAuditObservationDisclosureProbe.verify(Path.of(publishedReport));
    }
}

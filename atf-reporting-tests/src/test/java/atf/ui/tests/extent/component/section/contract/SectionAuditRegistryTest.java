package atf.ui.tests.extent.component.section.contract;

import atf.reporting.browser.junit.ReportBrowserTest;
import atf.testkit.AtfComponentTest;

import atf.ui.tests.extent.support.section.module.SectionAuditRegistry;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionGherkin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AtfComponentTest
@ReportBrowserTest
class SectionAuditRegistryTest {
    @Test
    void everySupportedSectionHasOnePlugAndPlayModule() {
        SectionAuditRegistry registry = new SectionAuditRegistry();
        for (ExtentSection section : ExtentSection.values()) {
            var module = assertDoesNotThrow(() -> registry.module(section));
            org.junit.jupiter.api.Assertions.assertEquals(
                    ExtentSectionGherkin.proposition(section), module.auditTitle());
            org.junit.jupiter.api.Assertions.assertEquals(
                    ExtentSectionGherkin.thenStatement(section), module.evidenceTitle());
        }
    }
}

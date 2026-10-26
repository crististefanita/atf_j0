package atf.reporting.ui.audit.plan;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class SectionAuditSpecPlanTest {
    @Test
    void acceptsGeneratedJsonPlanWithEvidence() {
        SectionAuditSpec spec = new SectionAuditSpec("generated", UiExtentReportSource.GENERATED_API,
                "", "", Set.of(ExtentSection.OVERVIEW, ExtentSection.EVIDENCE),
                EnumSet.of(ExtentSectionCheck.STRUCTURE, ExtentSectionCheck.JSON_CONTRACT,
                        ExtentSectionCheck.MOCK_RECONCILIATION), ExtentJourney.NONE);

        assertDoesNotThrow(spec::validatePlan);
    }

    @Test
    void rejectsMockCheckOnStaticPlan() {
        SectionAuditSpec spec = new SectionAuditSpec("static", UiExtentReportSource.STATIC_PATH,
                "", "report.property", Set.of(ExtentSection.TESTS),
                EnumSet.of(ExtentSectionCheck.STRUCTURE, ExtentSectionCheck.MOCK_RECONCILIATION),
                ExtentJourney.NONE);

        assertThrows(IllegalArgumentException.class, spec::validatePlan);
    }

    @Test
    void rejectsFailureJourneyWithoutBothSections() {
        SectionAuditSpec spec = new SectionAuditSpec("journey", UiExtentReportSource.STATIC_RESOURCE,
                "reports/report.html", "", Set.of(ExtentSection.FAILS),
                Set.of(ExtentSectionCheck.STRUCTURE), ExtentJourney.FAILURE_NAVIGATION);

        assertThrows(IllegalArgumentException.class, spec::validatePlan);
    }
}

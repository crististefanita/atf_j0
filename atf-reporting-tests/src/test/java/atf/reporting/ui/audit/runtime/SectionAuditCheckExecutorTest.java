package atf.reporting.ui.audit.runtime;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ValidationResult;
import atf.reporting.ui.audit.plan.ExtentJourney;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.plan.UiExtentReportSource;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class SectionAuditCheckExecutorTest {
    @Test
    void oneExecutorProjectsRequestedChecksAndAccountsForExecution() {
        SectionAuditSpec spec = spec(ExtentSection.CATEGORIES,
                ExtentSectionCheck.STRUCTURE, ExtentSectionCheck.STYLE);
        ValidationResult collected = ValidationResult.of(List.of(
                assertion("Categories", true),
                assertion("Linked scenarios", true),
                assertion("Category surface", true),
                assertion("Link cardinality", true)));

        SectionAuditCheckExecutor.Execution execution = new SectionAuditCheckExecutor()
                .execute(ExtentSection.CATEGORIES, spec, collected);

        assertEquals(EnumSet.of(ExtentSectionCheck.STRUCTURE, ExtentSectionCheck.STYLE),
                execution.executedChecks());
        assertTrue(execution.validation().valid());
        assertEquals(List.of("Categories", "Linked scenarios", "Category surface",
                        "Link cardinality"),
                execution.validation().assertions().stream().map(AtfAssertion::name).toList());
    }

    @Test
    void checksBelongingToAnotherSectionAreNotReportedAsExecuted() {
        SectionAuditSpec spec = spec(ExtentSection.SCREEN_VALIDATION,
                ExtentSectionCheck.STRUCTURE, ExtentSectionCheck.JSON_CONTRACT);
        ValidationResult collected = ValidationResult.of(List.of(
                assertion("Screen validation node", true)));

        SectionAuditCheckExecutor.Execution execution = new SectionAuditCheckExecutor()
                .execute(ExtentSection.SCREEN_VALIDATION, spec, collected);

        assertEquals(EnumSet.of(ExtentSectionCheck.STRUCTURE), execution.executedChecks());
        assertTrue(execution.validation().valid());
    }

    private static SectionAuditSpec spec(ExtentSection section, ExtentSectionCheck... checks) {
        return new SectionAuditSpec("executor-test", UiExtentReportSource.STATIC_RESOURCE,
                "report.html", "", EnumSet.of(section), EnumSet.copyOf(List.of(checks)),
                ExtentJourney.NONE);
    }

    private static AtfAssertion assertion(String name, boolean passed) {
        return new AtfAssertion(name, "true", "true", passed);
    }
}

package atf.ui.tests.extent.support.uireport;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.ui.tests.extent.support.uireport.browser.UiReportContractFixture;
import atf.reporting.ui.contract.UiReportContractValidator;
import atf.reporting.ui.contract.UiReportTabSnapshot;
import atf.ui.tests.extent.support.uireport.browser.UiReportTabInspector;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit adapter for the reusable static UI report contract. */
public final class UiReportContractDynamicEngine {
    private UiReportContractDynamicEngine() { }

    public static DynamicNode container(UiReportContractFixture fixture) {
        Objects.requireNonNull(fixture, "fixture");
        UiReportTabInspector inspector = new UiReportTabInspector(fixture);
        UiReportContractValidator validator = new UiReportContractValidator();
        return DynamicContainer.dynamicContainer(fixture.spec().id(),
                fixture.spec().tabs().stream()
                        .map(tab -> DynamicTest.dynamicTest("GIVEN UI Extent report WHEN "
                                        + tab.name() + " is opened THEN its contract is valid",
                                () -> fixture.audit().auditResult(
                                        "UI report — " + tab.name() + " contract",
                                        audit -> {
                                            UiReportTabSnapshot snapshot = inspector.inspect(tab);
                                            ValidationResult result = validator.validate(snapshot,
                                                    fixture.spec());
                                            audit.screen("UI report " + tab.name() + " contract",
                                                    "static UI report / " + tab.name(),
                                                    fixture.audit().capture(fixture.openTab(tab),
                                                            "UI report " + tab.name() + " contract"),
                                                    result.assertions(),
                                                    ReportAuditChecks.status(result.valid()));
                                            assertTrue(result.valid(), result::failureSummary);
                                            return null;
                                        })))
                        .toList());
    }
}

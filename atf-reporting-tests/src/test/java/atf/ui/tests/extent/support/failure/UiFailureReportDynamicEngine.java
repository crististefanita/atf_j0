package atf.ui.tests.extent.support.failure;

import atf.ui.tests.extent.support.inspection.failure.UiFailureReportInspector;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.audit.ReportAuditChecks;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** JUnit adapter for the annotation-driven failure surface matrix. */
public final class UiFailureReportDynamicEngine {
    private UiFailureReportDynamicEngine() { }

    public static DynamicNode container(UiFailureReportFixture fixture) {
        Objects.requireNonNull(fixture, "fixture");
        UiFailureReportInspector inspector = new UiFailureReportInspector(fixture);
        return DynamicContainer.dynamicContainer(fixture.spec().id(),
                fixture.spec().surfaces().stream()
                        .map(surface -> DynamicTest.dynamicTest(
                                "GIVEN a failed " + surface + " WHEN Tests is inspected THEN failure evidence is actionable",
                                () -> fixture.audit().auditResult("UI failure — " + surface,
                                        audit -> {
                                            UiFailureReportInspector.Observation result = inspector.inspect(surface);
                                            audit.screen("UI failure " + surface,
                                                    "Tests / " + surface,
                                                    fixture.audit().capture(fixture.openTests(),
                                                            "UI failure " + surface),
                                                    java.util.List.of(
                                                            ReportAuditChecks.check("Failure channel", "> 0",
                                                                    String.valueOf(result.matchingFailures()),
                                                                    result.matchingFailures() > 0),
                                                            ReportAuditChecks.check("Fails projection", "> 0",
                                                                    String.valueOf(result.failRows()),
                                                                    result.failRows() > 0),
                                                            ReportAuditChecks.check("Collapsed arrival", "0 expanded",
                                                                    String.valueOf(result.expandedOnArrival()),
                                                                    result.expandedOnArrival() == 0),
                                                            ReportAuditChecks.check("Collapsed evidence bodies", "0 visible",
                                                                    String.valueOf(result.visibleClosedEvidenceBodies()),
                                                                    result.visibleClosedEvidenceBodies() == 0),
                                                            ReportAuditChecks.check("Invented HTTP evidence", "0 disclosures",
                                                                    String.valueOf(result.fabricatedHttpDisclosures()),
                                                                    result.fabricatedHttpDisclosures() == 0),
                                                            ReportAuditChecks.check("Sibling isolation", "failure remains in one owner",
                                                                    String.valueOf(result.siblingIsolationClean()),
                                                                    result.siblingIsolationClean())),
                                                    result.valid() ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                                            assertTrue(result.valid(), () -> String.join("; ", result.violations()));
                                            return null;
                                        })))
                        .toList());
    }
}

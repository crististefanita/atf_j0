package atf.reporting.diagnostic;

import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportField;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.CONTRACT_ASSOCIATION;
import static atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE;
import static atf.reporting.model.diagnostic.ReportPhaseDiagnostic.Kind.JUNIT_FAILURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportPhaseDiagnosticTest {
    @Test
    void onlyExplicitDiagnosticKeysArePublicAndTheirSourceIsNotMutated() {
        var fields = new ArrayList<>(List.of(
                new ReportField("API audit completeness", "2 captured"),
                new ReportField("request.method", "POST"),
                new ReportField(" junit FAILURE ", "assertion mismatch"),
                new ReportField("Failure cause", "transport unavailable"),
                new ReportField("API contract association", "contract missing")));
        var diagnostics = ReportPhaseDiagnostic.from("TEST BODY", fields);

        assertEquals(List.of(JUNIT_FAILURE, EXECUTION_FAILURE, CONTRACT_ASSOCIATION),
                diagnostics.stream().map(ReportPhaseDiagnostic::kind).toList());
        assertEquals("assertion mismatch", diagnostics.getFirst().message());
        assertEquals(5, fields.size());
        fields.clear();
        assertEquals(3, diagnostics.size());
        assertThrows(UnsupportedOperationException.class, diagnostics::clear);
    }

    @Test
    void diagnosticsKeepTheOriginatingPhaseVerdictRatherThanTheScenarioVerdict() {
        var fields = List.of(new ReportField("API contract association", "setup note"));
        assertEquals(AtfReportStatus.PASS,
                ReportPhaseDiagnostic.from("SETUP", fields, AtfReportStatus.PASS).getFirst().status());
        assertEquals(AtfReportStatus.FAIL,
                ReportPhaseDiagnostic.from("TEST BODY", fields, AtfReportStatus.FAIL).getFirst().status());
    }

    @Test
    void emptyAndUnrecognizedContextDoesNotInventAReportDiagnostic() {
        assertTrue(ReportPhaseDiagnostic.from("SETUP", List.of()).isEmpty());
        assertTrue(ReportPhaseDiagnostic.from("TEST BODY",
                List.of(new ReportField("audit.error.count", "0"))).isEmpty());
    }

    @Test
    void typedDiagnosticPortUsesTheCanonicalCompatibilityBoundaryWithoutChangingVerdict() {
        var fields = new ArrayList<ReportField>();
        AtfReportPorts.Phase phase = new AtfReportPorts.Phase() {
            @Override public AtfReportPorts.Request startRequest(String title, String method, String path) {
                throw new AssertionError("diagnostics must not create HTTP exchanges");
            }
            @Override public void detail(String name, String value) { fields.add(new ReportField(name, value)); }
            @Override public void complete(AtfReportStatus status) { }
            @Override public void close() { }
        };
        phase.diagnostic(JUNIT_FAILURE, "actual failure");
        assertEquals(List.of(new ReportPhaseDiagnostic("TEST BODY", JUNIT_FAILURE, "actual failure")),
                ReportPhaseDiagnostic.from("TEST BODY", fields));
    }
}

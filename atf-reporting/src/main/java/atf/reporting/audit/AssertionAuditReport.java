package atf.reporting.audit;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.List;
import java.util.Objects;

/** Phase-owned assertion publication; independent of JUnit, Test Kit and the chosen renderer. */
public final class AssertionAuditReport implements AutoCloseable {
    public record Identity(String testClass, String testPackage, String scenario,
                           String method, List<String> tags) {
        public Identity { tags = List.copyOf(tags); }
    }
    private final AtfReportPorts.Session session;
    private final AtfReportPorts.Scenario scenario;
    private final SensitiveDataRedactor redactor;
    private final Identity identity;
    private boolean completed;

    public AssertionAuditReport(AtfReportPorts.Session session, Identity identity, SensitiveDataRedactor redactor) {
        this.session = Objects.requireNonNull(session);
        this.identity = Objects.requireNonNull(identity);
        this.redactor = Objects.requireNonNull(redactor);
        try {
            scenario = session.startScenario(identity.testClass(), identity.testPackage(),
                    identity.scenario(), identity.tags());
        } catch (RuntimeException | Error failure) {
            try (session) { throw failure; }
        }
    }

    /** Called on the full text by the consumer's neutral value-display policy. */
    public String redactField(String name, String text) { return redactor.redactField(name, text); }

    public void phase(String name, List<AtfAssertion> assertions, AtfReportStatus status, String diagnostic) {
        try (var phase = scenario.startPhase(name)) {
            phase.detail("Test method", identity.method());
            if (!assertions.isEmpty()) {
                try (var step = phase.startAssertionStep("Component assertions")) {
                    step.assertions(assertions);
                    step.complete(assertions.stream().allMatch(AtfAssertion::passed)
                            ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
                }
            }
            if (diagnostic != null && !diagnostic.isBlank()) {
                phase.diagnostic(status == AtfReportStatus.SKIP ? ReportPhaseDiagnostic.Kind.EXECUTION_ABORTED
                        : ReportPhaseDiagnostic.Kind.EXECUTION_FAILURE, redactor.redactText(diagnostic));
            }
            phase.complete(status);
        }
    }

    public void complete(AtfReportStatus status) {
        scenario.complete(status);
        completed = true;
    }

    @Override public void close() {
        try (session; scenario) {
            if (!completed) scenario.complete(AtfReportStatus.FAIL);
        }
    }
}

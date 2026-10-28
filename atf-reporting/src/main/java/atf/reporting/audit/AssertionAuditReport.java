package atf.reporting.audit;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.List;
import java.util.Objects;
import atf.reporting.model.timing.ReportPhaseTiming;

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
    private final boolean ownsSession;
    private final AssertionAuditRun.PublicationExecutor executor;
    private boolean completed;
    private volatile boolean closed;

    public AssertionAuditReport(AtfReportPorts.Session session, Identity identity, SensitiveDataRedactor redactor) {
        this(session, identity, redactor, true, AssertionAuditRun.PublicationExecutor.direct());
    }

    static AssertionAuditReport borrowed(AtfReportPorts.Session session, Identity identity, SensitiveDataRedactor redactor,
                                         AssertionAuditRun.PublicationExecutor executor) {
        return new AssertionAuditReport(session, identity, redactor, false, executor);
    }

    private AssertionAuditReport(AtfReportPorts.Session session, Identity identity, SensitiveDataRedactor redactor,
                                 boolean ownsSession, AssertionAuditRun.PublicationExecutor executor) {
        this.session = Objects.requireNonNull(session);
        this.identity = Objects.requireNonNull(identity);
        this.redactor = Objects.requireNonNull(redactor);
        this.ownsSession = ownsSession;
        this.executor = executor;
        scenario = executor.call(() -> {
            synchronized (session) {
                try {
                    return session.startScenario(identity.testClass(), identity.testPackage(),
                        identity.scenario(), identity.tags());
                } catch (RuntimeException | Error failure) {
                    if (ownsSession) { try (session) { throw failure; } }
                    throw failure;
                }
            }
        });
    }

    /** Called on the full text by the consumer's neutral value-display policy. */
    public String redactField(String name, String text) { return redactor.redactField(name, text); }

    public void phase(String name, List<AtfAssertion> assertions, AtfReportStatus status, String diagnostic) {
        phase(name, assertions, status, diagnostic, null);
    }

    public void phase(String name, List<AtfAssertion> assertions, AtfReportStatus status, String diagnostic,
                      ReportPhaseTiming timing) {
        executor.run(() -> {
            synchronized (session) {
                requireOpen();
                publishPhase(name, assertions, status, diagnostic, timing);
            }
        });
    }

    private void publishPhase(String name, List<AtfAssertion> assertions, AtfReportStatus status, String diagnostic,
                              ReportPhaseTiming timing) {
        try (var phase = scenario.startPhase(name)) {
            if (timing != null) phase.executionTiming(timing);
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
        executor.run(() -> {
            synchronized (session) {
                requireOpen();
                scenario.complete(status);
                completed = true;
            }
        });
    }

    @Override public void close() {
        if (closed) return;
        executor.run(() -> {
            synchronized (session) {
                if (closed) return;
                closed = true;
                if (ownsSession) {
                    try (session) { closeScenario(); }
                } else closeScenario();
            }
        });
    }

    private void closeScenario() {
        try (scenario) {
            if (!completed) scenario.complete(AtfReportStatus.FAIL);
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Assertion audit scenario is closed");
    }
}

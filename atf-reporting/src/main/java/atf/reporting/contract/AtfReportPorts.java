package atf.reporting.contract;
import atf.reporting.contract.evidence.ReportEvidenceOwner;

import atf.reporting.inventory.ValidationSink;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.network.ReportNetworkInspector;
import java.nio.file.Path;
import java.util.Collection;

/** Public renderer contract. Consumers provide data; reporting owns rendering. */
public final class AtfReportPorts {
    private AtfReportPorts() { }
    public interface Session extends AutoCloseable {
        Path reportPath();
        Scenario startScenario(String testClass, String scenario, Collection<String> tags);
        default Scenario startScenario(String testClass, String testPackage, String scenario,
                                       Collection<String> tags) { return startScenario(testClass, scenario, tags); }
        void flush();
        @Override void close();
    }
    public interface Scenario extends AutoCloseable {
        String id(); Phase startPhase(String name); void complete(AtfReportStatus status);
        boolean hasWarnings(); @Override void close();
    }
    public interface Phase extends AutoCloseable {
        Request startRequest(String title, String method, String path);
        /** Internal phase context; not a public log band or transport evidence. */
        default void detail(String name, String value) { }
        /** Explicit, scenario-owned execution diagnostic; does not invent a business assertion. */
        default void diagnostic(ReportPhaseDiagnostic.Kind kind, String message) {
            detail(kind.label(), message);
        }
        void complete(AtfReportStatus status); @Override void close();
    }
    public interface Request extends ValidationSink, AutoCloseable {
        void detail(String name, String value); void json(String label, String json);
        /** Typed HTTP body channel; the legacy JSON seam remains source-compatible. */
        default void body(ReportJsonBody body) {
            json(body.label(), body.body());
        }
        default void screenshot(String title, String base64Png) { }
        default void uiActions(Collection<AtfUiActionRecord> actions) { }
        default void execution(ReportExecutionEvidence result) { }
        /** Compatibility seam for legacy core execution snapshots during migration. */
        default void execution(Object result) { execution(ReportModelAdapters.execution(result)); }
        /** Adds one typed, renderer-neutral journey aggregate disclosure. */
        default void networkAggregate(ReportNetworkInspector.JourneyAggregate aggregate) { }
        @Override Validation startValidation(String title);
        void complete(AtfReportStatus status); @Override void close();
    }
    public interface Validation extends ValidationSink.Scope, AutoCloseable {
        /** Explicit assertion channel; legacy lightweight sinks may ignore it. */
        default void evidenceOwner(ReportEvidenceOwner owner) { }
        /**
         * Adds a nested request evidence node to this validation.  The seam is
         * intentionally optional so existing lightweight adapters remain
         * source-compatible; renderer implementations that support the
         * structured API trace expose the full request/response/execution
         * pipeline here.
         */
        default Request startRequest(String title, String method, String path) {
            throw new UnsupportedOperationException("Nested request evidence is not supported");
        }
        void assertions(Collection<AtfAssertion> assertions);
        default void completeFailure(AtfReportStatus status) { complete(status); }
        default void uiActions(Collection<AtfUiActionRecord> actions) { }
        default void screenshot(String title, String base64Png) { }
        default void mediaScreenshot(String title, String base64Png) { }
        void complete(AtfReportStatus status); @Override void close();
    }
}

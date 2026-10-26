package atf.core.reporting.contract;
import atf.core.http.response.ApiExecutionResult;
import atf.core.reporting.inventory.ValidationSink;

import atf.core.ui.AtfUiActionRecord;

import java.nio.file.Path;
import java.util.Collection;

public final class AtfReportPorts {
    private AtfReportPorts() { }

    public interface Session extends AutoCloseable {
        Path reportPath();
        Scenario startScenario(String testClass, String scenario, Collection<String> tags);

        /**
         * Starts a scenario with optional Java package metadata for the class
         * root. Renderer implementations that do not expose this metadata
         * retain the original contract through the compatibility default.
         */
        default Scenario startScenario(String testClass, String testPackage,
                                       String scenario, Collection<String> tags) {
            return startScenario(testClass, scenario, tags);
        }
        void flush();
        @Override void close();
    }

    public interface Scenario extends AutoCloseable {
        String id();
        Phase startPhase(String name);
        void complete(AtfReportStatus status);
        boolean hasWarnings();
        @Override void close();
    }

    public interface Phase extends AutoCloseable {
        Request startRequest(String title, String method, String path);
        default void detail(String name, String value) { }
        void complete(AtfReportStatus status);

        @Override void close();
    }

    public interface Request extends ValidationSink, AutoCloseable {
        void detail(String name, String value);
        void json(String label, String json);

        /** Publishes a native inline UI screenshot on this report step. */
        default void screenshot(String title, String base64Png) { }

        /** Publishes consecutive Playwright actions as one auditable table. */
        default void uiActions(Collection<AtfUiActionRecord> actions) { }

        /**
         * Publishes renderer-neutral transport evidence for the completed
         * exchange. Implementations may render attempts and retry history, but
         * must keep the API response/body redaction policy at the reporting
         * boundary. The default preserves compatibility for external ports.
         */
        default void execution(ApiExecutionResult result) { }

        @Override
        Validation startValidation(String title);
        void complete(AtfReportStatus status);
        @Override void close();
    }

    public interface Validation extends ValidationSink.Scope, AutoCloseable {
        void assertions(Collection<AtfAssertion> assertions);

        /** Completes a failure evidence scope without requiring a screenshot. */
        default void completeFailure(AtfReportStatus status) { complete(status); }

        /** Publishes consecutive Playwright actions inside this validation zone. */
        default void uiActions(Collection<AtfUiActionRecord> actions) { }

        /** Publishes the visual state produced by the Playwright actions. */
        default void screenshot(String title, String base64Png) { }

        /** Publishes Base64 media as a native clickable report artifact. */
        default void mediaScreenshot(String title, String base64Png) { }

        void complete(AtfReportStatus status);
        @Override void close();
    }
}

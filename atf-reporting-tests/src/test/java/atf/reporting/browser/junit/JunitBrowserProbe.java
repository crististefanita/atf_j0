package atf.reporting.browser.junit;

import atf.reporting.browser.lifecycle.BrowserProbe;
import atf.reporting.browser.lifecycle.BrowserProbeScope;
import atf.ui.tests.extent.support.lifecycle.BrowserResources;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

/** Executes the selected original class subtree once; parent cases consume their own exact terminal verdict. */
public final class JunitBrowserProbe implements BrowserProbe {
    @Override public void open(Path directory) { }

    @Override public void verify(String[] arguments) throws Exception {
        var cases = new ArrayList<Map<String, Object>>();
        var dynamic = new ArrayList<Map<String, Object>>();
        var containers = new ArrayList<Throwable>();
        var assertions = new ArrayList<Throwable>();
        var witness = new TestExecutionListener() {
            @Override public void executionStarted(TestIdentifier id) { if (id.isTest()) BrowserProbeScope.caseStarted(); }
            @Override public void dynamicTestRegistered(TestIdentifier id) {
                dynamic.add(Map.of("id", Objects.requireNonNull(BrowserSelection.normalize(id.getUniqueId())),
                        "parent", Objects.requireNonNull(BrowserSelection.normalize(id.getParentId().orElseThrow())),
                        "name", id.getDisplayName(), "test", id.isTest()));
            }
            @Override public void executionFinished(TestIdentifier id, TestExecutionResult result) {
                if (id.isTest()) cases.add(Map.of("id", Objects.requireNonNull(BrowserSelection.normalize(id.getUniqueId())),
                        "status", result.getStatus().name(), "failure", result.getThrowable().map(JunitBrowserProbe::stack).orElse("")));
                if (id.isTest()) result.getThrowable().ifPresent(assertions::add);
                else result.getThrowable().ifPresent(containers::add);
                result.getThrowable().ifPresent(error -> BrowserProbeScope.recordFailure(assertions.isEmpty() ? error : assertions.getFirst()));
            }
            @Override public void executionSkipped(TestIdentifier id, String reason) {
                if (id.isTest()) cases.add(Map.of("id", Objects.requireNonNull(BrowserSelection.normalize(id.getUniqueId())), "status", "SKIPPED", "failure", reason));
            }
        };
        BrowserProbeScope.discoveryStarted();
        var request = LauncherDiscoveryRequestBuilder.request().configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
        for (String id : arguments) request.selectors(selectUniqueId(id));
        var config = LauncherConfig.builder().enableTestEngineAutoRegistration(false).addTestEngines(new JupiterTestEngine())
                .enableLauncherDiscoveryListenerAutoRegistration(false).enableLauncherSessionListenerAutoRegistration(false)
                .enableTestExecutionListenerAutoRegistration(false).enablePostDiscoveryFilterAutoRegistration(false).build();
        try (var session = LauncherFactory.openSession(config)) {
            var launcher = session.getLauncher();
            TestPlan selected = launcher.discover(request.build());
            BrowserProbeScope.discoveryCompleted();
            launcher.execute(selected, witness);
        }
        if (!containers.isEmpty()) {
            Throwable original = assertions.isEmpty() ? containers.getFirst() : assertions.getFirst();
            for (Throwable container : containers) if (container != original) original.addSuppressed(container);
            if (original instanceof Error error) throw error;
            if (original instanceof RuntimeException error) throw error;
            throw new IllegalStateException("Original browser container failed", original);
        }
        if (cases.isEmpty()) throw new AssertionError("Selected browser subtree executed no original cases");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(BrowserProbeScope.directory().resolve("junit-result.json").toFile(),
                Map.of("terminal", true, "selected", List.of(arguments), "cases", cases, "dynamic", dynamic));
    }

    private static String stack(Throwable error) { var text = new StringWriter(); error.printStackTrace(new PrintWriter(text)); return text.toString(); }
    @Override public void close() { BrowserResources.closeRemaining(); }
}

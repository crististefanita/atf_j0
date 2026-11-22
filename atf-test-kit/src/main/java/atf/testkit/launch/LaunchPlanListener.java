package atf.testkit.launch;

import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** One session owns request provenance, plan state, and callback scopes. */
final class LaunchPlanListener implements LauncherDiscoveryListener, TestExecutionListener, AutoCloseable {
    private final Map<ConfigurationParameters, Optional<List<String>>> requests = new IdentityHashMap<>();
    private final Map<String, TestLaunchContext.Scope> scopes = new ConcurrentHashMap<>();
    private State state;
    private Supplier<TestLaunchSnapshot> inherited;

    @Override public synchronized void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
        var selection = Optional.of(LaunchSelectors.describe(request.getSelectorsByType(DiscoverySelector.class)));
        requests.merge(request.getConfigurationParameters(), selection,
                (previous, current) -> previous.equals(current) ? previous : Optional.empty());
    }
    @Override public synchronized void testPlanExecutionStarted(TestPlan plan) {
        var selectors = requests.remove(plan.getConfigurationParameters());
        inherited = TestLaunchContext.enclosingSuite(plan.getRoots().stream()
                .map(TestIdentifier::getUniqueIdObject).toList()).orElse(null);
        state = new State(plan, selectors == null ? null : selectors.orElse(null));
    }
    @Override public void executionStarted(TestIdentifier test) {
        var supplier = inherited == null ? (Supplier<TestLaunchSnapshot>) state::snapshot : inherited;
        scopes.put(test.getUniqueId(), TestLaunchContext.bind(test.getUniqueIdObject(), supplier));
    }
    @Override public void executionFinished(TestIdentifier test, TestExecutionResult result) {
        var scope = scopes.remove(test.getUniqueId());
        if (scope != null) scope.close();
    }
    @Override public void dynamicTestRegistered(TestIdentifier test) {
        if (test.isTest()) state.dynamic.put(test.getUniqueId(), source(test));
    }
    @Override public void testPlanExecutionFinished(TestPlan plan) {
        scopes.values().forEach(TestLaunchContext.Scope::close);
        scopes.clear();
        state = null; inherited = null;
    }
    @Override public synchronized void close() {
        scopes.values().forEach(TestLaunchContext.Scope::close);
        scopes.clear(); requests.clear(); state = null; inherited = null;
    }

    private static TestLaunchSnapshot.TestSource source(TestIdentifier test) {
        String source = test.getSource().map(value -> {
            if (value instanceof MethodSource method) return method.getClassName() + "#"
                    + method.getMethodName() + "(" + method.getMethodParameterTypes() + ")";
            if (value instanceof ClassSource type) return type.getClassName();
            return value.getClass().getName();
        }).orElse("source unavailable");
        return new TestLaunchSnapshot.TestSource(test.getUniqueId(), source);
    }
    private static final class State {
        private final List<String> selectors;
        private final List<TestLaunchSnapshot.TestSource> discovered;
        private final Map<String, TestLaunchSnapshot.TestSource> dynamic = new ConcurrentHashMap<>();
        private State(TestPlan plan, List<String> selectors) {
            this.selectors = selectors;
            discovered = plan.getRoots().stream().flatMap(root -> plan.getDescendants(root).stream())
                    .filter(TestIdentifier::isTest).map(LaunchPlanListener::source)
                    .distinct().sorted(Comparator.comparing(TestLaunchSnapshot.TestSource::uniqueId)).toList();
        }
        private TestLaunchSnapshot snapshot() {
            return new TestLaunchSnapshot(selectors == null ? List.of() : selectors,
                    selectors == null ? "TestPlan (request unavailable in this session)" : "launcher request",
                    discovered, dynamic.values().stream()
                            .sorted(Comparator.comparing(TestLaunchSnapshot.TestSource::uniqueId)).toList());
        }
    }
}

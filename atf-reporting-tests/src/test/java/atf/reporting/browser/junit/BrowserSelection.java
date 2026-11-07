package atf.reporting.browser.junit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.*;
import org.junit.platform.launcher.*;

/** Captures the host's real selection before delegation; an absent selection fails closed. */
public final class BrowserSelection implements TestExecutionListener, LauncherDiscoveryListener {
    private static final Map<String, List<String>> SELECTED = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> EXACT = new ConcurrentHashMap<>();
    private static final Map<String, SortedSet<Integer>> ITERATIONS = new ConcurrentHashMap<>();

    @Override public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
        EXACT.clear(); ITERATIONS.clear();
        for (var selector : request.getSelectorsByType(ClassSelector.class)) EXACT.remove(selector.getClassName());
        for (var selector : request.getSelectorsByType(UniqueIdSelector.class)) {
            String id = normalize(selector.getUniqueId().toString());
            if (id != null && (id.contains("[test-template-invocation:") || id.contains("[dynamic-")))
                EXACT.computeIfAbsent(owner(id), ignored -> new ArrayList<>()).add(id);
        }
        for (var selector : request.getSelectorsByType(IterationSelector.class)) {
            if (selector.getParentSelector() instanceof MethodSelector method)
                ITERATIONS.put(method.getClassName() + "#" + method.getMethodName(), selector.getIterationIndices());
        }
    }

    @Override public void testPlanExecutionStarted(TestPlan plan) {
        var selections = new LinkedHashMap<String, LinkedHashSet<String>>();
        for (var root : plan.getRoots()) for (var item : plan.getDescendants(root)) {
            String id = normalize(item.getUniqueId());
            if (id == null) continue;
            var last = UniqueId.parse(id).getLastSegment();
            if (!Set.of("method", "test-template", "test-factory").contains(last.getType())) continue;
            String owner = owner(id);
            var selected = selections.computeIfAbsent(owner, ignored -> new LinkedHashSet<>());
            List<String> exact = EXACT.getOrDefault(owner, List.of()).stream().filter(value -> value.startsWith(id + "/")).toList();
            if (!exact.isEmpty()) selected.addAll(exact);
            else {
                String method = last.getValue().substring(0, last.getValue().indexOf('('));
                var iterations = ITERATIONS.get(owner + "#" + method);
                if (iterations != null && last.getType().equals("test-template"))
                    for (int index : iterations) selected.add(id + "/[test-template-invocation:#" + (index + 1) + "]");
                else selected.add(id);
            }
        }
        selections.forEach((name, ids) -> SELECTED.put(name, List.copyOf(ids)));
    }

    static List<String> forClass(Class<?> owner) {
        var selected = SELECTED.get(owner.getName());
        if (selected == null || selected.isEmpty()) throw new IllegalStateException("No host JUnit selection captured for " + owner.getName());
        return selected;
    }

    static String normalize(String id) {
        int owner = id.indexOf("[class:");
        return owner < 0 ? null : "[engine:junit-jupiter]/" + id.substring(owner);
    }
    static String owner(String id) {
        return UniqueId.parse(id).getSegments().stream().filter(segment -> segment.getType().equals("class"))
                .findFirst().orElseThrow().getValue();
    }
}

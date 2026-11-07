package atf.reporting.browser.junit;

import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import atf.reporting.browser.lifecycle.BrowserProbeProcess;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.platform.engine.UniqueId;

/** Terminal transport success and the original case verdict are distinct, independently checked facts. */
final class BrowserClassRun {
    static final BrowserProbeBudget BUDGET = new BrowserProbeBudget(Duration.ofSeconds(30), Duration.ofSeconds(120), Duration.ofSeconds(5), Duration.ofMinutes(10));
    private final Map<String, JsonNode> cases = new LinkedHashMap<>();
    private final Map<String, List<JsonNode>> children = new LinkedHashMap<>();
    private final Path directory;

    private BrowserClassRun(Path directory, List<String> selected) throws Exception {
        this.directory = directory;
        JsonNode result = new ObjectMapper().readTree(directory.resolve("junit-result.json").toFile());
        if (!result.path("terminal").asBoolean() || result.path("cases").isEmpty()) throw new AssertionError("No terminal JUnit cases: " + directory);
        for (JsonNode item : result.path("cases")) {
            String id = item.path("id").asText();
            if (selected.stream().noneMatch(value -> id.equals(value) || id.startsWith(value + "/")) || cases.put(id, item) != null)
                throw new AssertionError("Foreign or duplicate JUnit case: " + id);
        }
        var nodes = new HashSet<String>();
        for (JsonNode item : result.path("dynamic")) {
            if (!nodes.add(item.path("id").asText())) throw new AssertionError("Duplicate dynamic browser identity");
            children.computeIfAbsent(item.path("parent").asText(), ignored -> new ArrayList<>()).add(item);
        }
    }

    static BrowserClassRun execute(Class<?> owner) throws Exception {
        List<String> selected = BrowserSelection.forClass(owner);
        Path classes = Path.of(owner.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "report-browser-classes");
        Path result = BrowserProbeProcess.run(root, JunitBrowserProbe.class, BUDGET, List.of(classes), selected.toArray(String[]::new));
        return new BrowserClassRun(result, selected);
    }

    void assertCase(String id) {
        JsonNode result = cases.get(id);
        if (result == null || !result.path("status").asText().equals("SUCCESSFUL"))
            throw new AssertionError("Original browser case did not PASS: " + id + "; evidence=" + directory,
                    new IllegalStateException(result == null ? "Exact result missing" : result.path("failure").asText(result.path("status").asText())));
    }

    Stream<DynamicNode> dynamicChildren(String parent) {
        List<JsonNode> nodes = children.getOrDefault(parent, List.of());
        if (nodes.isEmpty()) throw new AssertionError("Original browser factory has no dynamic results: " + parent);
        var restored = new ArrayList<DynamicNode>();
        int next = 1;
        for (JsonNode node : nodes) {
            String id = node.path("id").asText(), name = node.path("name").asText();
            int ordinal = Integer.parseInt(UniqueId.parse(id).getLastSegment().getValue().substring(1));
            if (ordinal < next) throw new AssertionError("Dynamic browser registration order changed: " + id);
            while (next < ordinal) {
                restored.add(DynamicTest.dynamicTest("Unselected original dynamic node", () -> { throw new AssertionError("Unselected dynamic case was executed"); }));
                next++;
            }
            restored.add(node.path("test").asBoolean() ? DynamicTest.dynamicTest(name, () -> assertCase(id))
                    : DynamicContainer.dynamicContainer(name, dynamicChildren(id)));
            next++;
        }
        return restored.stream();
    }
}

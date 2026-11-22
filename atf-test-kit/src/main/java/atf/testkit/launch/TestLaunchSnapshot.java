package atf.testkit.launch;

import java.util.List;

/** Discovery facts, distinct from dynamic registration and execution results. */
public record TestLaunchSnapshot(List<String> selectors, String selectionSource,
                                 List<TestSource> discoveredTests, List<TestSource> dynamicTests) {
    public TestLaunchSnapshot {
        selectors = List.copyOf(selectors);
        java.util.Objects.requireNonNull(selectionSource, "selectionSource");
        discoveredTests = List.copyOf(discoveredTests);
        dynamicTests = List.copyOf(dynamicTests);
    }
    public long discoveredTestCount() { return discoveredTests.size(); }
    public long registeredDynamicTestCount() { return dynamicTests.size(); }

    public record TestSource(String uniqueId, String source) {
        public TestSource {
            java.util.Objects.requireNonNull(uniqueId, "uniqueId");
            java.util.Objects.requireNonNull(source, "source");
        }
    }
}

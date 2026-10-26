package atf.core.testing.api;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportStatus;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest

class ApiScenarioSelectionTest {
    @Test
    void exposesAStableConsumerReference() {
        ApiScenarioSelection selection = new ApiScenarioSelection(
                SampleScenario.class, "create", "posts", "PostsApiTests", "Create post");

        assertEquals("SampleScenario.create", selection.reference());
        assertEquals("posts", selection.family());
        assertEquals(java.util.List.of(AtfReportStatus.PASS), selection.reportScenarioStatuses());
    }

    @Test
    void normalizesSelectionLabelsAtThePlatformBoundary() {
        ApiScenarioSelection selection = new ApiScenarioSelection(
                SampleScenario.class, " create ", " posts ", " owner ", " Create post ");

        assertEquals("create", selection.method());
        assertEquals("posts", selection.family());
        assertEquals("owner", selection.reportOwner());
        assertEquals("Create post", selection.scenarioTitle());
    }

    @Test
    void rejectsIncompleteSelectionMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new ApiScenarioSelection(
                SampleScenario.class, "", "posts", "owner", "title"));
        assertThrows(IllegalArgumentException.class, () -> new ApiScenarioSelection(
                SampleScenario.class, "method", "", "owner", "title"));
        assertThrows(IllegalArgumentException.class, () -> new ApiScenarioSelection(
                SampleScenario.class, "method", "posts", "owner", "title",
                java.util.List.of("title", "other"), java.util.List.of(AtfReportStatus.PASS)));
    }

    private static final class SampleScenario { }
}

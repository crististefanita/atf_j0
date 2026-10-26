package atf.console.junit;

import atf.testkit.AtfUnitTest;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportPathOptInTest {
    @Test
    void sharedArtifactUsesAnyOwnerOptInRegardlessOfOwnerOrder() {
        for (List<Class<?>> owners : List.of(
                List.of(OptedOutOwner.class, OptedInOwner.class),
                List.of(OptedInOwner.class, OptedOutOwner.class))) {
            List<AtfConsoleEvent> published = new ArrayList<>();
            AtfReportPathOptIn policy = new AtfReportPathOptIn(published::add);
            owners.forEach(policy::include);

            policy.publish(AtfConsoleEvent.reportPublished(Path.of("build/report.html")));

            assertTrue(policy.isEnabled());
            assertEquals(1, published.size());
        }
    }

    @Test
    void sharedArtifactStaysSilentWhenNoOwnerOptedIn() {
        List<AtfConsoleEvent> published = new ArrayList<>();
        AtfReportPathOptIn policy = new AtfReportPathOptIn(published::add);
        policy.include(OptedOutOwner.class);
        policy.publish(AtfConsoleEvent.reportPublished(Path.of("build/report.html")));

        assertFalse(policy.isEnabled());
        assertTrue(published.isEmpty());
    }

    @AtfReportPath(AtfReportPath.Policy.DISABLED)
    private static class OptedOutOwner { }

    @AtfReportPath
    private static class OptedInOwner { }
}

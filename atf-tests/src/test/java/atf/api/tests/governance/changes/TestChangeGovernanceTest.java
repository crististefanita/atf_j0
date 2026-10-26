package atf.api.tests.governance.changes;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
public class TestChangeGovernanceTest {
    @Test
    void moduleTestChangesRequireAnExplicitControlRecord() throws Exception {
        var root = locateProjectRoot();
        var snapshot = GitTestChanges.read(root, GitTestChanges.baseRef(
                System.getProperty(GitTestChanges.BASE_PROPERTY), System.getenv()));
        var requests = FeedbackTestChangeRequests.read(root.resolve("docs/feedback"));
        var violations = TestChangePolicy.validate(snapshot.changes(), requests, snapshot.base(), LocalDate.now());
        assertTrue(violations.isEmpty(), () -> "Test-change governance failed:\n" + String.join("\n", violations));
    }
}

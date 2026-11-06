package atf.testkit.assertions;

import atf.testkit.assertions.AuditedAssertions;

import atf.testkit.AtfComponentTest;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@AtfComponentTest
class AuditedAssertionScopeComponentTest {
    @Test
    void nestedJupiterFailuresDoNotContaminateTheOuterComponentAudit() {
        var rows = new RecordedAssertions();
        java.util.List<RecordedAssertions.Check> nestedEvidence;
        try (var scope = AuditedAssertionScope.open(getClass().getName(),
                "nestedJupiterFailuresDoNotContaminateTheOuterComponentAudit", rows)) {
            var summary = new SummaryGeneratingListener();
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectClass(NestedFixture.class)).build(), summary);
            nestedEvidence = rows.checks();
            AuditedAssertions.assertEquals(1, summary.getSummary().getTestsFailedCount());
        }
        AuditedAssertions.assertTrue(nestedEvidence.isEmpty());
        AuditedAssertions.assertEquals(1, rows.checks().size());
        AuditedAssertions.assertTrue(rows.checks().getFirst().passed());
    }

    @AtfUnitTest
    static class NestedFixture {
        @Test void intentionalFailure() { AuditedAssertions.assertTrue(false); }
    }
}

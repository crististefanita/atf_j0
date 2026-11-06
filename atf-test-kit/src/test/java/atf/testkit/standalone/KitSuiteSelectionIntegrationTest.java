package atf.testkit.standalone;

import atf.testkit.AtfIntegrationTest;
import atf.testkit.suites.KitComponentSuite;
import atf.testkit.suites.KitUnitSuite;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Real suite discovery must keep deliberately failing inner fixtures behind their owning tests. */
@AtfIntegrationTest
class KitSuiteSelectionIntegrationTest {
    @Test void unitAndComponentSuitesRunTheirOraclesWithoutSelectingInnerFixtures() {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(KitUnitSuite.class), selectClass(KitComponentSuite.class)).build();
        var summary = new SummaryGeneratingListener();
        var started = new ArrayList<String>();
        LauncherFactory.create().execute(request, summary, new TestExecutionListener() {
            @Override public void executionStarted(TestIdentifier id) {
                if (id.isTest()) started.add(id.getUniqueId());
            }
        });
        var result = summary.getSummary();
        assertAll(
                () -> assertTrue(result.getTestsFoundCount() > 0, "Native suites must discover real tests"),
                () -> assertEquals(0, result.getTotalFailureCount(), () -> result.getFailures().stream()
                        .map(failure -> failure.getTestIdentifier().getUniqueId() + ": " + failure.getException()).toList().toString()),
                () -> assertEquals(0, result.getTestsSkippedCount(), "No disabled fixture may leak into a suite"),
                () -> assertEquals(0, result.getTestsAbortedCount()),
                () -> assertTrue(started.stream().anyMatch(id -> id.contains("RecordedAssertionsTest")), "Unit oracle missing"),
                () -> assertTrue(started.stream().anyMatch(id -> id.contains("ComponentAuditExtensionComponentTest")), "Component oracle missing"),
                () -> assertTrue(started.stream().noneMatch(id -> id.contains("$")), "Inner fixtures were selected directly"));
    }
}

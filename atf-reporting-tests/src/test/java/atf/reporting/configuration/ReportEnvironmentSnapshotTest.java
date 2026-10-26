package atf.reporting.configuration;

import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit contract for the operational Overview/System projection boundary. */
@AtfUnitTest
class ReportEnvironmentSnapshotTest {
    @Test
    void genericClientFallbackDoesNotClaimAnApiTransportForUiOnlyRuns() {
        assertEquals("Configured per execution", ReportEnvironment.defaults().get("Client"));
    }

    @Test
    void keepsRunContextTypedButPublishesOnlyOperationalSystemFields() {
        var snapshot = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "ACCEPTANCE")
                .put(ReportEnvironmentField.UI_HOST, "https://ui.example")
                .put(ReportEnvironmentField.SUITE, "secret test suite")
                .put(ReportEnvironmentField.TEST_PACKAGE, "atf.tests.internal")
                .put(ReportEnvironmentField.FLOW, "business checkout")
                .put(ReportEnvironmentField.FIXTURE, "private-fixture")
                .put(ReportEnvironmentField.UI_JOURNEY_PRECONDITION, "guest checkout configuration ready")
                .put(ReportEnvironmentField.RETRY_POLICY, "disabled")
                .build();

        var published = ReportEnvironment.with(snapshot, new SensitiveDataRedactor());

        assertAll(
                () -> assertTrue(snapshot.displayValues().containsKey("Suite")),
                () -> assertTrue(snapshot.displayValues().containsKey("UI journey precondition")),
                () -> assertTrue(published.containsKey("Environment")),
                () -> assertTrue(published.containsKey("UI host")),
                () -> assertTrue(published.containsKey("Retry policy")),
                () -> assertFalse(published.containsKey("Suite")),
                () -> assertFalse(published.containsKey("Test package")),
                () -> assertFalse(published.containsKey("Flow")),
                () -> assertFalse(published.containsKey("Fixture")),
                () -> assertFalse(published.containsKey("UI journey precondition")),
                () -> assertFalse(published.values().stream()
                        .anyMatch(value -> value.contains("private-fixture"))));
    }
}

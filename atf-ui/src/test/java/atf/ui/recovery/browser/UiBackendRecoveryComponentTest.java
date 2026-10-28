package atf.ui.recovery.browser;

import atf.testkit.AtfComponentTest;
import atf.ui.config.recovery.UiRecoveryConfiguration;
import atf.ui.playwright.recovery.UiBackendRecovery;
import atf.ui.recovery.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.ui.recovery.UiRecoveryTrace.*;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class UiBackendRecoveryComponentTest {
    private static UiRecoveryConfiguration policy(int attempts, boolean capture) {
        return new UiRecoveryConfiguration(true, "read-only", Set.of("catalog-readiness"), attempts,
                Duration.ofSeconds(3), Duration.ofMillis(20), capture, "YAML; browser fixture");
    }
    private static UiBackendRecovery controller(RecoveryBrowserFixture fixture, UiRecoveryConfiguration policy, List<UiRecoveryTrace> traces) {
        var read = new UiRecoveryOperation("catalog-readiness", UiRecoveryOperation.Safety.READ_ONLY, fixture.uri("/"), fixture.uri("/catalog"), "GET");
        var write = new UiRecoveryOperation("cart-add", UiRecoveryOperation.Safety.MUTATION, fixture.uri("/"), fixture.uri("/add_to_cart"), "GET");
        return new UiBackendRecovery(fixture.page, policy, List.of(read, write), traces::add);
    }
    @ParameterizedTest @ValueSource(strings = {"delayed", "transient"})
    void applicationReadCompletesAfterPassiveReobservationWithOnePointerAction(String mode) throws Exception {
        try (var fixture = new RecoveryBrowserFixture(mode)) {
            var traces = new ArrayList<UiRecoveryTrace>();
            try (var recovery = controller(fixture, policy(100, true), traces); var scope = recovery.begin("catalog-readiness")) {
                fixture.page.locator("#read").click();
                var probes = new AtomicInteger();
                var result = scope.await(() -> {
                    boolean ready = fixture.ready(); if (probes.incrementAndGet() == 1) fixture.release.countDown();
                    return ready ? Dom.READY : Dom.PENDING;
                });
                assertEquals(Decision.RECOVERED, result.decision()); assertTrue(probes.get() > 1);
                assertEquals(1, fixture.clicks()); assertEquals(0, fixture.writes.get());
                assertEquals(mode.equals("transient") ? 2 : 1, fixture.reads.get());
                assertTrue(traces.stream().anyMatch(t -> t.decision() == Decision.REOBSERVE));
            }
        }
    }
    @ParameterizedTest @ValueSource(strings = {"noise", "frame", "query-change"})
    void unrelatedFramesRoutesAndChangedQueryCannotAuthorizeRecovery(String mode) throws Exception {
        try (var fixture = new RecoveryBrowserFixture(mode)) {
            var traces = new ArrayList<UiRecoveryTrace>();
            try (var recovery = controller(fixture, policy(100, true), traces); var scope = recovery.begin("catalog-readiness")) {
                fixture.page.locator("#read").click(); fixture.release.countDown();
                var failure = assertThrows(UiRecoveryException.class, () -> scope.await(() -> fixture.ready() ? Dom.READY : Dom.PENDING));
                assertTrue(Set.of(Reason.MISSING_SIGNAL, Reason.CONFLICTING_REQUEST).contains(failure.trace().reason()));
                assertEquals(1, fixture.clicks()); assertEquals(0, fixture.writes.get());
            }
        }
    }
    @Test void getMutationStillWritesExactlyOnceAndIsNeverReplayEligible() throws Exception {
        try (var fixture = new RecoveryBrowserFixture("mutation")) {
            var traces = new ArrayList<UiRecoveryTrace>();
            try (var recovery = controller(fixture, policy(3, true), traces); var scope = recovery.begin("cart-add")) {
                fixture.page.locator("#write").click(); fixture.page.waitForCondition(() -> fixture.writes.get() == 1);
                assertEquals(Reason.OPERATION_NOT_ALLOWED, assertThrows(UiRecoveryException.class, () -> scope.await(() -> Dom.PENDING)).trace().reason());
                assertEquals(1, fixture.writes.get()); assertEquals(1, fixture.clicks());
                assertFalse(traces.stream().anyMatch(t -> t.decision() == Decision.REOBSERVE));
            }
        }
    }
    @Test void changedDocumentNestedScopeAndMissingOptInAreRejected() throws Exception {
        try (var fixture = new RecoveryBrowserFixture("delayed")) {
            var traces = new ArrayList<UiRecoveryTrace>();
            try (var recovery = controller(fixture, policy(3, true), traces); var scope = recovery.begin("catalog-readiness")) {
                assertThrows(IllegalStateException.class, () -> recovery.begin("catalog-readiness"));
                fixture.page.reload();
                assertEquals(Reason.DOCUMENT_CHANGED, assertThrows(UiRecoveryException.class, () -> scope.await(() -> Dom.READY)).trace().reason());
            }
            try (var recovery = controller(fixture, policy(3, false), traces); var scope = recovery.begin("catalog-readiness")) {
                assertEquals(Reason.CAPTURE_DISABLED, assertThrows(UiRecoveryException.class, () -> scope.await(() -> Dom.PENDING)).trace().reason());
            }
        }
    }
    @Test void aResponseFromBeforeTheScopeCannotAuthorizeTheNextInvocation() throws Exception {
        try (var fixture = new RecoveryBrowserFixture("delayed")) {
            fixture.page.evaluate("() => { void fetch('/catalog'); }");
            fixture.page.waitForCondition(() -> fixture.reads.get() == 1);
            var traces = new ArrayList<UiRecoveryTrace>();
            try (var recovery = controller(fixture, policy(3, true), traces); var scope = recovery.begin("catalog-readiness")) {
                fixture.release.countDown();
                fixture.page.waitForTimeout(30);
                assertEquals(Reason.MISSING_SIGNAL,
                        assertThrows(UiRecoveryException.class, () -> scope.await(() -> Dom.PENDING)).trace().reason());
                assertEquals(1, fixture.reads.get()); assertEquals(0, fixture.clicks());
            }
        }
    }
    @Test void persistent503IsBoundedWithoutAnotherRequest() throws Exception {
        try (var fixture = new RecoveryBrowserFixture("exhaust")) {
            var traces = new ArrayList<UiRecoveryTrace>();
            try (var recovery = controller(fixture, policy(3, true), traces); var scope = recovery.begin("catalog-readiness")) {
                fixture.page.locator("#read").click(); fixture.release.countDown();
                var failure = assertThrows(UiRecoveryException.class, () -> scope.await(() -> Dom.PENDING));
                assertEquals(Reason.ATTEMPTS, failure.trace().reason());
                assertEquals(3, failure.trace().attempt()); assertEquals(1, fixture.reads.get()); assertEquals(1, fixture.clicks());
            }
        }
    }
}

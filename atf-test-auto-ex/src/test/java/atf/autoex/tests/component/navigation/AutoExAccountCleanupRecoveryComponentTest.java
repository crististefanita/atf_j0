package atf.autoex.tests.component.navigation;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.runtime.navigation.AutoExStoreUnavailableException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static atf.testkit.assertions.AuditedAssertions.*;

/** Real browser documents prove cleanup recovery, identity ownership and the non-replay boundary. */
@AutoExComponentTest
class AutoExAccountCleanupRecoveryComponentTest {
    @Test void queueFullAfterPaymentRecoversThroughHomeAndDeletesOnlyTheOwnedAccount() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(false, false)) {
            Throwable primary = fixture.failPayment();
            fixture.run.finish(primary);
            assertEquals(0, primary.getSuppressed().length, java.util.Arrays.toString(primary.getSuppressed()));
            assertTrue(fixture.run.accountLifecycle().deletionWasConfirmed());
            assertEquals(List.of("GET /", "POST /payment", "GET /", "GET /delete_account", "GET /"), fixture.calls);
            assertEquals(1, fixture.deletions);
            String html = fixture.html();
            assertTrue(html.contains("POST /payment"));
            assertTrue(html.contains("queueFull=true"));
            assertTrue(html.contains("Cleanup recovery destination"));
            assertTrue(html.contains("Account deletion navigation"));
        }
    }

    @Test void anotherVisibleIdentityCannotAuthorizeDeletionAfterRecovery() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(true, false)) {
            Throwable primary = fixture.failPayment();
            fixture.run.finish(primary);
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0].getMessage().contains("identity is not owned"));
            assertEquals(0, fixture.deletions);
            assertEquals(List.of("GET /", "POST /payment", "GET /"), fixture.calls);
        }
    }

    @Test void persistentBusyHomeIsBoundedAndNeverReplaysPayment() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(false, true)) {
            Throwable primary = fixture.failPayment();
            fixture.run.finish(primary);
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0] instanceof AutoExStoreUnavailableException);
            assertEquals(1, fixture.calls.stream().filter("POST /payment"::equals).count());
            assertEquals(4, fixture.calls.stream().filter("GET /"::equals).count());
            assertEquals(0, fixture.deletions);
        }
    }

    @Test void anAlreadyStartedDeletionCannotBeReplayedByDocumentRecovery() throws Exception {
        try (var fixture = new AutoExCleanupBrowserFixture(false, false)) {
            Throwable primary = fixture.failPayment();
            fixture.run.accountLifecycle().deletionStarted();
            fixture.run.finish(primary);
            assertEquals(1, primary.getSuppressed().length);
            assertTrue(primary.getSuppressed()[0].getMessage().contains("may replay"));
            assertEquals(List.of("GET /", "POST /payment"), fixture.calls);
            assertEquals(0, fixture.deletions);
        }
    }

}

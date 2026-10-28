package atf.autoex.tests.component.journey;

import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.page.AccountPage;
import atf.autoex.support.account.AutoExAccountLifecycle;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.autoex.tests.extension.AutoExComponentTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static atf.testkit.assertions.AuditedAssertions.assertDoesNotThrow;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AutoExComponentTest
class AutoExAccountLifecycleComponentTest {
    @Test
    void distinctPageObjectsShareTheRunOwnerAndCannotReplayAnUnconfirmedDeletion() {
        var lifecycle = new AutoExAccountLifecycle();
        var cleanup = new AtomicReference<AutoExAccountPagePort.AccountCleanup>();
        var session = session(lifecycle, cleanup);
        var account = account();
        new AccountPage(session).trackDisposableAccountCreation(account);
        lifecycle.creationConfirmed();
        assertTrue(lifecycle.creationWasConfirmed());
        lifecycle.deletionStarted();
        var failure = assertThrows(IllegalStateException.class,
                () -> new AccountPage(session).accountDeletionResult(account));
        assertTrue(failure.getMessage().contains("may replay"));
        assertThrows(IllegalStateException.class, () -> cleanup.get().run());
    }

    @Test
    void uncertainCreationIsOwnedButAnotherIdentityOrBorrowedFixtureCannotBeDeleted() {
        var lifecycle = new AutoExAccountLifecycle();
        var owned = account();
        assertThrows(IllegalStateException.class, () -> lifecycle.requireOwner(owned, owned.signupName()));
        lifecycle.creationAttempted(owned);
        assertTrue(lifecycle.creationWasAttempted());
        assertDoesNotThrow(() -> lifecycle.requireOwner(owned, owned.signupName()));
        assertThrows(IllegalStateException.class, () -> lifecycle.requireOwner(owned, "another customer"));
        var other = account();
        assertThrows(IllegalStateException.class, () -> lifecycle.creationAttempted(other));
        assertThrows(IllegalStateException.class, () -> lifecycle.requireOwner(other, other.signupName()));
    }

    @Test
    void confirmedDeletionIsTerminalAndASeparateExecutionHasNoOwnership() {
        var lifecycle = new AutoExAccountLifecycle();
        var owned = account();
        lifecycle.creationAttempted(owned);
        lifecycle.deletionStarted();
        lifecycle.deletionConfirmed();
        lifecycle.deletionStarted();
        lifecycle.creationConfirmed();
        assertTrue(lifecycle.deletionWasConfirmed());
        assertThrows(IllegalStateException.class, lifecycle::requireDeletionNotStarted);
        assertThrows(IllegalStateException.class, () -> lifecycle.creationAttempted(owned));
        assertThrows(IllegalStateException.class,
                () -> new AutoExAccountLifecycle().requireOwner(owned, owned.signupName()));
    }

    private static AutoExTestAccount account() {
        return AutoExTestAccount.create(AutoExConfigurationLoader.load().registration());
    }

    private static AutoExAccountPagePort session(AutoExAccountLifecycle lifecycle,
                                                AtomicReference<AutoExAccountPagePort.AccountCleanup> cleanup) {
        return (AutoExAccountPagePort) Proxy.newProxyInstance(AutoExAccountPagePort.class.getClassLoader(),
                new Class<?>[]{AutoExAccountPagePort.class}, (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "accountLifecycle": return lifecycle;
                        case "onFailureCleanup": cleanup.set((AutoExAccountPagePort.AccountCleanup) arguments[0]); return null;
                        case "currentUrl": return "https://fixture.test/";
                        case "configuration": return AutoExConfigurationLoader.load();
                        case "page": throw new AssertionError("No browser or mutation may be reached after deletion started");
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
    }
}

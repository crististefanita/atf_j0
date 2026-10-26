package atf.autoex.page;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.account.AutoExAccountCleanupAction;
import atf.autoex.support.account.AutoExAccountCleanupDecision;
import atf.autoex.support.account.AutoExAccountDeletionAttempt;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.trace.AutoExCustomerTraceStep;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.support.validation.AutoExStoreAssertions;

import java.net.URI;
import java.util.List;

import static atf.autoex.support.validation.AutoExUiText.ACCOUNT_CREATED;
import static atf.autoex.support.validation.AutoExUiText.ACCOUNT_DELETED;

/** Registration during checkout and deterministic disposal of the synthetic account. */
public final class AccountPage extends AutoExPage {
    private final AutoExAccountPagePort run;
    public AccountPage(AutoExAccountPagePort run) { super(run); this.run = run; }

    public void registerFromCheckout(AutoExTestAccount account) {
        clickLast(AutoExSelectors.REGISTER_LOGIN, "registration form opens from checkout");
        register(account);
    }

    public void registerFromStore(AutoExTestAccount account) {
        click(AutoExSelectors.HEADER_LOGIN, "registration form opens from the store header");
        register(account);
    }

    private void register(AutoExTestAccount account) {
        fill(AutoExSelectors.SIGNUP_NAME, account.signupName(), "synthetic account name is entered");
        fill(AutoExSelectors.SIGNUP_EMAIL, account.email(), "unique test email is entered");
        click(AutoExSelectors.SIGNUP_BUTTON, "signup details are submitted");
        AutoExConfiguration.Registration profile = account.profile();
        click(AutoExSelectors.ACCOUNT_GENDER_MR, "demo customer title is selected");
        fill(AutoExSelectors.ACCOUNT_PASSWORD, account.password(), "generated test password is entered");
        select(AutoExSelectors.ACCOUNT_DAYS, profile.birthDay(), "birth day is selected");
        select(AutoExSelectors.ACCOUNT_MONTHS, profile.birthMonth(), "birth month is selected");
        select(AutoExSelectors.ACCOUNT_YEARS, profile.birthYear(), "birth year is selected");
        fill(AutoExSelectors.ACCOUNT_FIRST_NAME, profile.firstName(), "synthetic first name is entered");
        fill(AutoExSelectors.ACCOUNT_LAST_NAME, profile.lastName(), "synthetic last name is entered");
        fill(AutoExSelectors.ACCOUNT_COMPANY, profile.company(), "test company is entered");
        fill(AutoExSelectors.ACCOUNT_ADDRESS_1, profile.address1(), "test address is entered");
        fill(AutoExSelectors.ACCOUNT_ADDRESS_2, profile.address2(), "secondary test address is entered");
        select(AutoExSelectors.ACCOUNT_COUNTRY, profile.country(), "country is selected");
        fill(AutoExSelectors.ACCOUNT_STATE, profile.state(), "test state is entered");
        fill(AutoExSelectors.ACCOUNT_CITY, profile.city(), "test city is entered");
        fill(AutoExSelectors.ACCOUNT_POSTAL_CODE, profile.postalCode(), "test postal code is entered");
        fill(AutoExSelectors.ACCOUNT_MOBILE, profile.mobileNumber(), "test phone number is entered");
        trackDisposableAccountCreation(account);
        click(AutoExSelectors.CREATE_ACCOUNT, "disposable account is created on the practice site");

        String result = text(AutoExSelectors.ACCOUNT_RESULT_HEADING, "account-creation result is read");
        AtfAssertion creation = AutoExStoreAssertions.accountCreated(result);
        if (creation.passed()) run.accountLifecycle().creationConfirmed();
        run.checkpoint(AutoExPurchaseTraceStep.ACCOUNT_CREATED,
                List.of(creation));
        click(AutoExSelectors.CONTINUE, "created account session continues to the store");
    }

    public String loggedInName() {
        return text(AutoExSelectors.LOGGED_IN_NAME, "signed-in customer identity is read");
    }

    /** Arm before a real UI or API create attempt; both use the same identity-checked UI cleanup. */
    public void trackDisposableAccountCreation(AutoExTestAccount account) {
        run.accountLifecycle().creationAttempted(account);
        run.onFailureCleanup(() -> deleteIfCreatedAndLoggedIn(account));
    }

    public void login(AutoExTestAccount account) {
        if (!visible(AutoExSelectors.LOGIN_EMAIL)) {
            click(AutoExSelectors.HEADER_LOGIN, "existing customer login form opens");
        }
        fill(AutoExSelectors.LOGIN_EMAIL, account.email(), "disposable customer email is entered");
        fill(AutoExSelectors.LOGIN_PASSWORD, account.password(), "disposable customer password is entered");
        click(AutoExSelectors.LOGIN_BUTTON, "existing customer signs in");
        run.waitForText(AutoExSelectors.LOGGED_IN_NAME, account.signupName(),
                "the signed-in session belongs to this test's disposable customer");
    }

    public boolean logout() {
        click(AutoExSelectors.LOGOUT, "the disposable customer signs out");
        run.waitForText(AutoExSelectors.LOGIN_BUTTON, "Login", "the customer login form is ready");
        return visible(AutoExSelectors.LOGIN_EMAIL) && !visible(AutoExSelectors.DELETE_ACCOUNT);
    }

    public String deliveryAddress(String expectedDisplayName) {
        run.waitForText(AutoExSelectors.DELIVERY_ADDRESS, expectedDisplayName,
                "delivery address contains the configured customer name");
        return text(AutoExSelectors.DELIVERY_ADDRESS, "delivery address is read");
    }

    public String billingAddress(String expectedDisplayName) {
        run.waitForText(AutoExSelectors.BILLING_ADDRESS, expectedDisplayName,
                "billing address contains the configured customer name");
        return text(AutoExSelectors.BILLING_ADDRESS, "billing address is read");
    }

    public String accountDeletionResult(AutoExTestAccount account) {
        requireDeletionNotStarted();
        run.dismissGoogleVignetteIfPresent();
        String identity = requireOwnIdentity(account);
        URI destination = AutoExRoutes.DELETE_ACCOUNT.resolve(run.configuration().baseUrl());
        try (var attempt = new AutoExAccountDeletionAttempt(run.page(), destination)) {
            try {
                return completeDeletion(account, identity, destination, attempt);
            } finally {
                if (attempt.startedRequests() > 0) run.accountLifecycle().deletionStarted();
            }
        }
    }

    private String completeDeletion(AutoExTestAccount account, String identity, URI destination,
                                    AutoExAccountDeletionAttempt attempt) {
        click(AutoExSelectors.DELETE_ACCOUNT, "this run's disposable customer account is deleted");
        boolean dismissed = awaitDeletionOrDismissAdvert(destination);
        if (attempt.progressAfter(dismissed).mayResumeNavigation()) {
            resumeDeletionNavigation(account, attempt);
            awaitDeletionOrDismissAdvert(destination);
        }
        run.page().waitForURL(destination.toString());
        run.waitForText(AutoExSelectors.ACCOUNT_DELETED_HEADING, ACCOUNT_DELETED.text(),
                "the deletion destination displays its visible account-deleted confirmation");
        String result = text(AutoExSelectors.ACCOUNT_DELETED_HEADING, "account-deletion result is read");
        String actualUrl = run.currentUrl();
        AtfAssertion deletion = AutoExStoreAssertions.accountDeleted(result);
        boolean reachedDestination = destination.toString().equals(actualUrl);
        var traffic = attempt.traffic();
        boolean singleNavigation = traffic.singleDeletionNavigation();
        if (deletion.passed() && reachedDestination && singleNavigation) {
            run.accountLifecycle().deletionConfirmed();
            run.clearFailureCleanup();
        }
        run.checkpoint(AutoExPurchaseTraceStep.ACCOUNT_DELETED,
                List.of(new AtfAssertion("Disposable customer identity", account.signupName(), identity,
                                account.signupName().equals(identity)),
                        new AtfAssertion("Account deletion destination", destination.toString(), actualUrl,
                                reachedDestination),
                        new AtfAssertion("Account deletion navigation", "exactly one GET document navigation",
                                traffic.description(), singleNavigation), deletion));
        click(AutoExSelectors.CONTINUE, "deleted account flow returns to the store");
        return result;
    }

    private String requireOwnIdentity(AutoExTestAccount account) {
        String identity = loggedInName();
        run.accountLifecycle().requireOwner(account, identity);
        return identity;
    }

    private boolean awaitDeletionOrDismissAdvert(URI destination) {
        String expectedUrl = destination.toString();
        run.page().waitForURL(url -> expectedUrl.equals(url)
                || url.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT));
        return run.dismissGoogleVignetteIfPresent();
    }

    private void resumeDeletionNavigation(AutoExTestAccount account, AutoExAccountDeletionAttempt attempt) {
        String interruptedUrl = run.currentUrl();
        run.navigate(AutoExRoutes.HOME.resolve(run.configuration().baseUrl()));
        String identity = requireOwnIdentity(account);
        boolean deleteAvailable = visible(AutoExSelectors.DELETE_ACCOUNT);
        int startedRequests = attempt.startedRequests();
        run.checkpoint(AutoExCustomerTraceStep.ACCOUNT_DELETION_RESUMED,
                List.of(new AtfAssertion("Deletion request before continuation", "0",
                                Integer.toString(startedRequests), startedRequests == 0),
                        new AtfAssertion("Disposable customer after reloading the store", account.signupName(),
                                identity, account.signupName().equals(identity)),
                        new AtfAssertion("Account deletion is still offered", "visible Delete account",
                                "available=" + deleteAvailable + "; interrupted=" + interruptedUrl, deleteAvailable)));
        click(AutoExSelectors.DELETE_ACCOUNT, "account deletion navigation resumes once after the visible advert");
    }

    private void deleteIfCreatedAndLoggedIn(AutoExTestAccount account) {
        if (run.accountLifecycle().deletionWasConfirmed()) {
            run.clearFailureCleanup();
            return;
        }
        requireDeletionNotStarted();
        if (deletionWasConfirmed()) {
            run.clearFailureCleanup();
            return;
        }
        if (accountCreationWasConfirmed()) run.accountLifecycle().creationConfirmed();
        recoverObservedVignette();
        recoverUnavailableStoreDocument();
        try {
            cleanupFromCurrentPage(account);
        } catch (RuntimeException | AssertionError firstFailure) {
            if (!onVignette()) throw firstFailure;
            try {
                recoverObservedVignette();
                cleanupFromCurrentPage(account);
            } catch (RuntimeException | AssertionError recoveryFailure) {
                firstFailure.addSuppressed(recoveryFailure);
                throw firstFailure;
            }
        }
    }

    private void cleanupFromCurrentPage(AutoExTestAccount account) {
        if (deletionWasConfirmed()) {
            run.clearFailureCleanup();
            return;
        }
        if (run.accountLifecycle().creationWasAttempted() && !visible(AutoExSelectors.DELETE_ACCOUNT)
                && !accountCreationWasConfirmed()) {
            login(account);
        }
        AutoExAccountCleanupAction action = AutoExAccountCleanupDecision.resolve(
                run.accountLifecycle().creationWasConfirmed(), visible(AutoExSelectors.CONTINUE),
                visible(AutoExSelectors.DELETE_ACCOUNT));
        if (action == AutoExAccountCleanupAction.CONTINUE_THEN_DELETE) {
            click(AutoExSelectors.CONTINUE, "finish account setup before failure cleanup");
        }
        run.dismissGoogleVignetteIfPresent();
        if (action == AutoExAccountCleanupAction.NO_OP) return;
        if (action == AutoExAccountCleanupAction.CONFIRMED_BUT_UNAVAILABLE
                || !visible(AutoExSelectors.DELETE_ACCOUNT)) {
            throw new IllegalStateException(
                    "Disposable account creation was confirmed, but its cleanup action is unavailable: "
                            + account.email());
        }
        accountDeletionResult(account);
    }

    private boolean accountCreationWasConfirmed() {
        return accountResultContains(ACCOUNT_CREATED.text());
    }

    private boolean deletionWasConfirmed() {
        URI destination = AutoExRoutes.DELETE_ACCOUNT.resolve(run.configuration().baseUrl());
        if (!destination.toString().equals(run.currentUrl())) return false;
        var heading = run.page().locator(AutoExSelectors.ACCOUNT_DELETED_HEADING);
        return heading.count() == 1 && heading.isVisible() && heading.innerText().contains(ACCOUNT_DELETED.text());
    }

    private void requireDeletionNotStarted() {
        run.accountLifecycle().requireDeletionNotStarted();
    }

    private boolean accountResultContains(String expected) {
        var heading = run.page().locator(AutoExSelectors.ACCOUNT_RESULT_HEADING);
        return heading.count() == 1 && heading.isVisible() && heading.innerText().contains(expected);
    }

    private boolean onVignette() {
        return run.currentUrl().contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT);
    }

    private void recoverObservedVignette() {
        String interruptedUrl = run.currentUrl();
        if (!interruptedUrl.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT)) return;
        var home = AutoExRoutes.HOME.resolve(run.configuration().baseUrl());
        run.navigate(home);
        String recoveredUrl = run.currentUrl();
        run.checkpoint(AutoExCustomerTraceStep.CLEANUP_RECOVERED,
                List.of(new AtfAssertion("Observed cleanup interruption",
                                AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT, interruptedUrl,
                                interruptedUrl.contains(AutoExRoutes.GOOGLE_VIGNETTE_FRAGMENT)),
                        new AtfAssertion("Cleanup recovery destination", home.toString(), recoveredUrl,
                                home.toString().equals(recoveredUrl))));
    }

    private void recoverUnavailableStoreDocument() {
        String interruptedUrl = run.currentUrl();
        URI base = run.configuration().baseUrl();
        if (base.getHost().equalsIgnoreCase(URI.create(interruptedUrl).getHost())) return;
        URI home = AutoExRoutes.HOME.resolve(base);
        run.navigate(home);
        String recoveredUrl = run.currentUrl();
        run.checkpoint(AutoExCustomerTraceStep.CLEANUP_DOCUMENT_RECOVERED,
                List.of(new AtfAssertion("Interrupted store document", "outside the configured store origin",
                                interruptedUrl, !base.getHost().equalsIgnoreCase(URI.create(interruptedUrl).getHost())),
                        new AtfAssertion("Cleanup recovery destination", home.toString(), recoveredUrl,
                                home.toString().equals(recoveredUrl))));
    }
}

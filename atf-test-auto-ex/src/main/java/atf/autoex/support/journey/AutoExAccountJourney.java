package atf.autoex.support.journey;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.page.AccountPage;
import atf.autoex.page.HomePage;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.validation.AutoExStoreAssertions;

import java.util.List;

import static atf.autoex.support.journey.AutoExPurchaseTraceStep.STORE_OPEN;
import static atf.autoex.support.journey.AutoExPurchaseTraceStep.TERMINAL_STATE;
import static atf.autoex.support.trace.AutoExCustomerTraceStep.CUSTOMER_SIGNED_IN;

/** One real account self-service story shared by the UI and UI+API evidence profiles. */
public final class AutoExAccountJourney {
    private AutoExAccountJourney() { }

    public static void registersAndDeletesOwnAccount(AutoExAccountJourneyPort run) {
        String heading = new HomePage(run).open();
        run.checkpoint(STORE_OPEN, List.of(AutoExStoreAssertions.storeAvailability(heading)));
        var customer = AutoExTestAccount.create(run.configuration().registration());
        var account = new AccountPage(run);
        account.registerFromStore(customer);
        String identity = account.loggedInName();
        run.checkpoint(CUSTOMER_SIGNED_IN,
                List.of(new AtfAssertion("Registered customer", customer.signupName(), identity,
                        customer.signupName().equals(identity))));

        String deletion = account.accountDeletionResult(customer);
        run.dismissGoogleVignetteIfPresent();
        String destination = AutoExRoutes.HOME.resolve(run.configuration().baseUrl()).toString();
        String actualUrl = run.currentUrl();
        run.finalScreenshot(TERMINAL_STATE,
                List.of(AutoExStoreAssertions.accountDeleted(deletion),
                        new AtfAssertion("Post-deletion store", destination, actualUrl, destination.equals(actualUrl))));
    }
}

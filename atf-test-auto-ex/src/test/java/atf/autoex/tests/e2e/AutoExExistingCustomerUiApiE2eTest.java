package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.autoex.tests.extension.AutoExNetworkDocuments;
import atf.reporting.contract.AtfAssertion;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.page.AccountPage;
import atf.autoex.page.HomePage;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.account.AutoExCustomerApiSetup;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.validation.AutoExStoreAssertions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static atf.autoex.support.journey.AutoExPurchaseTraceStep.STORE_OPEN;
import static atf.autoex.support.journey.AutoExPurchaseTraceStep.TERMINAL_STATE;
import static atf.autoex.support.trace.AutoExCustomerTraceStep.CUSTOMER_SIGNED_IN;

/** Existing-customer self-service: real API setup, real UI login and identity-checked UI deletion. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI_API)
@AutoExNetworkDocuments({"/login", "/delete_account"})

@DisplayName("Automation Exercise existing customer — UI + API")
public class AutoExExistingCustomerUiApiE2eTest {
    @Test
    @DisplayName("an existing customer signs in, deletes their own account and returns to the store")
    void existingCustomerDeletesOwnAccount(AutoExE2eRun run) throws Exception {
        var customer = AutoExTestAccount.create(run.configuration().registration());
        var account = new AccountPage(run);
        AutoExCustomerApiSetup.prepare(run, account, customer);
        String heading = new HomePage(run).open();
        run.checkpoint(STORE_OPEN, List.of(AutoExStoreAssertions.storeAvailability(heading)));
        account.login(customer);
        String identity = account.loggedInName();
        run.checkpoint(CUSTOMER_SIGNED_IN,
                List.of(new AtfAssertion("Existing customer", customer.signupName(), identity,
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

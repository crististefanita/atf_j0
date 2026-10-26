package atf.autoex.tests.e2e;

import atf.autoex.tests.extension.AutoExE2eTest;
import atf.reporting.contract.AtfAssertion;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.page.AccountPage;
import atf.autoex.page.HomePage;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.support.validation.AutoExStoreAssertions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static atf.autoex.support.journey.AutoExPurchaseTraceStep.STORE_OPEN;
import static atf.autoex.support.journey.AutoExPurchaseTraceStep.TERMINAL_STATE;
import static atf.autoex.support.trace.AutoExCustomerTraceStep.CUSTOMER_SIGNED_IN;

/** Real account self-service, independent of checkout and payment; adverts are never fabricated. */
@AutoExE2eTest
@AtfTestChannel(TestChannel.UI)

@DisplayName("Automation Exercise customer account — UI")
public class AutoExAccountUiE2eTest {
    @Test
    @DisplayName("a registered customer deletes their own account and returns to the store")
    void registersAndDeletesOwnAccount(AutoExE2eRun run) {
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

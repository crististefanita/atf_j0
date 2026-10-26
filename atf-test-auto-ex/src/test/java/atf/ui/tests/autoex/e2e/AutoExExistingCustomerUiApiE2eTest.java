package atf.ui.tests.autoex.e2e;

import atf.api.console.junit.AtfConsoleTestExtension;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.testkit.AtfConsoleMessages;
import atf.api.testkit.AtfE2eTest;
import atf.api.testkit.channel.AtfTestChannel;
import atf.api.testkit.channel.TestChannel;
import atf.api.testkit.output.UiReportSubdirectory;
import atf.api.testkit.reporting.AtfReportPath;
import atf.ui.autoex.locator.AutoExRoutes;
import atf.ui.autoex.page.AccountPage;
import atf.ui.autoex.page.HomePage;
import atf.ui.autoex.runtime.AutoExE2eRun;
import atf.ui.autoex.runtime.account.AutoExCustomerApiSetup;
import atf.ui.autoex.support.account.AutoExTestAccount;
import atf.ui.autoex.support.validation.AutoExStoreAssertions;
import atf.ui.tests.autoex.extension.AutoExE2eExtension;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static atf.ui.autoex.support.journey.AutoExPurchaseTraceStep.STORE_OPEN;
import static atf.ui.autoex.support.journey.AutoExPurchaseTraceStep.TERMINAL_STATE;
import static atf.ui.autoex.support.trace.AutoExCustomerTraceStep.CUSTOMER_SIGNED_IN;

/** Existing-customer self-service: real API setup, real UI login and identity-checked UI deletion. */
@AtfE2eTest
@AtfTestChannel(TestChannel.UI_API)
@UiReportSubdirectory("auto-ex")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExE2eExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
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

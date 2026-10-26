package atf.autoex.runtime.account;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.api.AutoExApiAccount;
import atf.autoex.api.AutoExApiRequestFactory;
import atf.autoex.api.AutoExLiveApi;
import atf.autoex.page.AccountPage;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.support.account.AutoExTestAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import static atf.autoex.api.AutoExApiJsonField.MESSAGE;
import static atf.autoex.api.AutoExApiJsonField.RESPONSE_CODE;
import static atf.autoex.api.AutoExApiOutcome.USER_CREATED;

/** A real pre-existing customer, with cleanup armed before the single create attempt. */
public final class AutoExCustomerApiSetup {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int EXPECTED_HTTP_STATUS = 200;

    private AutoExCustomerApiSetup() { }

    public static void prepare(AutoExE2eRun run, AccountPage accountPage, AutoExTestAccount customer)
            throws java.io.IOException {
        var profile = customer.profile();
        var account = new AutoExApiAccount(customer.signupName(), customer.email(), customer.password(),
                "Mr", profile.birthDay(), profile.birthMonth(), profile.birthYear(), profile.firstName(),
                profile.lastName(), profile.company(), profile.address1(), profile.address2(), profile.country(),
                profile.postalCode(), profile.state(), profile.city(), profile.mobileNumber());
        var api = AutoExLiveApi.open(run.configuration().baseUrl(), run.configuration().browser().timeout());
        accountPage.trackDisposableAccountCreation(customer);
        var execution = api.execute(new AutoExApiRequestFactory().createAccount(account));
        var body = JSON.readTree(execution.response().body());
        String actualCode = body == null ? "<missing>" : body.path(RESPONSE_CODE.path()).asText("<missing>");
        String actualMessage = body == null ? "<missing>" : body.path(MESSAGE.path()).asText("<missing>");
        String expectedCode = Integer.toString(USER_CREATED.responseCode());
        int httpStatus = execution.response().status();
        run.apiSetup("the disposable customer already exists before signing in to the store", execution,
                List.of(new AtfAssertion("HTTP transport status", Integer.toString(EXPECTED_HTTP_STATUS),
                                Integer.toString(httpStatus), httpStatus == EXPECTED_HTTP_STATUS),
                        new AtfAssertion("JSON responseCode", expectedCode, actualCode,
                                expectedCode.equals(actualCode)),
                        new AtfAssertion("JSON message", USER_CREATED.message(), actualMessage,
                                USER_CREATED.message().equals(actualMessage))));
    }
}

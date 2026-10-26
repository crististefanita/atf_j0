package atf.autoex.tests.api;

import atf.console.junit.AtfConsoleTestExtension;
import atf.core.http.execution.ApiExecutor;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.channel.AtfTestChannel;
import atf.testkit.channel.TestChannel;
import atf.testkit.lifecycle.ArmedCleanup;
import atf.testkit.reporting.AtfReportPath;
import atf.autoex.api.AutoExApiAccount;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.liveExecutor;
import atf.autoex.api.AutoExApiJsonField;
import atf.autoex.api.AutoExApiRequestFactory;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;
import java.util.UUID;

import static atf.autoex.api.AutoExApiOutcome.ACCOUNT_DELETED;
import static atf.autoex.api.AutoExApiOutcome.LOGIN_PARAMETER_MISSING;
import static atf.autoex.api.AutoExApiOutcome.METHOD_NOT_SUPPORTED;
import static atf.autoex.api.AutoExApiOutcome.SEARCH_PARAMETER_MISSING;
import static atf.autoex.api.AutoExApiOutcome.USER_CREATED;
import static atf.autoex.api.AutoExApiOutcome.USER_EXISTS;
import static atf.autoex.api.AutoExApiOutcome.USER_NOT_FOUND;
import static atf.autoex.api.AutoExApiOutcome.USER_UPDATED;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.account;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.assertResponse;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.assertResponseWithTextFields;
import static atf.autoex.tests.support.api.AutoExApiTestSupport.updatedName;

/** Opt-in live contract proof; default component gates never contact the public service. */
@AtfIntegrationTest
@AtfTestChannel(TestChannel.API)
@EnabledIfSystemProperty(named = "atf.autoex.api.live.enabled", matches = "true")
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfReportPath
@ExtendWith({AtfConsoleTestExtension.class, AutoExApiContractReportExtension.class})
@Execution(ExecutionMode.SAME_THREAD)
public class AutoExApiLiveContractTest {
    @Test
    void publicApiContractsUseAUniqueAccountAndAlwaysAttemptCleanup() {
        AutoExConfiguration configuration = AutoExConfigurationLoader.load();
        ApiExecutor api = liveExecutor(configuration);
        AutoExApiRequestFactory requests = new AutoExApiRequestFactory();
        AutoExApiAccount account = account(configuration, UUID.randomUUID().toString());

        assertResponse("API 1 products list", api, requests.productsList(), 200, null,
                AutoExApiJsonField.PRODUCTS);
        assertResponse("API 2 unsupported products POST", api, requests.postProductsList(), METHOD_NOT_SUPPORTED);
        assertResponse("API 3 brands list", api, requests.brandsList(), 200, null,
                AutoExApiJsonField.BRANDS);
        assertResponse("API 4 unsupported brands PUT", api, requests.putBrandsList(), METHOD_NOT_SUPPORTED);
        assertResponse("API 5 search product", api,
                requests.searchProduct(configuration.catalog().searchTerm()), 200, null,
                AutoExApiJsonField.PRODUCTS);
        assertResponse("API 6 missing search parameter", api, requests.searchProduct(null), SEARCH_PARAMETER_MISSING);
        assertResponse("API 8 missing login email", api,
                requests.verifyLoginWithoutEmail(account.password()), LOGIN_PARAMETER_MISSING);
        assertResponse("API 9 unsupported login DELETE", api, requests.deleteMethodOnVerifyLogin(), METHOD_NOT_SUPPORTED);
        assertResponse("API 10 invalid login", api,
                requests.verifyLogin(account.email(), "invalid-" + account.password()), USER_NOT_FOUND);

        try (ArmedCleanup cleanup = new ArmedCleanup(() ->
                assertResponse("API 12 cleanup", api,
                        requests.deleteAccount(account.email(), account.password()), ACCOUNT_DELETED))) {
            cleanup.arm();
            assertResponse("API 11 create account", api, requests.createAccount(account), USER_CREATED);
            assertResponse("API 7 valid login", api,
                    requests.verifyLogin(account.email(), account.password()), USER_EXISTS);
            assertResponseWithTextFields("API 14 user detail", api,
                    requests.userDetail(account.email()), 200, null,
                    Map.of(AutoExApiJsonField.USER_EMAIL.path(), account.email()));
            assertResponse("API 13 update account", api,
                    requests.updateAccount(updatedName(account)), USER_UPDATED);
        }
    }

}

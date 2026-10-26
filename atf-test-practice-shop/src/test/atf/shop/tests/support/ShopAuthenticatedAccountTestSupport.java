package atf.shop.tests.support;

import atf.reporting.contract.AtfAssertion;
import atf.testkit.scope.TestRunScope;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.model.ShopCredentials;
import atf.shop.config.model.ShopRegistrationData;
import atf.shop.config.ShopRegistrationProfile;
import atf.shop.flow.ShopAccountFlow;
import atf.shop.pages.account.ShopAccountPage;
import atf.shop.pages.account.ShopRegistrationPage;
import atf.shop.support.validation.ShopPageValidator;
import atf.shop.support.ShopPurchaseTraceStep;

import java.util.ArrayList;
import java.util.List;

/** Keeps registration/reuse and login inside SETUP; the JUnit owner holds registered credentials. */
public final class ShopAuthenticatedAccountTestSupport {
    private ShopAuthenticatedAccountTestSupport() { }

    public static ShopCredentials prepare(ShopE2eTestSupport.E2eSetup setup) {
        ShopCredentials credentials;
        var accounts = setup.accounts();
        if (setup.accountSetupMode() == ShopTestSupport.AccountSetupMode.REGISTER_PER_RUN) {
            var acquired = accounts.getOrCreate(() -> register(setup));
            credentials = acquired.value();
            if (!acquired.created()) publishReuse(setup);
        } else {
            var registered = accounts.current();
            if (registered.isPresent()) {
                credentials = registered.orElseThrow();
                publishReuse(setup);
            } else {
                credentials = ShopCredentials.load(setup.configuration().accountFile());
                boolean credentialsAvailable = credentials.hasLiveAccountCandidate();
                setup.checkpoint(ShopPurchaseTraceStep.LOCAL_ACCOUNT_FIXTURE_LOADED,
                        List.of(new AtfAssertion("Local account fixture", "valid credentials are available",
                                credentialsAvailable ? "local credentials are available; values are redacted"
                                        : "local credentials are incomplete", credentialsAvailable)), List.of());
            }
        }

        new ShopAccountFlow(setup.locators()).loginAndAssertAccount(
                setup.page(), setup.actions(), setup.configuration(), credentials);
        List<AtfAssertion> assertions = new ArrayList<>(new ShopPageValidator(setup.locators())
                .validate(setup.page(), ShopAccountPage.class, "My account")
                .assertions());
        assertions.addAll(setup.apiNetwork().loginConfirmed(setup.configuration().api(), credentials));
        setup.checkpoint(ShopPurchaseTraceStep.ACCOUNT_SESSION_CONFIRMED,
                assertions, setup.apiNetwork().evidence(ShopApiCheckpoint.LOGIN));
        setup.complete();
        return credentials;
    }

    private static ShopCredentials register(ShopE2eTestSupport.E2eSetup setup) {
        ShopRegistrationProfile profile = setup.registrationProfileFile() == null
                ? ShopRegistrationProfile.synthetic()
                : ShopRegistrationProfile.load(setup.registrationProfileFile());
        ShopRegistrationData registration = profile.newRegistrationData();
        var registrationPage = new ShopRegistrationPage(
                setup.page(), setup.locators(), setup.actions());
        registrationPage.open(setup.configuration());
        registrationPage.fill(registration);
        long sequenceFence = setup.apiNetwork().sequenceMarker();
        int responseStatus;
        try {
            responseStatus = registrationPage.submitAndAwaitApiResponse(
                    setup.configuration().api().endpoint());
        } catch (RuntimeException | AssertionError uncertainSubmission) {
            List<AtfAssertion> failedSetup = new ArrayList<>(
                    setup.apiNetwork().registrationConfirmed(sequenceFence,
                            setup.configuration().api().endpoint()));
            failedSetup.add(new AtfAssertion("Registration submission outcome",
                    "one successful browser submission response is observed",
                    "registration outcome is unsuccessful or uncertain; the form will not be replayed", false));
            setup.fail(ShopPurchaseTraceStep.ACCOUNT_SETUP_FAILED,
                    failedSetup, setup.apiNetwork().evidence(ShopApiCheckpoint.REGISTRATION), uncertainSubmission);
            throw uncertainSubmission;
        }
        setup.checkpoint(ShopPurchaseTraceStep.NEW_ACCOUNT_REGISTERED,
                registrationAssertions(responseStatus,
                        setup.apiNetwork().registrationConfirmed(sequenceFence,
                                setup.configuration().api().endpoint())),
                setup.apiNetwork().evidence(ShopApiCheckpoint.REGISTRATION));
        return profile.credentials(registration);
    }

    private static void publishReuse(ShopE2eTestSupport.E2eSetup setup) {
        boolean suite = setup.runScope() == TestRunScope.SUITE;
        setup.checkpoint(suite ? ShopPurchaseTraceStep.SUITE_ACCOUNT_REUSED
                        : ShopPurchaseTraceStep.CLASS_RUN_ACCOUNT_REUSED,
                List.of(new AtfAssertion(suite ? "Suite account" : "Class-run account",
                        "a registered account is available",
                        "the lifecycle-owned account is reused; credentials are redacted", true)), List.of());
    }

    static List<AtfAssertion> registrationAssertions(int responseStatus,
                                                       List<AtfAssertion> journalAssertions) {
        boolean responseAccepted = responseStatus >= 200 && responseStatus < 300;
        boolean journalConfirmed = journalAssertions == null
                || journalAssertions.stream().allMatch(AtfAssertion::passed);
        boolean successful = responseAccepted && journalConfirmed;
        List<AtfAssertion> assertions = new ArrayList<>(List.of(
                new AtfAssertion("Account setup mode", "REGISTER_PER_RUN", "REGISTER_PER_RUN", true),
                new AtfAssertion("Registration browser response", "HTTP 2xx",
                        "HTTP " + responseStatus, responseAccepted),
                new AtfAssertion("Login readiness", "registered account details are ready for sign-in",
                        successful ? "credentials retained for the following login; values redacted"
                                : "registration was not accepted; sign-in must not start", successful)));
        if (journalAssertions != null) assertions.addAll(journalAssertions);
        return List.copyOf(assertions);
    }
}

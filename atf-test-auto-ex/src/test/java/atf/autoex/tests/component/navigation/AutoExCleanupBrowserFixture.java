package atf.autoex.tests.component.navigation;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.config.AutoExConfigurationLoader;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.page.AccountPage;
import atf.autoex.runtime.AutoExE2eRun;
import atf.autoex.runtime.AutoExReportSession;
import atf.autoex.runtime.reporting.AutoExReportSystemMetadata;
import atf.autoex.runtime.config.AutoExEvidenceProfile;
import atf.autoex.runtime.navigation.AutoExStoreUnavailableException;
import atf.autoex.support.account.AutoExTestAccount;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.ui.failure.UiCheckpointFailure;
import atf.autoex.support.journey.AutoExPurchaseTraceStep;
import atf.autoex.support.validation.AutoExStoreAssertions;
import atf.ui.playwright.UiScreenValidation;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.autoex.business.cart.AutoExCartFlow;
import atf.autoex.business.cart.AutoExCartResult;
import atf.autoex.flow.cart.AutoExCartUiFlow;
import atf.autoex.page.CartPage;
import atf.autoex.support.checkout.AutoExCheckoutAssertions;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.PlaywrightUiSession;
import com.microsoft.playwright.Route;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static atf.testkit.assertions.AuditedAssertions.*;
import static atf.ui.config.UiRetryOperation.NAVIGATE;

/** Browser-only fixture for deterministic owned-cleanup contracts; no remote traffic. */
final class AutoExCleanupBrowserFixture implements AutoCloseable {
    final List<String> calls = new ArrayList<>();
    final PlaywrightUiSession browser;
    final AutoExReportSession reports;
    final AutoExE2eRun run;
    final AutoExTestAccount account;
    int deletions;
    int signups;
    int creations;
    private enum Document { BUSY_PAYMENT, REJECTED_ORDER, INVALID_CART, VALID_CART, ACCOUNT_SETUP }
    static AutoExCleanupBrowserFixture accountSetup(boolean apiEnabled, boolean foreignIdentity) throws Exception {
        return new AutoExCleanupBrowserFixture(foreignIdentity, false, apiEnabled, Document.ACCOUNT_SETUP);
    }
    AutoExCleanupBrowserFixture(boolean foreignIdentity, boolean busyHome) throws Exception {
        this(foreignIdentity, busyHome, false, Document.BUSY_PAYMENT);
    }
    AutoExCleanupBrowserFixture(boolean apiEnabled) throws Exception {
        this(false, false, apiEnabled, Document.REJECTED_ORDER);
    }
    static AutoExCleanupBrowserFixture cart(boolean apiEnabled, boolean valid) throws Exception {
        return new AutoExCleanupBrowserFixture(false, false, apiEnabled,
                valid ? Document.VALID_CART : Document.INVALID_CART);
    }
    private AutoExCleanupBrowserFixture(boolean foreignIdentity, boolean busyHome,
                                       boolean apiEnabled, Document document) throws Exception {
        var source = AutoExConfigurationLoader.load();
        account = AutoExTestAccount.create(source.registration());
        URI base = URI.create("https://autoex.cleanup.fixture.test/");
        var defaults = UiBrowserConfiguration.defaults();
        var network = apiEnabled ? UiNetworkInspectionConfiguration.forHosts(List.of(base.getHost()))
                : UiNetworkInspectionConfiguration.disabled();
        var browserConfiguration = new UiBrowserConfiguration(base, defaults.browser(), defaults.headless(),
                defaults.timeout(), defaults.navigationTimeout(), defaults.viewportWidth(), defaults.viewportHeight(),
                defaults.zoomPercent(), defaults.screenshotOnFailure(), defaults.retryPolicy(), network, defaults.recovery());
        var config = new AutoExConfiguration(base, false, browserConfiguration,
                source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(),
                new UiRetryPolicy(3, Duration.ZERO, Set.of(NAVIGATE), "queue full"));
        if (apiEnabled) config = AutoExE2eRun.withNetworkInspection(config,
                List.of("/payment", "/view_cart", "/checkout", "/delete_account", "/signup", "/create_account"));
        browser = PlaywrightUiSession.open(config.browser());
        browser.page().route(base + "**", route -> {
            String path = URI.create(route.request().url()).getPath();
            if (route.request().isNavigationRequest()) calls.add(route.request().method() + " " + path);
            boolean busy = document == Document.BUSY_PAYMENT && path.equals("/payment")
                    || busyHome && calls.size() > 1 && path.equals("/");
            String body;
            if (busy) body = "<h2>" + AutoExSelectors.STORE_BUSY_MESSAGE + "</h2>";
            else if (path.equals("/delete_account")) {
                deletions++;
                body = "<h2 data-qa='account-deleted'>ACCOUNT DELETED!</h2><a data-qa='continue-button' href='/'>Continue</a>";
            } else body = "<header id='header'><a><i class='fa-user'></i><b>"
                    + (foreignIdentity ? "Another customer" : account.signupName())
                    + "</b></a><a href='/delete_account'>Delete account</a></header>"
                    + "<form method='post' action='/payment'><button id='pay'>Pay once</button></form>";
            if (document == Document.REJECTED_ORDER && path.equals("/payment")) body += "<h2 id='confirmation'>PAYMENT REJECTED</h2>";
            if (document == Document.ACCOUNT_SETUP && !path.equals("/delete_account")) {
                if (path.equals("/") && creations == 0) body = "<header id='header'><a href='/login'>Signup / Login</a></header>";
                if (path.equals("/login")) body = """
                        <form method='post' action='/signup'>
                        <input data-qa='signup-name' name='name'><input data-qa='signup-email' name='email'>
                        <button data-qa='signup-button'>Signup</button></form>
                        """;
                if (path.equals("/signup")) {
                    signups++;
                    body = registrationDocument(source.registration());
                }
                if (path.equals("/create_account")) {
                    creations++;
                    body = "<h2 class='title text-center'>ACCOUNT CREATED!</h2><a data-qa='continue-button' href='/'>Continue</a>";
                }
            }
            if (document == Document.INVALID_CART || document == Document.VALID_CART) {
                if (path.equals("/")) body += "<form method='post' action='/view_cart'><button id='add'>Add once</button></form>";
                if (path.equals("/view_cart")) body += cartDocument(source.catalog(), document == Document.VALID_CART);
            }
            route.fulfill(new Route.FulfillOptions().setStatus(busy ? 503 : 200).setContentType("text/html").setBody(body));
        });
        var profile = AutoExEvidenceProfile.forRun(ReportEvidenceFlowMode.SCREEN_CHECKPOINTS,
                ReportScreenshotPolicy.FINAL_AND_FAILURE);
        var output = Path.of(System.getProperty("atf.test.reportDirectory", "target/cleanup-reports")).toAbsolutePath();
        reports = AutoExReportSession.openWithEnvironment(output.resolve("cleanup"), AutoExReportSession.fileName("owned-recovery"),
                AtfConsoleSink.noOp(), profile, AutoExReportSystemMetadata.snapshot(config, profile, apiEnabled, "COMPONENT"));
        var scenario = reports.report().startScenario("OwnedCleanupFixture", getClass().getPackageName(),
                "owned account cleanup in a controlled browser", List.of("component", "cleanup", "local-browser"));
        run = new AutoExE2eRun(config, browser, scenario, scenario.startPhase("TEST BODY"), profile, apiEnabled,
                AutoExRunLifecycleAdapter.INSTANCE);
        run.navigate(base);
        if (document != Document.ACCOUNT_SETUP) {
            new AccountPage(run).trackDisposableAccountCreation(account);
            run.accountLifecycle().creationConfirmed();
        }
    }
    void registerAccount() { new AccountPage(run).registerFromStore(account); }
    void failAfterAccountSetup(IllegalStateException failure) {
        registerAccount();
        throw failure;
    }
    private static String registrationDocument(AutoExConfiguration.Registration profile) {
        var html = new StringBuilder("<form method='post' action='/create_account'><input type='radio' id='id_gender1'>");
        for (String field : List.of("password", "first_name", "last_name", "company", "address", "address2",
                "state", "city", "zipcode", "mobile_number")) {
            html.append("<input data-qa='").append(field).append("' name='").append(field).append("'>");
        }
        html.append(selectDocument("days", profile.birthDay())).append(selectDocument("months", profile.birthMonth()))
                .append(selectDocument("years", profile.birthYear())).append(selectDocument("country", profile.country()));
        return html.append("<button data-qa='create-account'>Create account</button></form>").toString();
    }
    private static String selectDocument(String field, String value) {
        String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace("'", "&#39;");
        return "<select data-qa='" + field + "' name='" + field + "'><option>" + escaped + "</option></select>";
    }
    Throwable failPayment() {
        browser.page().locator("#pay").click();
        return assertThrows(AutoExStoreUnavailableException.class,
                () -> run.text("#not-rendered", "payment page must be available"));
    }
    Throwable rejectOrderConfirmation() {
        run.click("#pay", "the configured demo payment is submitted once");
        String observed = browser.page().locator("#confirmation").innerText();
        return assertThrows(UiCheckpointFailure.class, () -> run.checkpoint(
                AutoExPurchaseTraceStep.ORDER_CONFIRMED, List.of(AutoExStoreAssertions.orderPlaced(observed))));
    }
    AutoExCartResult checkoutCart() {
        run.click("#add", "the configured product is added once");
        var product = run.configuration().catalog();
        return AutoExCartFlow.checkout(new AutoExCartUiFlow(new CartPage(run), product),
                AutoExCheckoutAssertions.expectedLine(product, product.expectedTotal()),
                result -> run.checkpoint(AutoExPurchaseTraceStep.CART_VERIFIED, AutoExCheckoutAssertions.cart(result)));
    }
    private static String cartDocument(AutoExConfiguration.Catalog product, boolean valid) {
        return """
                <h1>Controlled cart</h1><table><tbody><tr id='product-%s'>
                <td><h4>%s</h4></td><td class='cart_price'>%s</td>
                <td class='cart_quantity'><button>%s</button></td><td class='cart_total'>%s</td>
                </tr></tbody></table><a class='check_out' href='/checkout'>Proceed to checkout</a>
                """.formatted(product.productId(), product.productName(),
                valid ? product.unitPrice() : "Rs. 999", product.quantity(), product.expectedTotal());
    }
    String failurePng() {
        return UiScreenValidation.capture(browser.page(),
                UiScreenValidationConfiguration.defaults(), "original failure",
                List.of(browser.page().locator(AutoExSelectors.SENSITIVE_FIELDS))).base64Png();
    }
    void markBrowser() { browser.page().evaluate("localStorage.setItem('failure-owner','failed')"); }
    Object browserMarker() { return browser.page().evaluate("localStorage.getItem('failure-owner')"); }
    void completeOwnedAccount() {
        new AccountPage(run).accountDeletionResult(account);
    }
    String html() throws Exception { reports.close(); return Files.readString(reports.report().reportPath()); }
    @Override public void close() { try (reports; browser) { } }
}

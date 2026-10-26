package atf.shop.config;

import atf.core.config.ApiTransportMode;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.ui.config.UiBrowserConfiguration;
import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.config.api.ShopApiPrecondition;
import atf.shop.config.model.ShopAccountFixturePolicy;
import atf.shop.config.model.ShopCredentials;
import atf.shop.routes.ShopUiRoutes;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/** Immutable external-system policy for the Practice Software Testing suite. */
public record ShopConfiguration(
        URI baseUrl,
        Path accountFile,
        String productName,
        String productPath,
        ShopCheckoutConfiguration checkout,
        UiBrowserConfiguration browser,
        ShopUiRetryConfiguration uiRetry,
        ShopValidationProfile validationProfile,
        ShopApiConfiguration api,
        Path uiReportDirectory,
        Path apiReportDirectory,
        boolean authenticatedE2eEnabled,
        ShopReportConfiguration reporting) {
    public static final String DYNAMIC_PRODUCT_PATH = "auto";

    public ShopConfiguration {
        baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        accountFile = Objects.requireNonNull(accountFile, "accountFile").toAbsolutePath().normalize();
        productName = required(productName, "productName");
        productPath = required(productPath, "productPath");
        checkout = Objects.requireNonNull(checkout, "checkout");
        browser = Objects.requireNonNull(browser, "browser");
        uiRetry = Objects.requireNonNull(uiRetry, "uiRetry");
        validationProfile = Objects.requireNonNull(validationProfile, "validationProfile");
        api = Objects.requireNonNull(api, "api");
        reporting = Objects.requireNonNull(reporting, "reporting");
        if (authenticatedE2eEnabled && !api.authenticatedPrecondition()) {
            throw new IllegalArgumentException(
                    "authenticatedE2eEnabled requires api.precondition=authenticated");
        }
        uiReportDirectory = Objects.requireNonNull(uiReportDirectory, "uiReportDirectory")
                .toAbsolutePath().normalize();
        apiReportDirectory = Objects.requireNonNull(apiReportDirectory, "apiReportDirectory")
                .toAbsolutePath().normalize();
    }

    /** Compatibility constructor for UI-only callers. */
    public ShopConfiguration(URI baseUrl, Path accountFile, String productName,
                                     String productPath, ShopCheckoutConfiguration checkout,
                                     UiBrowserConfiguration browser,
                                     ShopValidationProfile validationProfile) {
        this(baseUrl, accountFile, productName, productPath, checkout, browser,
                ShopUiRetryConfiguration.none(), validationProfile,
                ShopApiConfiguration.defaults(),
                Path.of("artifacts", "reports", "ui"), Path.of("artifacts", "reports", "api"), false,
                ShopReportConfiguration.defaults());
    }

    /** Compatibility constructor for callers that already provide the complete pre-retry shape. */
    public ShopConfiguration(URI baseUrl, Path accountFile, String productName,
                                     String productPath, ShopCheckoutConfiguration checkout,
                                     UiBrowserConfiguration browser,
                                     ShopValidationProfile validationProfile,
                                     ShopApiConfiguration api,
                                     Path uiReportDirectory, Path apiReportDirectory,
                                     boolean authenticatedE2eEnabled) {
        this(baseUrl, accountFile, productName, productPath, checkout, browser,
                ShopUiRetryConfiguration.none(), validationProfile, api,
                uiReportDirectory, apiReportDirectory, authenticatedE2eEnabled,
                ShopReportConfiguration.defaults());
    }

    /** Compatibility constructor for callers that already provide UI retry policy. */
    public ShopConfiguration(URI baseUrl, Path accountFile, String productName,
                                     String productPath, ShopCheckoutConfiguration checkout,
                                     UiBrowserConfiguration browser,
                                     ShopUiRetryConfiguration uiRetry,
                                     ShopValidationProfile validationProfile,
                                     ShopApiConfiguration api,
                                     Path uiReportDirectory, Path apiReportDirectory,
                                     boolean authenticatedE2eEnabled) {
        this(baseUrl, accountFile, productName, productPath, checkout, browser, uiRetry,
                validationProfile, api, uiReportDirectory, apiReportDirectory,
                authenticatedE2eEnabled, ShopReportConfiguration.defaults());
    }

    public boolean accountConfigured() {
        if (!accountPolicy().accepts(accountFile)) return false;
        if (!java.nio.file.Files.isRegularFile(accountFile)) return false;
        try {
            return ShopCredentials.load(accountFile).hasLiveAccountCandidate();
        } catch (IllegalArgumentException | IllegalStateException invalidFixture) {
            // An unreadable, incomplete or reserved-domain fixture must fail closed.
            return false;
        }
    }

    public boolean authenticatedE2eAllowed() {
        return authenticatedE2eEnabled && accountConfigured();
    }

    /**
     * Creates the explicit live-auth launch profile used by the IntelliJ E2E
     * annotation. YAML remains the source for the business fixture; this
     * method only promotes the selected test into an authenticated live run
     * and applies the browser settings declared by that test.
     */
    public ShopConfiguration forAuthenticatedE2e(Path fixture, boolean headless, int zoomPercent) {
        UiBrowserConfiguration selectedBrowser = new UiBrowserConfiguration(
                browser.baseUrl(), browser.browser(), headless, browser.timeout(),
                browser.navigationTimeout(), browser.viewportWidth(), browser.viewportHeight(),
                zoomPercent, browser.screenshotOnFailure(), browser.retryPolicy(), browser.networkInspection());
        ShopApiConfiguration selectedApi = new ShopApiConfiguration(
                api.endpoint(), ApiTransportMode.LIVE, ShopApiPrecondition.AUTHENTICATED,
                api.timeout(), api.evidenceBinding());
        return new ShopConfiguration(baseUrl, fixture, productName, productPath, checkout,
                selectedBrowser, uiRetry, validationProfile, selectedApi, uiReportDirectory,
                apiReportDirectory, true, reporting);
    }

    /** Local registered-account files are restricted to this module's test resources. */
    public ShopAccountFixturePolicy accountPolicy() {
        return ShopAccountFixturePolicy.SOURCE_TEST_RESOURCE;
    }

    public URI uri(String path) {
        String suffix = path == null ? "" : path.trim();
        if (suffix.isEmpty()) return baseUrl;
        return baseUrl.resolve(suffix.startsWith("/") ? suffix.substring(1) : suffix);
    }

    /** True when the external catalog owns the product id and may rotate it. */
    public boolean usesDynamicProductPath() {
        return catalogSelection().usesDynamicProductPath();
    }

    public String productPathExpectation() {
        return catalogSelection().productPathExpectation();
    }

    /** Projects only the purchase inputs required by the catalog stage. */
    public CatalogSelection catalogSelection() {
        return new CatalogSelection(baseUrl, productName, productPath, checkout.quantity());
    }

    /** Returns the same runtime fixture with a profile-scoped evidence policy. */
    public ShopConfiguration withValidationProfile(ShopValidationProfile profile) {
        ShopValidationProfile selected = Objects.requireNonNull(profile, "profile");
        ShopApiEvidenceBinding binding = selected.apiEvidenceRequired()
                ? api.evidenceBinding() : ShopApiEvidenceBinding.none();
        if (selected.apiEvidenceRequired() && binding.checkpoints().isEmpty()
                && !validationProfile.apiEvidenceRequired()) {
            binding = ShopApiEvidenceBinding.all();
        }
        ShopApiConfiguration selectedApi = new ShopApiConfiguration(
                api.endpoint(), api.mode(), api.precondition(), api.timeout(), binding);
        return new ShopConfiguration(baseUrl, accountFile, productName, productPath,
                checkout, browser, uiRetry, selected, selectedApi,
                uiReportDirectory, apiReportDirectory, authenticatedE2eEnabled, reporting);
    }

    /** Returns this fixture with a suite-scoped report capture policy. */
    public ShopConfiguration withScreenshotPolicy(ReportScreenshotPolicy screenshotPolicy) {
        ShopReportConfiguration selectedReporting = new ShopReportConfiguration(
                reporting.systemBodyPreviewCharacters(), Objects.requireNonNull(screenshotPolicy,
                        "screenshotPolicy"), reporting.flowMode(), reporting.actionDetailsMode());
        return new ShopConfiguration(baseUrl, accountFile, productName, productPath, checkout,
                browser, uiRetry, validationProfile, api, uiReportDirectory, apiReportDirectory,
                authenticatedE2eEnabled, selectedReporting);
    }

    public boolean matchesProductPath(String selectedPath) {
        return catalogSelection().matchesProductPath(selectedPath);
    }

    public record CatalogSelection(URI baseUrl, String productName, String productPath, int quantity) {
        public CatalogSelection {
            baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
            productName = required(productName, "productName");
            productPath = required(productPath, "productPath");
            if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
        }

        public URI homeUrl() { return uri(ShopUiRoutes.HOME); }

        public boolean usesDynamicProductPath() {
            return DYNAMIC_PRODUCT_PATH.equalsIgnoreCase(productPath);
        }

        public String productPathExpectation() {
            return usesDynamicProductPath()
                    ? ShopUiRoutes.PRODUCT_PATTERN + " (dynamic fixture)"
                    : productPath;
        }

        public boolean matchesProductPath(String selectedPath) {
            if (selectedPath == null || !selectedPath.startsWith(ShopUiRoutes.PRODUCT_PREFIX)) return false;
            return usesDynamicProductPath() || productPath.equals(selectedPath);
        }

        private URI uri(String path) {
            String suffix = path == null ? "" : path.trim();
            if (suffix.isEmpty()) return baseUrl;
            return baseUrl.resolve(suffix.startsWith("/") ? suffix.substring(1) : suffix);
        }
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

}



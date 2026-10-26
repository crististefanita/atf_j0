package atf.shop.config;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryOperation;
import atf.ui.config.UiRetryPolicy;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.core.config.ApiTransportMode;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.config.api.ShopApiPrecondition;
import atf.shop.config.model.ShopAccountFixturePolicy;
import atf.shop.config.model.ShopBillingAddress;
import atf.shop.config.model.ShopGuest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.StreamSupport;
import java.util.LinkedHashSet;

/** YAML-first setup with an explicit module-owned demo account fixture. */
public final class ShopConfigurationLoader {
    public static final String RESOURCE = "atf-practice-shop.yaml";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private ShopConfigurationLoader() { }

    public static ShopConfiguration load() {
        try (var stream = openConfiguration()) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + RESOURCE
                        + "; practiceShop.baseUrl must be supplied by the module YAML");
            }
            return load(stream);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load " + RESOURCE, error);
        }
    }

    /** Loads an explicit YAML file for component tests and controlled launches. */
    public static ShopConfiguration load(Path configurationFile) {
        try (InputStream stream = Files.newInputStream(configurationFile)) {
            return load(stream);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot load Practice Shop configuration: " + configurationFile,
                    error);
        }
    }

    /** Resolves and validates a module-owned account fixture for test annotations. */
    public static Path resolveAccountFile(String configured) {
        return accountPath(configured);
    }

    /** Resolves an ignored, module-local properties file used only by explicit register-per-run opt-in. */
    public static Path resolveRegistrationProfileFile(String configured) {
        Path path = resolvePath(configured);
        ShopAccountFixturePolicy.SOURCE_TEST_RESOURCE.validate(path);
        if (!isShopTestResource(path)) {
            throw new IllegalArgumentException(
                    "Registration profiles must be under atf-test-practice-shop/src/test/resources");
        }
        if (!path.getFileName().toString().endsWith(".local.properties")) {
            throw new IllegalArgumentException(
                    "Registration profiles must use a module-local .local.properties file ignored by Git");
        }
        return path;
    }

    private static boolean isShopTestResource(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        for (int index = 0; index + 3 < normalized.getNameCount(); index++) {
            if (normalized.getName(index).toString().equalsIgnoreCase("atf-test-practice-shop")
                    && normalized.getName(index + 1).toString().equalsIgnoreCase("src")
                    && normalized.getName(index + 2).toString().equalsIgnoreCase("test")
                    && normalized.getName(index + 3).toString().equalsIgnoreCase("resources")) {
                return true;
            }
        }
        return false;
    }

    private static ShopConfiguration load(InputStream stream) throws IOException {
            Path account = Path.of("atf-test-practice-shop", "src", "test", "resources",
                    "practice-software-testing-account.properties");
            JsonNode root = YAML.readTree(stream).path("practiceShop");
            URI base = URI.create(requiredText(root, "baseUrl"));
            Path configuredAccount = accountPath(text(root, "accountFile", account.toString()));
            String product = text(root, "productName", "Pliers");
            String path = text(root, "productPath", ShopConfiguration.DYNAMIC_PRODUCT_PATH);
            ShopValidationProfile validationProfile = ShopValidationProfile.from(
                    text(root.path("validation"), "profile", "ui-api"));
            ShopApiEvidenceBinding evidenceBinding = apiEvidenceBinding(
                    root.path("validation"), validationProfile);
            ShopUiRetryConfiguration uiRetry = retry(root.path("retry"));
            UiNetworkInspectionConfiguration networkInspection = networkInspection(
                    root.path("networkInspector"));
            JsonNode api = root.path("api");
            ShopApiConfiguration apiConfiguration = new ShopApiConfiguration(
                    URI.create(text(api, "endpoint", ShopApiConfiguration.defaults().endpoint().toString())),
                    ApiTransportMode.parse(text(api, "mode", "acceptance")),
                    ShopApiPrecondition.from(text(api, "precondition", "authenticated")),
                    duration(api, "timeout", Duration.ofSeconds(20)), evidenceBinding);
            JsonNode checkout = root.path("checkout");
            JsonNode guest = checkout.path("guest");
            JsonNode billing = checkout.path("billing");
            ShopCheckoutConfiguration checkoutConfiguration =
                    new ShopCheckoutConfiguration(
                            integer(checkout, "quantity", 2),
                            new ShopGuest(text(guest, "email",
                                    "atf.practice.shop@example.test"),
                                    text(guest, "firstName", "ATF"),
                                    text(guest, "lastName", "Shop")),
                            new ShopBillingAddress(text(billing, "country", "Romania"),
                                    text(billing, "postalCode", "010101"),
                                    text(billing, "houseNumber", "10"),
                                    text(billing, "street", "Test Street"),
                                    text(billing, "city", "Bucharest"),
                                    text(billing, "state", "Bucharest")),
                            text(checkout, "paymentMethod", "Cash on Delivery"));
            JsonNode reports = root.path("reports");
            Path uiReports = resolveDirectory(text(reports, "ui", "artifacts/reports/ui"));
            Path apiReports = resolveDirectory(text(reports, "api", "artifacts/reports/api"));
            boolean legacyCheckpointScreenshots = bool(root, "screenshotOnFailure", true);
            ShopReportConfiguration reportConfiguration = reportConfiguration(
                    root.path("reporting"), legacyCheckpointScreenshots);
            boolean authenticatedE2e = bool(root, "authenticatedE2eEnabled", false);
            return override(new ShopConfiguration(base, configuredAccount, product, path,
                    checkoutConfiguration,
                    new UiBrowserConfiguration(base, text(root, "browser", "chromium"),
                            bool(root, "headless", true), duration(root, "timeout", Duration.ofSeconds(20)),
                            duration(root, "navigationTimeout", Duration.ofSeconds(45)),
                            integer(root.path("viewport"), "width", 1440),
                            integer(root.path("viewport"), "height", 1000),
                            integer(root, "zoomPercent", 100),
                            legacyCheckpointScreenshots,
                            uiRetry.policy(), networkInspection), uiRetry, validationProfile,
                    apiConfiguration, uiReports, apiReports, authenticatedE2e, reportConfiguration));
    }

    private static ShopUiRetryConfiguration retry(JsonNode retry) {
        int maxAttempts = positiveInteger(retry, "maxAttempts", UiRetryPolicy.DEFAULT_MAX_ATTEMPTS,
                "practiceShop.retry.maxAttempts");
        Set<UiRetryOperation> operations = retryOperations(retry);
        return new ShopUiRetryConfiguration(maxAttempts,
                duration(retry, "backoff", UiRetryPolicy.DEFAULT_BACKOFF),
                operations, text(retry, "reason", UiRetryPolicy.DEFAULT_REASON));
    }

    private static Set<UiRetryOperation> retryOperations(JsonNode retry) {
        JsonNode configured = retry.path("eligibleOperations");
        if (configured.isMissingNode() || configured.isNull()) return Set.of();
        if (!configured.isArray()) {
            throw new IllegalArgumentException(
                    "Configuration key practiceShop.retry.eligibleOperations must be an array");
        }
        EnumSet<UiRetryOperation> operations = EnumSet.noneOf(UiRetryOperation.class);
        StreamSupport.stream(configured.spliterator(), false).forEach(node -> {
            if (!node.isTextual() || node.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Configuration key practiceShop.retry.eligibleOperations must contain text values");
            }
            operations.add(UiRetryOperation.from(node.asText()));
        });
        return operations;
    }

    private static UiNetworkInspectionConfiguration networkInspection(JsonNode network) {
        boolean enabled = bool(network, "enabled", false);
        Set<String> hosts = textSet(network.path("allowedHosts"),
                "practiceShop.networkInspector.allowedHosts");
        UiNetworkSettlementPolicy settlement = new UiNetworkSettlementPolicy(
                bool(network.path("settlement"), "enabled", false),
                duration(network.path("settlement"), "timeout", Duration.ofSeconds(10)),
                duration(network.path("settlement"), "pollInterval", Duration.ofMillis(100)));
        return new UiNetworkInspectionConfiguration(enabled, hosts,
                positiveInteger(network, "maxObservations", 256,
                        "practiceShop.networkInspector.maxObservations"),
                positiveInteger(network, "maxBodyPreviewCharacters", 12_000,
                        "practiceShop.networkInspector.maxBodyPreviewCharacters"),
                bool(network, "collapseConsecutiveSignatures", true), settlement);
    }

    private static ShopReportConfiguration reportConfiguration(JsonNode reporting,
                                                                        boolean legacyCheckpointScreenshots) {
        String legacyPolicy = legacyCheckpointScreenshots
                ? ReportScreenshotPolicy.EACH_CHECKPOINT.configValue()
                : ReportScreenshotPolicy.FAILURE_ONLY.configValue();
        return new ShopReportConfiguration(
                positiveInteger(reporting, "systemBodyPreviewCharacters",
                        ShopReportConfiguration.DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS,
                        "practiceShop.reporting.systemBodyPreviewCharacters"),
                ReportScreenshotPolicy.fromConfig(text(reporting, "screenshotPolicy", legacyPolicy)));
    }

    private static Set<String> textSet(JsonNode configured, String path) {
        if (configured.isMissingNode() || configured.isNull()) return Set.of();
        if (!configured.isArray()) {
            throw new IllegalArgumentException("Configuration key " + path + " must be an array");
        }
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode node : configured) {
            if (!node.isTextual() || node.asText().isBlank()) {
                throw new IllegalArgumentException("Configuration key " + path
                        + " must contain text values");
            }
            values.add(node.asText().trim());
        }
        return Set.copyOf(values);
    }

    private static ShopApiEvidenceBinding apiEvidenceBinding(
            JsonNode validation, ShopValidationProfile profile) {
        JsonNode configured = validation.path("apiEvidence");
        if (configured.isMissingNode() || configured.isNull()) {
            return ShopApiEvidenceBinding.forProfile(profile, null);
        }
        if (!configured.isArray()) {
            throw new IllegalArgumentException(
                    "Configuration key practiceShop.validation.apiEvidence must be an array");
        }
        EnumSet<ShopApiCheckpoint> checkpoints = EnumSet.noneOf(ShopApiCheckpoint.class);
        for (JsonNode node : configured) {
            if (!node.isTextual() || node.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Configuration key practiceShop.validation.apiEvidence must contain text values");
            }
            checkpoints.add(ShopApiCheckpoint.from(node.asText()));
        }
        return ShopApiEvidenceBinding.forProfile(profile, checkpoints);
    }

    /**
     * The application fixture is owned by this module. The filesystem fallback
     * keeps local launches from an IDE and a staging checkout deterministic.
     */
    private static InputStream openConfiguration() throws IOException {
        InputStream classpath = ShopConfigurationLoader.class.getClassLoader()
                .getResourceAsStream(RESOURCE);
        if (classpath != null) return classpath;

        Path current = Path.of(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
        while (current != null) {
            Path file = current.resolve("atf-test-practice-shop/src/test/resources").resolve(RESOURCE);
            if (Files.isRegularFile(file)) return Files.newInputStream(file);
            current = current.getParent();
        }
        return null;
    }

    private static ShopConfiguration override(ShopConfiguration base) {
        String baseUrl = System.getProperty("atf.practice.base-url", base.baseUrl().toString());
        String account = System.getProperty("atf.practice.account-file",
                System.getenv().getOrDefault("ATF_PRACTICE_ACCOUNT_FILE", "")).trim();
        Path accountFile = account.isEmpty() ? base.accountFile() : accountPath(account);
        String product = System.getProperty("atf.practice.product-name", base.productName());
        String productPath = System.getProperty("atf.practice.product-path", base.productPath());
        String profile = System.getProperty("atf.practice.validation-profile", base.validationProfile().name());
        ShopValidationProfile validationProfile = ShopValidationProfile.from(profile);
        String checkpoints = System.getProperty("atf.practice.api-evidence-checkpoints");
        ShopApiEvidenceBinding evidenceBinding = checkpoints == null
                ? base.api().evidenceBinding()
                : ShopApiEvidenceBinding.fromCsv(validationProfile, checkpoints);
        String apiEndpoint = System.getProperty("atf.practice.api-endpoint", base.api().endpoint().toString());
        String apiMode = System.getProperty("atf.practice.api-mode", base.api().mode().name());
        String apiPrecondition = System.getProperty("atf.practice.api-precondition",
                base.api().precondition().name());
        String uiReports = System.getProperty("atf.practice.ui-report-directory",
                base.uiReportDirectory().toString());
        String apiReports = System.getProperty("atf.practice.api-report-directory",
                base.apiReportDirectory().toString());
        boolean authenticatedE2e = Boolean.parseBoolean(System.getProperty(
                "atf.practice.authenticated-e2e", Boolean.toString(base.authenticatedE2eEnabled())));
        URI uri = URI.create(baseUrl);
        UiBrowserConfiguration browser = new UiBrowserConfiguration(uri, base.browser().browser(),
                base.browser().headless(), base.browser().timeout(), base.browser().navigationTimeout(),
                base.browser().viewportWidth(), base.browser().viewportHeight(), base.browser().zoomPercent(),
                base.browser().screenshotOnFailure(), base.browser().retryPolicy(),
                base.browser().networkInspection());
        return new ShopConfiguration(uri, accountFile, product, productPath,
                base.checkout(), browser, base.uiRetry(), validationProfile,
                new ShopApiConfiguration(URI.create(apiEndpoint), ApiTransportMode.parse(apiMode),
                        ShopApiPrecondition.from(apiPrecondition), base.api().timeout(), evidenceBinding),
                resolveDirectory(uiReports), resolveDirectory(apiReports), authenticatedE2e,
                base.reporting());
    }

    private static String text(JsonNode parent, String key, String fallback) {
        JsonNode value = parent.path(key);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : fallback;
    }

    private static String requiredText(JsonNode parent, String key) {
        JsonNode value = parent.path(key);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Configuration key practiceShop." + key
                    + " must be a non-blank YAML value");
        }
        return value.asText().trim();
    }

    private static Path resolvePath(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) return path.normalize();
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(path).normalize();
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
                .resolve(path).normalize();
    }

    private static Path resolveDirectory(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) return path.normalize();
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("atf-reporting/pom.xml"))) {
                return current.resolve(path).normalize();
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
                .resolve(path).normalize();
    }

    private static Path accountPath(String configured) {
        Path path = resolvePath(configured);
        ShopAccountFixturePolicy.SOURCE_TEST_RESOURCE.validate(path);
        return path;
    }

    private static boolean bool(JsonNode parent, String key, boolean fallback) {
        JsonNode value = parent.path(key);
        return value.isBoolean() ? value.booleanValue() : fallback;
    }

    private static int integer(JsonNode parent, String key, int fallback) {
        JsonNode value = parent.path(key);
        return value.canConvertToInt() && value.intValue() > 0 ? value.intValue() : fallback;
    }

    private static int positiveInteger(JsonNode parent, String key, int fallback, String path) {
        JsonNode value = parent.path(key);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (value.canConvertToInt() && value.intValue() > 0) return value.intValue();
        throw new IllegalArgumentException("Configuration key " + path
                + " must be a positive integer");
    }

    private static Duration duration(JsonNode parent, String key, Duration fallback) {
        JsonNode value = parent.path(key);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Configuration key " + key
                    + " must be an ISO-8601 duration; value=" + value);
        }
        try {
            return Duration.parse(value.asText().trim());
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("Configuration key " + key
                    + " has malformed duration: " + value.asText(), error);
        }
    }
}



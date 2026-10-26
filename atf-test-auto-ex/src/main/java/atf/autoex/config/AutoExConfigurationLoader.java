package atf.autoex.config;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkCapturePolicy;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.ui.config.UiRetryPolicy;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.Optional;

/** Reads module-owned YAML once into validated browser and demo data. */
public final class AutoExConfigurationLoader {
    public static final String CONFIG_PROPERTY = "atf.autoex.config";
    public static final String DEFAULT_RESOURCE = "atf-auto-ex.yaml";

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private AutoExConfigurationLoader() { }

    public static AutoExConfiguration load() {
        String selected = System.getProperty(CONFIG_PROPERTY, "").trim();
        if (!selected.isEmpty()) {
            try {
                return load(Path.of(selected));
            } catch (InvalidPathException error) {
                throw new IllegalStateException("Cannot load Automation Exercise configuration from -D"
                        + CONFIG_PROPERTY + "=" + selected, error);
            }
        }
        InputStream defaultResource;
        try {
            defaultResource = openDefaultResource();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot locate " + DEFAULT_RESOURCE, error);
        }
        if (defaultResource == null) {
            throw new IllegalStateException("Missing " + DEFAULT_RESOURCE
                    + "; expected it on the test classpath or under atf-test-auto-ex/src/test/resources");
        }
        String source;
        try (InputStream resource = defaultResource) {
            source = new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read " + DEFAULT_RESOURCE, error);
        }
        return parse(source);
    }

    public static AutoExConfiguration load(Path path) {
        Path source = java.util.Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) {
            throw new IllegalStateException("Automation Exercise configuration is missing: " + source);
        }
        String yaml;
        try {
            yaml = Files.readString(source);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Automation Exercise configuration: " + source, error);
        }
        try {
            return parse(yaml);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Invalid Automation Exercise configuration: " + source, error);
        }
    }

    /** Classpath first; filesystem fallback supports direct IDE launches from a module or reactor root. */
    private static InputStream openDefaultResource() throws IOException {
        InputStream classpath = AutoExConfigurationLoader.class.getClassLoader()
                .getResourceAsStream(DEFAULT_RESOURCE);
        if (classpath != null) return classpath;

        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            Path moduleResource = current.resolve("src/test/resources").resolve(DEFAULT_RESOURCE);
            if (Files.isRegularFile(moduleResource)) return Files.newInputStream(moduleResource);
            Path reactorResource = current.resolve("atf-test-auto-ex/src/test/resources")
                    .resolve(DEFAULT_RESOURCE);
            if (Files.isRegularFile(reactorResource)) return Files.newInputStream(reactorResource);
            current = current.getParent();
        }
        return null;
    }

    private static AutoExConfiguration parse(String source) {
        try {
            JsonNode root = YAML.readTree(source);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("YAML document must contain an object root");
            }
            rejectUnknownFields(root, "root", "automationExercise");
            JsonNode site = requiredNode(root, "automationExercise");
            rejectUnknownFields(site, "automationExercise", "baseUrl", "e2eEnabled", "browser",
                    "reports", "catalog", "registration", "payment");
            URI baseUrl = URI.create(requiredText(site, "baseUrl"));
            if (baseUrl.getHost() == null || !("https".equalsIgnoreCase(baseUrl.getScheme())
                    || "http".equalsIgnoreCase(baseUrl.getScheme()))) {
                throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URI");
            }
            JsonNode browser = requiredNode(site, "browser");
            rejectUnknownFields(browser, "automationExercise.browser", "engine", "headless", "timeout",
                    "navigationTimeout", "viewport", "zoomPercent", "networkInspection", "storeAvailability");
            JsonNode viewport = requiredNode(browser, "viewport");
            rejectUnknownFields(viewport, "automationExercise.browser.viewport", "width", "height");
            UiBrowserConfiguration browserConfiguration = new UiBrowserConfiguration(baseUrl,
                    text(browser, "engine", "chromium"), bool(browser, "headless", true),
                    duration(browser, "timeout", "PT20S"),
                    duration(browser, "navigationTimeout", "PT45S"),
                    integer(viewport, "width", 1440), integer(viewport, "height", 1000),
                    integer(browser, "zoomPercent", UiBrowserConfiguration.DEFAULT_ZOOM_PERCENT),
                    true, UiRetryPolicy.none(), networkInspection(browser, baseUrl));

            JsonNode reports = requiredNode(site, "reports");
            JsonNode catalog = requiredNode(site, "catalog");
            JsonNode registration = requiredNode(site, "registration");
            JsonNode payment = requiredNode(site, "payment");
            rejectUnknownFields(reports, "automationExercise.reports", "uiBaseDirectory", "evidence");
            rejectUnknownFields(catalog, "automationExercise.catalog", "searchTerm", "productId",
                    "productName", "unitPrice", "quantity");
            rejectUnknownFields(registration, "automationExercise.registration", "birthDay", "birthMonth",
                    "birthYear", "firstName", "lastName", "company", "address1", "address2", "country",
                    "state", "city", "postalCode", "mobileNumber");
            rejectUnknownFields(payment, "automationExercise.payment", "nameOnCard", "cardNumber", "cvc",
                    "expiryMonth", "expiryYear");
            return new AutoExConfiguration(baseUrl, bool(site, "e2eEnabled", false), browserConfiguration,
                    Path.of(requiredText(reports, "uiBaseDirectory")),
                    new AutoExConfiguration.Catalog(requiredText(catalog, "searchTerm"),
                            requiredText(catalog, "productId"), requiredText(catalog, "productName"),
                            requiredText(catalog, "unitPrice"), integer(catalog, "quantity", 1)),
                    new AutoExConfiguration.Registration(requiredText(registration, "birthDay"),
                            requiredText(registration, "birthMonth"),
                            requiredText(registration, "birthYear"), requiredText(registration, "firstName"),
                            requiredText(registration, "lastName"), requiredText(registration, "company"),
                            requiredText(registration, "address1"), requiredText(registration, "address2"),
                            requiredText(registration, "country"), requiredText(registration, "state"),
                            requiredText(registration, "city"), requiredText(registration, "postalCode"),
                            requiredText(registration, "mobileNumber")),
                    new AutoExConfiguration.Payment(requiredText(payment, "nameOnCard"),
                            requiredText(payment, "cardNumber"), requiredText(payment, "cvc"),
                            requiredText(payment, "expiryMonth"), requiredText(payment, "expiryYear")),
                    storeAvailability(browser), evidence(reports));
        } catch (IOException | RuntimeException error) {
            if (error instanceof IllegalArgumentException argument) throw argument;
            throw new IllegalStateException("Invalid Automation Exercise configuration", error);
        }
    }

    private static JsonNode requiredNode(JsonNode parent, String name) {
        JsonNode node = parent.path(name);
        if (!node.isObject()) throw new IllegalArgumentException("Missing configuration object: " + name);
        return node;
    }

    private static UiNetworkInspectionConfiguration networkInspection(JsonNode browser, URI baseUrl) {
        JsonNode network = browser.path("networkInspection");
        if (!network.isMissingNode() && !network.isObject()) {
            throw new IllegalArgumentException("networkInspection must be a configuration object");
        }
        rejectUnknownFields(network, "automationExercise.browser.networkInspection",
                "includeDocuments", "maxObservations", "maxBodyPreviewCharacters");
        UiNetworkCapturePolicy policy = UiNetworkCapturePolicy.asynchronous();
        if (bool(network, "includeDocuments", false)) policy = policy.includingDocuments();
        return new UiNetworkInspectionConfiguration(false, Set.of(baseUrl.getHost()),
                integer(network, "maxObservations", 256),
                integer(network, "maxBodyPreviewCharacters", 12000), true,
                UiNetworkSettlementPolicy.disabled(), policy);
    }

    private static UiRetryPolicy storeAvailability(JsonNode browser) {
        JsonNode settings = browser.path("storeAvailability");
        if (!settings.isMissingNode() && !settings.isObject()) {
            throw new IllegalArgumentException("storeAvailability must be a configuration object");
        }
        rejectUnknownFields(settings, "automationExercise.browser.storeAvailability", "maxAttempts", "backoff");
        var defaults = AutoExConfiguration.DEFAULT_STORE_AVAILABILITY_POLICY;
        return new UiRetryPolicy(integer(settings, "maxAttempts", defaults.maxAttempts()),
                duration(settings, "backoff", defaults.backoff().toString()),
                defaults.eligibleOperations(), defaults.reason());
    }

    private static AutoExEvidenceSettings evidence(JsonNode reports) {
        JsonNode settings = reports.path("evidence");
        if (!settings.isMissingNode() && !settings.isObject()) {
            throw new IllegalArgumentException("reports.evidence must be a configuration object");
        }
        rejectUnknownFields(settings, "automationExercise.reports.evidence",
                "flowMode", "screenshotPolicy", "actionDetailsMode");
        var defaults = AutoExEvidenceSettings.defaults();
        var screenshots = settings.has("screenshotPolicy")
                ? Optional.of(ReportScreenshotPolicy.fromConfig(requiredText(settings, "screenshotPolicy")))
                : Optional.<ReportScreenshotPolicy>empty();
        return new AutoExEvidenceSettings(
                ReportEvidenceFlowMode.fromConfig(text(settings, "flowMode", defaults.flowMode().configValue())),
                screenshots, ReportUiActionDetailsMode.fromConfig(text(settings,
                        "actionDetailsMode", defaults.actionDetailsMode().configValue())));
    }

    private static void rejectUnknownFields(JsonNode node, String path, String... allowedFields) {
        Set<String> allowed = Set.of(allowedFields);
        node.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException("Unknown configuration key: " + path + "." + field);
            }
        });
    }

    private static String requiredText(JsonNode parent, String name) {
        JsonNode value = parent.path(name);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing configuration value: " + name);
        }
        return value.asText().trim();
    }

    private static String text(JsonNode parent, String name, String fallback) {
        JsonNode value = parent.path(name);
        return value.isMissingNode() || value.isNull() ? fallback : requiredText(parent, name);
    }

    private static Duration duration(JsonNode parent, String name, String fallback) {
        String value = text(parent, name, fallback);
        try {
            return Duration.parse(value);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Invalid ISO-8601 duration: automationExercise.browser." + name,
                    error);
        }
    }

    private static int integer(JsonNode parent, String name, int fallback) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.canConvertToInt()) throw new IllegalArgumentException("Invalid integer configuration: " + name);
        return value.intValue();
    }

    private static boolean bool(JsonNode parent, String name, boolean fallback) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isBoolean()) throw new IllegalArgumentException("Invalid boolean configuration: " + name);
        return value.booleanValue();
    }
}

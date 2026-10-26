package atf.core.config.source;

import atf.core.config.ApiBootstrapConfiguration;
import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.config.ApiTransportPolicy;
import atf.core.files.AtfPathLocator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Composition-root loader for the typed API section of {@code atf.yaml}.
 * It reads once, validates once and never participates in request execution.
 *
 * <p>The source adapter lives in core because it is reusable by any ATF
 * consumer. Core still owns only the typed result; this class is the explicit
 * boundary where YAML, JVM properties and environment are admitted.</p>
 */
public final class ApiBootstrapConfigurationLoader {
    public static final String CONFIG_PROPERTY = "atf.api.config";
    public static final String LIVE_OPT_IN_PROPERTY = "atf.api.live.optIn";
    public static final String LIVE_OPT_IN_ENVIRONMENT = "ATF_API_LIVE_OPT_IN";
    public static final List<String> DEFAULT_CONFIG_NAMES = List.of("atf.yaml", "atf.yml");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private ApiBootstrapConfigurationLoader() {
    }

    public static ApiBootstrapConfiguration load() {
        String configured = System.getProperty(CONFIG_PROPERTY);
        Path explicit = configured == null || configured.isBlank() ? null : Path.of(configured);
        Path path = explicit == null ? discover(Path.of(System.getProperty("user.dir", "."))) : explicit;
        return path == null ? defaults() : load(path);
    }

    public static ApiBootstrapConfiguration load(Path path) {
        Path config = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (!Files.isRegularFile(config)) {
            throw new IllegalStateException("ATF API configuration does not exist: " + config);
        }
        try {
            JsonNode root = YAML.readTree(config.toFile());
            JsonNode api = root == null ? null : root.path("api");
            if (api == null || api.isMissingNode() || api.isNull()) return defaults();
            URI baseUri = URI.create(text(api, "baseUri", ApiEnvironment.MOCK_BASE_URI.toString()));
            String auth = text(api, "defaultAuthProfile", ApiEnvironment.NO_AUTH_PROFILE);
            Duration timeout = Duration.parse(text(api, "timeout", "PT30S"));
            Set<Integer> retryStatuses = integers(api.path("retryStatuses"));
            int maxAttempts = integer(api, "maxAttempts", 1);
            ApiSettings settings = new ApiSettings(baseUri, auth, timeout, retryStatuses, maxAttempts);
            ApiTransportMode mode = ApiTransportMode.parse(text(api, "transport", "acceptance"));
            boolean liveOptIn = liveOptIn();
            ApiTransportPolicy policy = mode == ApiTransportMode.LIVE
                    ? new ApiTransportPolicy(mode, liveOptIn)
                    : ApiTransportPolicy.acceptance();
            return new ApiBootstrapConfiguration(settings, policy);
        } catch (IllegalStateException | IllegalArgumentException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot read ATF API configuration: " + config, error);
        }
    }

    public static Path discover(Path startDirectory) {
        return AtfPathLocator.firstRegularFileUpwards(startDirectory, DEFAULT_CONFIG_NAMES);
    }

    private static ApiBootstrapConfiguration defaults() {
        return ApiBootstrapConfiguration.acceptance(
                new ApiSettings(ApiEnvironment.MOCK_BASE_URI, ApiEnvironment.NO_AUTH_PROFILE));
    }

    private static boolean liveOptIn() {
        String value = System.getProperty(LIVE_OPT_IN_PROPERTY);
        if (value == null || value.isBlank()) value = System.getenv(LIVE_OPT_IN_ENVIRONMENT);
        return value != null && Boolean.parseBoolean(value.trim());
    }

    private static String text(JsonNode parent, String name, String fallback) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.isValueNode() || value.asText().isBlank()) {
            throw new IllegalStateException("api." + name + " must be a non-blank scalar");
        }
        return value.asText().trim();
    }

    private static int integer(JsonNode parent, String name, int fallback) {
        JsonNode value = parent.path(name);
        if (value.isMissingNode() || value.isNull()) return fallback;
        if (!value.canConvertToInt() || value.intValue() <= 0) {
            throw new IllegalStateException("api." + name + " must be a positive integer");
        }
        return value.intValue();
    }

    private static Set<Integer> integers(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return Set.of();
        if (!node.isArray()) throw new IllegalStateException("api.retryStatuses must be an array");
        Set<Integer> result = new LinkedHashSet<>();
        for (JsonNode value : node) {
            if (!value.canConvertToInt()) {
                throw new IllegalStateException("api.retryStatuses values must be integers");
            }
            result.add(value.intValue());
        }
        return Set.copyOf(result);
    }
}

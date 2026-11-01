package atf.mock;
import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loads the mock-owned route catalog and never changes live HTTP settings. */
public final class AtfMockConfigurationLoader {
    public static final String CONFIG_PROPERTY = "atf.mock.config";
    public static final String CONFIG_ENVIRONMENT_VARIABLE = "ATF_MOCK_CONFIG";
    public static final List<String> DEFAULT_CONFIG_NAMES = List.of("atf-mock.yaml", "atf-mock.yml");

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);

    private AtfMockConfigurationLoader() {
    }

    public static AtfMockConfiguration load() {
        Path explicit = explicitPath();
        if (explicit != null) return load(explicit);
        Path discovered = discover(Path.of(System.getProperty("user.dir", ".")));
        return discovered == null ? AtfMockConfiguration.defaults() : load(discovered);
    }

    public static AtfMockConfiguration load(Path configPath) {
        Path absolute = Objects.requireNonNull(configPath, "configPath").toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IllegalStateException("ATF mock configuration does not exist: " + absolute);
        }
        try {
            JsonNode root = YAML.readTree(absolute.toFile());
            if (root == null || !root.isObject()) {
                throw new IllegalStateException("ATF mock configuration root must be an object: " + absolute);
            }
            JsonNode mock = root.path("mock");
            if (mock.isMissingNode() || mock.isNull()) {
                throw new IllegalStateException("ATF mock configuration must contain a root 'mock' section: " + absolute);
            }
            JsonNode routes = mock.path("routes");
            if (!routes.isArray()) {
                throw new IllegalStateException("mock.routes must be an array: " + absolute);
            }
            List<MockRoute> parsed = new ArrayList<>();
            for (JsonNode route : routes) parsed.add(readRoute(route, "mock.routes", false));
            JsonNode fallback = mock.path("fallback");
            MockRoute fallbackRoute = fallback.isMissingNode() || fallback.isNull()
                    ? AtfMockConfiguration.defaults().fallback()
                    : readRoute(fallback, "mock.fallback", true);
            return new AtfMockConfiguration(parsed, fallbackRoute);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read ATF mock configuration: " + absolute, error);
        }
    }

    public static Path discover(Path startDirectory) {
        Path current = Objects.requireNonNull(startDirectory, "startDirectory")
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(current)) current = current.getParent();
        while (current != null) {
            for (String name : DEFAULT_CONFIG_NAMES) {
                Path candidate = current.resolve(name);
                if (Files.isRegularFile(candidate)) return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static MockRoute readRoute(JsonNode node, String location, boolean fallback) {
        if (!node.isObject()) throw new IllegalStateException(location + " entry must be an object");
        String method = text(node, "method", fallback ? MockRoute.ANY_METHOD : null, location);
        String path = text(node, "path", fallback ? MockRoute.ANY_PATH : null, location);
        int status = integer(node, "status", fallback ? 500 : -1, location);
        String body = text(node, "body", "", location);
        Duration duration = duration(node, "duration", Duration.ofMillis(12), location);
        String authProfile = text(node, "auth-profile", MockRoute.ANY_AUTH_PROFILE, location);
        Map<String, String> headers = new LinkedHashMap<>();
        JsonNode headerNode = node.path("headers");
        if (!headerNode.isMissingNode() && !headerNode.isObject()) {
            throw new IllegalStateException(location + ".headers must be an object");
        }
        headerNode.fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
        if (headers.isEmpty()) headers.put(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON);
        List<ApiQueryParameter> queryParameters = new ArrayList<>();
        JsonNode queryNode = node.path("query");
        if (queryNode.isObject()) {
            queryNode.fields().forEachRemaining(entry ->
                    queryParameters.add(new ApiQueryParameter(entry.getKey(), entry.getValue().asText())));
        } else if (queryNode.isArray()) {
            int index = 0;
            for (JsonNode parameter : queryNode) {
                if (!parameter.isObject()) {
                    throw new IllegalStateException(location + ".query[" + index + "] must be an object");
                }
                String name = text(parameter, "name", null, location + ".query[" + index + "]");
                JsonNode valueNode = parameter.path("value");
                if (valueNode.isMissingNode() || valueNode.isNull() || !valueNode.isValueNode()) {
                    throw new IllegalStateException(location + ".query[" + index + "].value must be scalar");
                }
                String value = valueNode.asText();
                queryParameters.add(new ApiQueryParameter(name, value));
                index++;
            }
        } else if (!queryNode.isMissingNode() && !queryNode.isNull()) {
            throw new IllegalStateException(location + ".query must be an object or parameter array");
        }
        ApiQueryParameters parsedQuery = ApiQueryParameters.of(queryParameters);
        Map<String, String> query = parsedQuery.asMap();
        Map<String, String> requiredHeaders = new LinkedHashMap<>();
        JsonNode requiredHeadersNode = node.path("request-headers");
        if (!requiredHeadersNode.isMissingNode() && !requiredHeadersNode.isObject()) {
            throw new IllegalStateException(location + ".request-headers must be an object");
        }
        requiredHeadersNode.fields().forEachRemaining(entry ->
                requiredHeaders.put(entry.getKey(), entry.getValue().asText()));
        String requestBodyMode = text(node, "request-body-mode", MockRequestBodyMode.JSON.value(), location);
        String expectedRequestBody = optionalText(node, "request-body", location);
        return new MockRoute(method, path, status, body, headers, duration, authProfile, query,
                parsedQuery,
                requiredHeaders, requestBodyMode, expectedRequestBody);
    }

    private static String text(JsonNode node, String name, String defaultValue, String location) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) {
            if (defaultValue != null) return defaultValue;
            throw new IllegalStateException(location + "." + name + " is required");
        }
        if (!value.isValueNode()) throw new IllegalStateException(location + "." + name + " must be scalar");
        String text = value.asText();
        if (text.isBlank() && defaultValue == null) throw new IllegalStateException(location + "." + name + " must not be blank");
        return text;
    }

    private static String optionalText(JsonNode node, String name, String location) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return null;
        if (!value.isValueNode()) throw new IllegalStateException(location + "." + name + " must be scalar JSON text");
        return value.asText();
    }

    private static int integer(JsonNode node, String name, int defaultValue, String location) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) {
            if (defaultValue >= 0) return defaultValue;
            throw new IllegalStateException(location + "." + name + " is required");
        }
        if (!value.canConvertToInt()) throw new IllegalStateException(location + "." + name + " must be an integer");
        return value.intValue();
    }

    private static Duration duration(JsonNode node, String name, Duration defaultValue, String location) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return defaultValue;
        try {
            return Duration.parse(value.asText());
        } catch (RuntimeException error) {
            throw new IllegalStateException(location + "." + name + " must be ISO-8601 duration", error);
        }
    }

    private static Path explicitPath() {
        String property = System.getProperty(CONFIG_PROPERTY);
        String environment = System.getenv(CONFIG_ENVIRONMENT_VARIABLE);
        String value = property == null || property.isBlank() ? environment : property;
        return value == null || value.isBlank() ? null : Path.of(value);
    }
}

package atf.reporting.api.tests.support;

import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockConfigurationLoader;
import atf.mock.AtfMockTransport;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/** API-only access to the reporting module's deterministic mock catalog. */
public final class ApiTestResources {
    public static final String MOCK_RESOURCE = "atf-mock.yaml";

    private ApiTestResources() { }

    public static Path resourcePath(String resourceName) {
        URL resource = ApiTestResources.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalStateException("API test resource is missing: " + resourceName);
        }
        try {
            Path path = Path.of(resource.toURI()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("API test resource is not a file: " + path);
            }
            return path;
        } catch (URISyntaxException | RuntimeException error) {
            throw new IllegalStateException("Cannot resolve API test resource: " + resourceName, error);
        }
    }

    public static AtfMockConfiguration mockConfiguration() {
        return AtfMockConfigurationLoader.load(resourcePath(MOCK_RESOURCE));
    }

    public static AtfMockTransport mockTransport() {
        return new AtfMockTransport(mockConfiguration());
    }
}

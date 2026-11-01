package atf.mock.json;

import atf.core.http.request.ApiRequest;
import atf.mock.AtfMockConfigurationLoader;
import atf.mock.AtfMockTransport;
import atf.testkit.AtfComponentTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class MockJsonCatalogComponentTest {
    @TempDir Path temporary;

    @ParameterizedTest @EmptySource
    @ValueSource(strings = {" ", "{} trailing", "{} {}", "null true", "{\"x\":1,\"x\":2}"})
    void quotedInvalidExpectedAndResponseAreRejectedDuringLoading(String invalid) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> load("'{}'", quote(invalid)));
        assertThrows(IllegalArgumentException.class, () -> load(quote(invalid), "'{}'"));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "[]", "{id: 1}", "[1]"})
    void yamlContainersCannotEraseTheExpectedConstraint(String node) {
        var error = assertThrows(IllegalStateException.class, () -> load("'{}'", node));
        assertTrue(error.getMessage().contains("mock.routes.request-body"));
    }

    @Test void absentExpectedAndQuotedJsonKeepTheirDistinctMeanings() throws Exception {
        var optional = load("'{}'", "null");
        assertNull(optional.routes().getFirst().expectedRequestBody());
        var explicit = load("'null'", "'null'");
        assertEquals("null", explicit.routes().getFirst().expectedRequestBody());
        assertEquals("null", explicit.routes().getFirst().body());
        var scalar = load("'true'", "true");
        assertEquals("true", scalar.routes().getFirst().expectedRequestBody());
    }

    @Test void invalidCatalogNeverFallsBackToSuccessfulDefaults() throws Exception {
        Path catalog = write("'{} trailing'", "'{}'");
        assertThrows(IllegalArgumentException.class, () -> AtfMockTransport.fromConfiguration(catalog));
        var valid = new AtfMockTransport(load("'{\"ok\":true}'", "null"));
        var response = valid.send(new ApiRequest(null, "GET", "/missing", Map.of(), Map.of(), "none", null, ""),
                "/missing", "none").join();
        assertEquals(500, response.status());
        assertEquals(Duration.ZERO, response.duration());
    }

    private atf.mock.AtfMockConfiguration load(String body, String expected) throws Exception {
        return AtfMockConfigurationLoader.load(write(body, expected));
    }
    private Path write(String body, String expected) throws Exception {
        Path path = temporary.resolve("catalog-" + java.util.UUID.randomUUID() + ".yaml");
        Files.writeString(path, "mock:\n  routes:\n    - method: POST\n      path: /fixture\n      status: 201\n"
                + "      duration: PT0S\n      body: " + body + "\n      request-body: " + expected
                + "\n  fallback:\n    status: 500\n    duration: PT0S\n    body: '{\"fallback\":true}'\n");
        return path;
    }
    private static String quote(String value) { return "'" + value.replace("'", "''") + "'"; }
}

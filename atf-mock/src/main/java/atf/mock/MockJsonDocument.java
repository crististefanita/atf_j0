package atf.mock;

import atf.core.json.JsonDocuments;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;

/** Complete, unambiguous JSON for mock fixtures and matching; parsed trees never escape this type. */
final class MockJsonDocument {
    private final JsonNode value;

    private MockJsonDocument(JsonNode value) { this.value = value; }

    static Optional<MockJsonDocument> read(String body) {
        try { return Optional.of(parse(body)); }
        catch (IOException | IllegalArgumentException invalid) { return Optional.empty(); }
    }

    static void require(String body, String field) {
        try { parse(body); }
        catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException(field + " must be one complete JSON document", invalid);
        }
    }

    boolean sameValue(MockJsonDocument other) { return value.equals(other.value); }

    private static MockJsonDocument parse(String body) throws IOException {
        JsonNode value = JsonDocuments.read(body);
        return new MockJsonDocument(value);
    }
}

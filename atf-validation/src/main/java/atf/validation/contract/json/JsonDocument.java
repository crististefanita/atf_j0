package atf.validation.contract.json;

import atf.core.json.JsonDocuments;
import com.fasterxml.jackson.databind.JsonNode;

/** One complete JSON document, including explicit null, or a parsing failure. */
public record JsonDocument(JsonNode root, String failure) {
    public JsonDocument {
        if ((root == null) == (failure == null) || root != null && root.isMissingNode())
            throw new IllegalArgumentException("A JSON document contains either a value or a failure");
    }

    public static JsonDocument parse(String text) {
        try {
            if (text == null || text.isBlank()) return new JsonDocument(null, "empty JSON document");
            JsonNode root = JsonDocuments.read(text);
            return root == null || root.isMissingNode()
                    ? new JsonDocument(null, "empty JSON document") : new JsonDocument(root, null);
        } catch (java.io.IOException | IllegalArgumentException failure) {
            return new JsonDocument(null, "invalid JSON document (" + failure.getClass().getSimpleName() + ")");
        }
    }

    public boolean valid() { return failure == null; }
}

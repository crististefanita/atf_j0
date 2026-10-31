package atf.validation.contract.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** One complete JSON document, including explicit null, or a parsing failure. */
public record JsonDocument(JsonNode root, String failure) {
    public JsonDocument {
        if ((root == null) == (failure == null) || root != null && root.isMissingNode())
            throw new IllegalArgumentException("A JSON document contains either a value or a failure");
    }
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

    public static JsonDocument parse(String text) {
        try {
            JsonNode root = MAPPER.readTree(text == null ? "" : text);
            return root == null || root.isMissingNode()
                    ? new JsonDocument(null, "empty JSON document") : new JsonDocument(root, null);
        } catch (java.io.IOException failure) {
            return new JsonDocument(null, "invalid JSON document (" + failure.getClass().getSimpleName() + ")");
        }
    }

    public boolean valid() { return failure == null; }
}

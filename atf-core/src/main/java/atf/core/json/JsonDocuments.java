package atf.core.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;

/** Reads one complete, unambiguous JSON value without narrowing decimal numbers. */
public final class JsonDocuments {
    private static final ObjectReader READER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build().reader();
    private JsonDocuments() { }

    /** Every call owns its tree; literal null is valid syntax, separately from a caller's write policy. */
    public static JsonNode read(String body) throws IOException {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("JSON document is empty");
        JsonNode value = READER.readTree(body);
        if (value == null || value.isMissingNode()) throw new IllegalArgumentException("JSON document is empty");
        return value;
    }
}

package atf.validation.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

    /** Shared support for the safe JSON path subset used by ATF contracts. */
final class JsonPathSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonPathSupport() { }

    static JsonNode read(String json, String path) {
        try {
            var root = MAPPER.readTree(json == null ? "" : json);
            String pointer = path.startsWith("$") ? path.substring(1) : path;
            // Keep the contract vocabulary aligned with the .NET tests: both
            // object properties ($.id) and array indexes ($[0].id) are valid.
            pointer = pointer.replaceAll("\\[(\\d+)]", "/$1").replace('.', '/');
            if (pointer.isBlank()) return root;
            if (!pointer.startsWith("/")) pointer = "/" + pointer;
            var value = root.at(pointer.replace("//", "/"));
            return value.isMissingNode() ? null : value;
        } catch (Exception error) {
            return null;
        }
    }

    static boolean validJson(String json) {
        try {
            return MAPPER.readTree(json == null ? "" : json) != null;
        } catch (Exception error) {
            return false;
        }
    }
}

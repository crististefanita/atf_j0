package atf.validation.contract;

import atf.validation.contract.json.JsonDocument;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Pattern;

/** Explicit presence states for the documented property/index and JSON Pointer subset. */
final class JsonPathSupport {
    enum State { INVALID_DOCUMENT, INVALID_PATH, MISSING, PRESENT }
    record Value(State state, JsonNode node, String diagnostic) {
        boolean present() { return state == State.PRESENT; }
        String display() { return present() ? node.toString() : diagnostic; }
        String text() { return present() ? node.isValueNode() ? node.asText() : node.toString() : diagnostic; }
        boolean matchesScalar(String expected) { return present() && scalarEquals(node, expected); }
        String type() {
            if (!present()) return diagnostic;
            return node.isNull() ? "null" : node.isObject() ? "object" : node.isArray() ? "array"
                    : node.isTextual() ? "string" : node.isIntegralNumber() ? "integer"
                    : node.isNumber() ? "number" : node.isBoolean() ? "boolean" : "unknown";
        }
    }
    private static final Pattern TOKEN = Pattern.compile("\\.([\\p{L}_][\\p{L}\\p{N}_-]*)|\\[(0|[1-9][0-9]*)]");
    private JsonPathSupport() { }

    /** String DSL compatibility applies to scalar values only, never to container coercions. */
    static boolean scalarEquals(JsonNode node, String expected) {
        return node != null && node.isValueNode() && expected.equals(node.asText());
    }

    static Value read(JsonDocument document, String path) {
        final JsonPointer pointer;
        try { pointer = pointer(path); }
        catch (IllegalArgumentException failure) { return new Value(State.INVALID_PATH, null, "<unsupported JSON path>"); }
        if (!document.valid()) return new Value(State.INVALID_DOCUMENT, null, document.failure());
        JsonNode node = document.root().at(pointer);
        return node.isMissingNode() ? new Value(State.MISSING, null, "<missing>") : new Value(State.PRESENT, node, null);
    }

    private static JsonPointer pointer(String path) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Empty JSON path");
        if (path.startsWith("/")) {
            if (Pattern.compile("~(?![01])").matcher(path).find()) throw new IllegalArgumentException("Invalid escape");
            return JsonPointer.compile(path);
        }
        String suffix = path.startsWith("$") ? path.substring(1) : "." + path;
        var matcher = TOKEN.matcher(suffix);
        var pointer = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) throw new IllegalArgumentException("Unsupported JSON path");
            pointer.append('/').append(matcher.group(1) == null ? matcher.group(2) : matcher.group(1));
            end = matcher.end();
        }
        if (end != suffix.length()) throw new IllegalArgumentException("Unsupported JSON path");
        return JsonPointer.compile(pointer.toString());
    }
}

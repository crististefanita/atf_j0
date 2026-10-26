package atf.core.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

/** Owns structured JSON traversal and falls back to the conservative text policy. */
final class StructuredJsonRedactor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedactionContext context;
    private final Redactor fallback;

    StructuredJsonRedactor(RedactionContext context, Redactor fallback) {
        this.context = context;
        this.fallback = fallback;
    }

    String redact(String json) {
        if (json == null || json.isBlank() || !context.enabled()) {
            return json;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            redactNode(node);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception error) {
            return fallback.redact(json);
        }
    }

    private void redactNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            var names = new ArrayList<String>();
            object.fieldNames().forEachRemaining(names::add);
            names.forEach(name -> {
                JsonNode value = object.get(name);
                if (context.settings().sensitiveFields().contains(name.toLowerCase(java.util.Locale.ROOT))) {
                    object.put(name, context.replacement());
                } else {
                    redactNode(value);
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::redactNode);
        }
    }
}

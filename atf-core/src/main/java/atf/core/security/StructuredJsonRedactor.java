package atf.core.security;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

/** Projects tokens without merging duplicates or narrowing public numbers. */
final class StructuredJsonRedactor {
    private static final JsonFactory JSON = new JsonFactory();

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
        try (var input = JSON.createParser(json)) {
            var text = new StringWriter();
            try (var output = JSON.createGenerator(text).useDefaultPrettyPrinter()) {
                if (input.nextToken() == null) return fallback.redact(json);
                copy(input, output);
                if (input.nextToken() != null) return fallback.redact(json);
            }
            return text.toString();
        } catch (IOException | IllegalArgumentException malformed) {
            return fallback.redact(json);
        }
    }

    private void copy(JsonParser input, JsonGenerator output) throws IOException {
        switch (input.currentToken()) {
            case START_OBJECT -> {
                output.writeStartObject();
                while (input.nextToken() != JsonToken.END_OBJECT) {
                    if (input.currentToken() != JsonToken.FIELD_NAME) throw new IOException("Expected JSON field");
                    String name = input.currentName();
                    output.writeFieldName(name);
                    if (input.nextToken() == null) throw new IOException("Missing JSON value");
                    if (context.settings().sensitiveFields().contains(name.toLowerCase(Locale.ROOT))) {
                        input.skipChildren();
                        output.writeString(context.replacement());
                    } else copy(input, output);
                }
                output.writeEndObject();
            }
            case START_ARRAY -> {
                output.writeStartArray();
                while (input.nextToken() != JsonToken.END_ARRAY) {
                    if (input.currentToken() == null) throw new IOException("Incomplete JSON array");
                    copy(input, output);
                }
                output.writeEndArray();
            }
            case VALUE_STRING -> output.writeString(fallback.redact(input.getText()));
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> output.writeNumber(input.getText());
            default -> output.copyCurrentEvent(input);
        }
    }
}

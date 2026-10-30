package atf.reporting.security;

import com.fasterxml.jackson.core.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** Streaming projection preserves duplicate fields and masks the complete sensitive value. */
final class JsonEvidenceRedactor {
    private static final JsonFactory JSON = new JsonFactory();
    private JsonEvidenceRedactor() { }

    static Optional<String> redact(String input, Predicate<String> sensitive,
                                   UnaryOperator<String> text, String replacement) {
        if (input == null || input.isBlank()) return Optional.empty();
        try (var parser = JSON.createParser(input)) {
            var output = new StringWriter();
            try (var generator = JSON.createGenerator(output)) {
                if (parser.nextToken() == null) return Optional.empty();
                copy(parser, generator, sensitive, text, replacement);
                if (parser.nextToken() != null) return Optional.empty();
            }
            return Optional.of(output.toString());
        } catch (IOException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private static void copy(JsonParser input, JsonGenerator output, Predicate<String> sensitive,
                              UnaryOperator<String> text, String replacement) throws IOException {
        switch (input.currentToken()) {
            case START_OBJECT -> {
                output.writeStartObject();
                while (input.nextToken() != JsonToken.END_OBJECT) {
                    if (input.currentToken() != JsonToken.FIELD_NAME) throw new IOException("Expected JSON field");
                    String name = input.currentName();
                    output.writeFieldName(name);
                    if (input.nextToken() == null) throw new IOException("Missing JSON value");
                    if (sensitive.test(name)) { input.skipChildren(); output.writeString(replacement); }
                    else copy(input, output, sensitive, text, replacement);
                }
                output.writeEndObject();
            }
            case START_ARRAY -> {
                output.writeStartArray();
                while (input.nextToken() != JsonToken.END_ARRAY) {
                    if (input.currentToken() == null) throw new IOException("Incomplete JSON array");
                    copy(input, output, sensitive, text, replacement);
                }
                output.writeEndArray();
            }
            case VALUE_STRING -> output.writeString(text.apply(input.getText()));
            default -> output.copyCurrentEvent(input);
        }
    }
}

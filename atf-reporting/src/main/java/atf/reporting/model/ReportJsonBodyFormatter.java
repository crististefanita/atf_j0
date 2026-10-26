package atf.reporting.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Renderer-neutral JSON display policy.
 *
 * <p>The authored body remains unchanged in {@link ReportJsonBody}. This
 * formatter only creates a deterministic view for human-readable evidence.
 * Invalid payloads are returned as authored so diagnostics never disappear at
 * the presentation boundary.</p>
 */
public final class ReportJsonBodyFormatter {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectReader STRICT_READER = JSON.reader()
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final ObjectWriter PRETTY_WRITER = JSON.writer(prettyPrinter());

    private ReportJsonBodyFormatter() {
    }

    public static boolean isJsonDocument(String body) {
        if (body == null || body.isBlank()) return false;
        try {
            return STRICT_READER.readTree(body) != null;
        } catch (JsonProcessingException | RuntimeException invalidDocument) {
            return false;
        }
    }

    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        return printer;
    }

    /**
     * Pretty-prints one complete JSON document, preserving scalars and JSON
     * {@code null}. Blank or invalid input is returned without invention.
     */
    public static String format(String body) {
        if (body == null || body.isBlank()) {
            return body == null ? "" : body;
        }
        try {
            JsonNode document = STRICT_READER.readTree(body);
            return document == null ? body : PRETTY_WRITER.writeValueAsString(document);
        } catch (JsonProcessingException | RuntimeException ignored) {
            return body;
        }
    }
}

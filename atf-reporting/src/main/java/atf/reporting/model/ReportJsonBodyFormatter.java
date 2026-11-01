package atf.reporting.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.StringWriter;
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
        var formatted = new StringWriter();
        try (var input = JSON.getFactory().createParser(body);
             var output = JSON.getFactory().createGenerator(formatted)) {
            output.setPrettyPrinter(prettyPrinter());
            int depth = 0;
            boolean complete = false;
            for (JsonToken token; (token = input.nextToken()) != null;) {
                if (complete) return body;
                if (token.isStructStart()) depth++;
                if (token.isStructEnd()) depth--;
                if (token.isNumeric()) output.writeNumber(input.getText());
                else output.copyCurrentEvent(input);
                complete = depth == 0;
            }
            if (!complete) return body;
        } catch (IOException | RuntimeException ignored) {
            return body;
        }
        return formatted.toString();
    }
}

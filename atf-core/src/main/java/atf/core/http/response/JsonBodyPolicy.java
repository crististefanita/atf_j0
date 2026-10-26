package atf.core.http.response;
import atf.core.http.request.HttpMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single source of truth for JSON request/response rules on HTTP writes.
 *
 * <p>POST and PUT are intentionally strict: a request must carry a non-empty
 * JSON document and the mock/transport response must carry a non-empty JSON
 * document as well. Read methods keep the explicit {@code null} evidence
 * convention because a body-less exchange is valid there.</p>
 */
public final class JsonBodyPolicy {
    public static final String NULL_DOCUMENT = "null";
    private static final ObjectMapper JSON = new ObjectMapper();

    private JsonBodyPolicy() {
    }

    public static boolean appliesTo(String method) {
        if (method == null) return false;
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        return HttpMethod.POST.value().equals(normalized)
                || HttpMethod.PUT.value().equals(normalized);
    }

    public static boolean validDocument(String body) {
        if (body == null || body.isBlank()) return false;
        try {
            return JSON.readTree(body) != null;
        } catch (Exception error) {
            return false;
        }
    }

    public static boolean validWriteDocument(String body) {
        return validDocument(body) && !NULL_DOCUMENT.equals(body.trim());
    }

    public static String display(String body) {
        return body == null || body.isBlank() ? NULL_DOCUMENT : body;
    }

    public static List<String> validateWriteExchange(String requestBody, String responseBody) {
        List<String> violations = new ArrayList<>();
        validateSide("request", requestBody, violations);
        validateSide("response", responseBody, violations);
        return List.copyOf(violations);
    }

    private static void validateSide(String side, String body, List<String> violations) {
        if (body == null || body.isBlank() || NULL_DOCUMENT.equals(body.trim())) {
            violations.add(side + " JSON body is required for POST/PUT and must not be null");
            return;
        }
        if (!validDocument(body)) {
            violations.add(side + " JSON body is not valid JSON");
        }
    }

    public static void requireWriteDocument(String body, String field) {
        if (!validWriteDocument(body)) {
            throw new IllegalArgumentException(field + " must be a non-empty JSON document");
        }
    }
}

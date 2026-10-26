package atf.shop.support;

import atf.api.core.http.response.ApiBodyEvidence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Locale;
import java.util.Map;

/** One parse-aware JSON/redaction boundary for all Practice Shop evidence. */
public final class ShopJsonEvidence {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final String REDACTED = "<redacted>";
    private static final String OMITTED_UNSAFE_BODY = "<body omitted: malformed or non-JSON payload>";
    private static final Set<String> PERSONAL_FIELDS = Set.of(
            "firstname", "middlename", "lastname", "givenname", "surname", "fullname",
            "email", "emailaddress", "username", "dob", "dateofbirth", "birthdate", "country",
            "postalcode", "zipcode", "housenumber", "street", "address", "addressline1",
            "addressline2", "billingaddress", "shippingaddress", "city", "state", "province",
            "phone", "phonenumber", "mobile", "telephone");

    private ShopJsonEvidence() { }

    public static JsonNode parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return JSON.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(value);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    public static boolean isJson(String value) {
        JsonNode node = parse(value);
        return node != null && !node.isMissingNode();
    }

    public static String redact(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;
        JsonNode root = parse(value);
        if (root == null) {
            // Preserve transport limitation markers, but fail closed for arbitrary text:
            // malformed JSON cannot be redacted reliably with a field-aware policy.
            return ApiBodyEvidence.isUnavailable(value) ? value : OMITTED_UNSAFE_BODY;
        }
        redactNode(root);
        if (root.isTextual()) root = JSON.getNodeFactory().textNode(REDACTED);
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Cannot serialize redacted JSON evidence", error);
        }
    }

    /** Compatibility alias; all published API bodies now use the same strict field-aware policy. */
    public static String redactRegistration(String value) {
        return redact(value);
    }

    public static Map<String, String> redactHeaders(Map<String, String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        (headers == null ? Map.<String, String>of() : headers).forEach((name, value) -> {
            boolean sensitive = isSensitive(name) || isPersonalField(name);
            result.put(name == null ? "" : name, sensitive ? REDACTED : value);
        });
        return Map.copyOf(result);
    }

    private static void redactNode(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey()) || isPersonalField(field.getKey())) {
                    field.setValue(JSON.getNodeFactory().textNode(REDACTED));
                } else if (field.getValue().isTextual()) {
                    field.setValue(JSON.getNodeFactory().textNode(
                            redactEmbeddedPii(field.getValue().asText())));
                } else {
                    redactNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                JsonNode child = node.get(index);
                if (child.isTextual()) {
                    ((ArrayNode) node).set(index, JSON.getNodeFactory().textNode(
                            redactEmbeddedPii(child.asText())));
                }
                else redactNode(child);
            }
        }
    }

    private static String redactEmbeddedPii(String value) {
        return value.replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", REDACTED);
    }

    private static boolean isPersonalField(String name) {
        String normalized = normalizeFieldName(name);
        return PERSONAL_FIELDS.contains(normalized)
                || normalized.contains("firstname") || normalized.contains("middlename")
                || normalized.contains("lastname") || normalized.contains("givenname")
                || normalized.contains("surname")
                || normalized.contains("address") || normalized.contains("postalcode")
                || normalized.contains("zipcode") || normalized.contains("phonenumber")
                || normalized.contains("email");
    }

    private static boolean isSensitive(String name) {
        String normalized = normalizeFieldName(name);
        return normalized.contains("password") || normalized.contains("token")
                || normalized.contains("authorization") || normalized.contains("cookie")
                || normalized.contains("apikey") || normalized.contains("secret");
    }

    private static String normalizeFieldName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

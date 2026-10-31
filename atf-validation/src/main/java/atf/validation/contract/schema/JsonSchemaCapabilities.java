package atf.validation.contract.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Fail-closed schema preflight, independent of the instance and its visited branches. */
public final class JsonSchemaCapabilities {
    private static final Set<String> TYPES = Set.of("object", "array", "string", "integer", "number", "boolean", "null");
    private static final Set<String> KEYWORDS = Set.of("$schema", "$comment", "title", "description", "default", "examples",
            "type", "enum", "required", "properties", "additionalProperties", "items", "minItems", "maxItems",
            "minLength", "maxLength", "pattern", "minimum", "maximum");
    private JsonSchemaCapabilities() { }

    public static List<String> inspect(JsonNode schema) {
        var errors = new ArrayList<String>();
        inspect(schema, "$schema", errors);
        return List.copyOf(errors);
    }
    private static void inspect(JsonNode schema, String path, List<String> errors) {
        if (schema.isBoolean()) return;
        if (!schema.isObject()) { errors.add(path + ": schema must be an object or boolean"); return; }
        schema.fields().forEachRemaining(field -> {
            String key = field.getKey();
            JsonNode value = field.getValue();
            String location = path + "." + key;
            if (!KEYWORDS.contains(key)) { errors.add(location + ": unsupported keyword"); return; }
            switch (key) {
                case "$schema" -> require(value.isTextual()
                        && value.textValue().equals("https://json-schema.org/draft/2020-12/schema"), location, errors);
                case "$comment", "title", "description" -> require(value.isTextual(), location, errors);
                case "examples" -> require(value.isArray(), location, errors);
                case "type" -> require(validTypes(value), location, errors);
                case "required" -> require(uniqueStrings(value), location, errors);
                case "enum" -> require(validEnum(value), location, errors);
                case "properties" -> {
                    require(value.isObject(), location, errors);
                    if (value.isObject()) value.fields().forEachRemaining(property ->
                            inspect(property.getValue(), location + "." + property.getKey(), errors));
                }
                case "items", "additionalProperties" -> inspect(value, location, errors);
                case "minItems", "maxItems", "minLength", "maxLength" ->
                        require(value.isNumber() && value.decimalValue().signum() >= 0
                                && value.decimalValue().stripTrailingZeros().scale() <= 0, location, errors);
                case "minimum", "maximum" -> require(value.isNumber(), location, errors);
                case "pattern" -> {
                    require(value.isTextual(), location, errors);
                    if (value.isTextual()) {
                        try { Pattern.compile(value.textValue()); }
                        catch (PatternSyntaxException failure) { errors.add(location + ": invalid Java regex"); }
                    }
                }
                default -> { /* default is annotation data, not a nested schema. */ }
            }
        });
    }
    private static boolean validTypes(JsonNode node) {
        if (node.isTextual()) return TYPES.contains(node.textValue());
        if (!uniqueStrings(node) || node.isEmpty()) return false;
        for (JsonNode type : node) if (!TYPES.contains(type.textValue())) return false;
        return true;
    }
    private static boolean uniqueStrings(JsonNode node) {
        if (!node.isArray()) return false;
        var values = new HashSet<String>();
        for (JsonNode value : node) if (!value.isTextual() || !values.add(value.textValue())) return false;
        return true;
    }
    private static boolean validEnum(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) return false;
        for (int i = 0; i < node.size(); i++)
            for (int j = 0; j < i; j++) if (JsonValueEquality.same(node.get(i), node.get(j))) return false;
        return true;
    }
    private static void require(boolean valid, String path, List<String> errors) {
        if (!valid) errors.add(path + ": invalid keyword value");
    }
}

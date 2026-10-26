package atf.validation.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Small evaluator for the JSON Schema keywords used by ATF response contracts. */
final class JsonSchemaValidator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaValidator() { }

    static Result validate(Path schemaPath, String body) throws IOException {
        JsonNode schema = MAPPER.readTree(Files.readString(schemaPath));
        JsonNode instance = MAPPER.readTree(body);
        List<String> violations = new ArrayList<>();
        validateNode(instance, schema, "$", violations);
        return new Result(violations);
    }

    private static void validateNode(JsonNode instance, JsonNode schema, String path, List<String> violations) {
        if (schema == null || schema.isMissingNode() || schema.isBoolean() && schema.booleanValue()) return;
        if (schema.isBoolean() && !schema.booleanValue()) {
            violations.add(path + ": schema rejected the value");
            return;
        }
        JsonNode type = schema.get("type");
        if (type != null && !matchesType(instance, type)) {
            violations.add(path + ": expected " + type + " but was " + actualType(instance));
            return;
        }
        JsonNode enumValues = schema.get("enum");
        if (enumValues != null && enumValues.isArray() && !contains(enumValues, instance)) {
            violations.add(path + ": value is not in enum");
        }
        if (instance != null && instance.isObject()) validateObject(instance, schema, path, violations);
        if (instance != null && instance.isArray()) validateArray(instance, schema, path, violations);
        if (instance != null && instance.isTextual()) validateString(instance, schema, path, violations);
        if (instance != null && instance.isNumber()) validateNumber(instance, schema, path, violations);
    }

    private static void validateObject(JsonNode instance, JsonNode schema, String path, List<String> violations) {
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            required.forEach(field -> {
                if (field.isTextual() && !instance.has(field.asText())) {
                    violations.add(path + "." + field.asText() + ": required property is missing");
                }
            });
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (instance.has(field.getKey())) {
                    validateNode(instance.get(field.getKey()), field.getValue(), path + "." + field.getKey(), violations);
                }
            }
        }
        JsonNode additional = schema.get("additionalProperties");
        if (additional != null && additional.isBoolean() && !additional.booleanValue() && properties != null) {
            instance.fieldNames().forEachRemaining(name -> {
                if (!properties.has(name)) violations.add(path + "." + name + ": additional property is not allowed");
            });
        }
    }

    private static void validateArray(JsonNode instance, JsonNode schema, String path, List<String> violations) {
        checkLimit(instance.size(), schema.get("minItems"), path + ": fewer items than minItems", violations, true);
        checkLimit(instance.size(), schema.get("maxItems"), path + ": more items than maxItems", violations, false);
        JsonNode items = schema.get("items");
        if (items != null && items.isObject()) {
            for (int index = 0; index < instance.size(); index++) {
                validateNode(instance.get(index), items, path + "[" + index + "]", violations);
            }
        }
    }

    private static void validateString(JsonNode instance, JsonNode schema, String path, List<String> violations) {
        checkLimit(instance.textValue().length(), schema.get("minLength"), path + ": shorter than minLength", violations, true);
        checkLimit(instance.textValue().length(), schema.get("maxLength"), path + ": longer than maxLength", violations, false);
        JsonNode pattern = schema.get("pattern");
        if (pattern != null && pattern.isTextual() && !Pattern.compile(pattern.textValue()).matcher(instance.textValue()).find()) {
            violations.add(path + ": does not match pattern");
        }
    }

    private static void validateNumber(JsonNode instance, JsonNode schema, String path, List<String> violations) {
        if (schema.has("minimum") && instance.decimalValue().compareTo(schema.get("minimum").decimalValue()) < 0) {
            violations.add(path + ": less than minimum");
        }
        if (schema.has("maximum") && instance.decimalValue().compareTo(schema.get("maximum").decimalValue()) > 0) {
            violations.add(path + ": greater than maximum");
        }
    }

    private static void checkLimit(int actual, JsonNode limit, String message,
                                   List<String> violations, boolean minimum) {
        if (limit != null && limit.isIntegralNumber() && (minimum ? actual < limit.intValue() : actual > limit.intValue())) {
            violations.add(message);
        }
    }

    private static boolean matchesType(JsonNode value, JsonNode type) {
        if (type.isArray()) {
            for (JsonNode alternative : type) if (matchesType(value, alternative)) return true;
            return false;
        }
        if (!type.isTextual()) return true;
        return switch (type.textValue()) {
            case "object" -> value != null && value.isObject();
            case "array" -> value != null && value.isArray();
            case "string" -> value != null && value.isTextual();
            case "integer" -> value != null && value.isIntegralNumber();
            case "number" -> value != null && value.isNumber();
            case "boolean" -> value != null && value.isBoolean();
            case "null" -> value == null || value.isNull();
            default -> true;
        };
    }

    private static String actualType(JsonNode value) {
        if (value == null || value.isNull()) return "null";
        if (value.isObject()) return "object";
        if (value.isArray()) return "array";
        if (value.isTextual()) return "string";
        if (value.isIntegralNumber()) return "integer";
        if (value.isNumber()) return "number";
        if (value.isBoolean()) return "boolean";
        return "unknown";
    }

    private static boolean contains(JsonNode values, JsonNode value) {
        for (JsonNode candidate : values) if (candidate.equals(value)) return true;
        return false;
    }

    record Result(List<String> violations) {
        Result { violations = List.copyOf(violations); }
        boolean valid() { return violations.isEmpty(); }
    }
}

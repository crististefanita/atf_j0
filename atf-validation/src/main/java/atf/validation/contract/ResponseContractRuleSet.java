package atf.validation.contract;

import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.core.http.response.ApiResponse;
import atf.validation.evaluation.ValidationTable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.StreamSupport;

/** Rule mechanics; each evaluation shares its own lazy JSON document. */
final class ResponseContractRuleSet {
    private final SchemaResourceResolver schemaResolver;
    ResponseContractRuleSet(SchemaResourceResolver schemaResolver) {
        this.schemaResolver = java.util.Objects.requireNonNull(schemaResolver, "schemaResolver");
    }
    ResponseContractRuleDefinition status(int expected) {
        return rule("status:" + expected, "HTTP status", (response, table) ->
                table.add("HTTP status", Integer.toString(expected), Integer.toString(response.status()), response.status() == expected));
    }
    ResponseContractRuleDefinition statusOneOf(int... expected) {
        if (expected == null || expected.length == 0) throw new IllegalArgumentException("expected statuses must not be empty");
        List<Integer> allowed = Arrays.stream(expected).boxed().toList();
        return rule("status-one-of:" + allowed, "HTTP status", (response, table) ->
                table.add("HTTP status", allowed.toString(), Integer.toString(response.status()), allowed.contains(response.status())));
    }
    ResponseContractRuleDefinition contentTypeContains(String expected) {
        return rule("content-type:" + expected, "Content-Type", (response, table) -> {
            var actual = response.header(HttpHeader.CONTENT_TYPE.value());
            table.add(HttpHeader.CONTENT_TYPE.value(), expected, actual == null ? "<missing>" : actual,
                    actual != null && actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)));
        });
    }
    ResponseContractRuleDefinition contentTypeJson() {
        return rule("content-type-json", "Content-Type JSON", (response, table) -> {
            var actual = response.header(HttpHeader.CONTENT_TYPE.value());
            table.add("Content-Type JSON", "JSON media type", actual == null ? "<missing>" : actual,
                    MediaTypes.isJson(actual));
        });
    }
    ResponseContractRuleDefinition headerPresent(String name) {
        return rule("header:" + name.toLowerCase(Locale.ROOT), "Header " + name, (response, table) -> {
            var actual = response.header(name);
            table.add("Header " + name, "present", actual == null ? "<missing>" : actual, actual != null);
        });
    }
    ResponseContractRuleDefinition bodyContains(String expected) {
        return rule("body:" + expected, "Response body", (response, table) ->
                table.add("Response body", expected, response.body(), response.body().contains(expected)));
    }
    ResponseContractRuleDefinition jsonPathExists(String path) {
        return jsonPresence("json-exists:", "JSON path exists: ", path, false);
    }
    ResponseContractRuleDefinition jsonPathRequired(String path) {
        return jsonPresence("json-required:", "JSON required: ", path, false);
    }
    ResponseContractRuleDefinition jsonPathAbsent(String path) {
        return jsonPresence("json-absent:", "JSON absent: ", path, true);
    }
    private ResponseContractRuleDefinition jsonPresence(String identity, String title, String path, boolean absent) {
        return contextual(identity + path, title + path, context -> {
            var value = context.at(path);
            boolean passed = absent ? value.state() == JsonPathSupport.State.MISSING : value.present();
            context.table().add(title + path, absent ? "missing" : "present", value.display(), passed);
        });
    }
    ResponseContractRuleDefinition jsonPathEquals(String path, String expected) {
        return contextual("json-equals:" + path + ":" + expected, "JSON path equals: " + path, context -> {
            var value = context.at(path);
            context.table().add("JSON path equals: " + path, expected, value.text(), value.matchesScalar(expected));
        });
    }
    ResponseContractRuleDefinition jsonPathType(String path, String expectedType) {
        String type = expectedType.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("object", "array", "string", "integer", "number", "boolean", "null").contains(type))
            throw new IllegalArgumentException("Unsupported JSON type: " + expectedType);
        return contextual("json-type:" + path + ":" + type, "JSON type: " + path, context -> {
            var value = context.at(path);
            context.table().add("JSON type: " + path, type, value.type(), value.present() && type.equals(value.type()));
        });
    }
    ResponseContractRuleDefinition jsonArraySize(String path, int expectedSize) {
        return contextual("json-array-size:" + path + ":" + expectedSize, "JSON array size: " + path, context -> {
            var value = context.at(path);
            boolean array = value.present() && value.node().isArray();
            context.table().add("JSON array size: " + path, Integer.toString(expectedSize),
                    array ? Integer.toString(value.node().size()) : value.display(), array && value.node().size() == expectedSize);
        });
    }
    ResponseContractRuleDefinition everyArrayItemEquals(String path, String property, String expected) {
        return contextual("every-array:" + path + ":" + property + ":" + expected, "Every " + path + " item " + property, context -> {
            var value = context.at(path);
            boolean passed = value.present() && value.node().isArray()
                    && StreamSupport.stream(value.node().spliterator(), false)
                    .allMatch(item -> item.isObject() && JsonPathSupport.scalarEquals(item.get(property), expected));
            context.table().add("Every " + path + " item " + property, expected, value.display(), passed);
        });
    }
    ResponseContractRuleDefinition jsonSchema(String schemaPath) {
        return contextual("schema:" + schemaPath, "JSON schema", context -> {
            var table = context.table();
            Path path = schemaResolver.resolve(schemaPath);
            if (path == null || !Files.isRegularFile(path)) {
                table.add("JSON schema", schemaPath, "schema file missing", false);
                return;
            }
            try {
                var result = JsonSchemaValidator.validate(path, context.document());
                table.add("JSON schema", schemaPath, result.valid() ? "schema matched" : String.join(" | ", result.violations()), result.valid());
            } catch (java.io.IOException error) {
                table.addFailure("JSON schema", schemaPath, error.getMessage(), error);
            }
        });
    }
    private static ResponseContractRuleDefinition rule(String identity, String title, ResponseValidationRule rule) {
        return contextual(identity, title, context -> rule.evaluate(context.response(), context.table()));
    }
    private static ResponseContractRuleDefinition contextual(String identity, String title, ContextualRule rule) {
        return new ResponseContractRuleDefinition(identity, title, rule);
    }
}

@FunctionalInterface
interface ContextualRule extends ResponseValidationRule {
    void evaluate(ContractEvaluation context);
    @Override default void evaluate(ApiResponse response, ValidationTable table) { evaluate(new ContractEvaluation(response, table)); }
}

record ResponseContractRuleDefinition(String identity, String title, ContextualRule rule) {
    ResponseContractRuleDefinition {
        java.util.Objects.requireNonNull(identity, "identity");
        java.util.Objects.requireNonNull(title, "title");
        java.util.Objects.requireNonNull(rule, "rule");
    }
}

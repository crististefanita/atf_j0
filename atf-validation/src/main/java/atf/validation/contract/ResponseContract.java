package atf.validation.contract;
import atf.core.http.response.ApiResponse;
import atf.validation.evaluation.ValidationTable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

    /** Fluent contract for exactly one response; it cannot be evaluated twice. */
public final class ResponseContract {
    private final List<NamedRule> rules = new ArrayList<>();
    private final Set<String> identities = new HashSet<>();
    private final ResponseContractRuleSet ruleSet;
    private boolean evaluated;

    public ResponseContract() {
        this(SchemaResourceResolver.fromWorkingDirectory());
    }

    public ResponseContract(SchemaResourceResolver schemaResolver) {
        this.ruleSet = new ResponseContractRuleSet(schemaResolver);
    }

    public ResponseContract status(int expected) {
        return add(ruleSet.status(expected));
    }

    public ResponseContract statusOneOf(int... expected) {
        return add(ruleSet.statusOneOf(expected));
    }

    public ResponseContract contentTypeContains(String expected) {
        requireText(expected, "expected");
        return add(ruleSet.contentTypeContains(expected));
    }

    /** Requires a concrete JSON type/subtype as classified by Core; parameters do not determine the type. */
    public ResponseContract contentTypeJson() {
        return add(ruleSet.contentTypeJson());
    }

    public ResponseContract headerPresent(String name) {
        requireText(name, "name");
        return add(ruleSet.headerPresent(name));
    }

    public ResponseContract bodyContains(String expected) {
        requireText(expected, "expected");
        return add(ruleSet.bodyContains(expected));
    }

    public ResponseContract jsonPathExists(String path) {
        requireText(path, "path");
        return add(ruleSet.jsonPathExists(path));
    }

    /** Compares a present JSON scalar's text; objects and arrays cannot match, including empty expected text. */
    public ResponseContract jsonPathEquals(String path, String expected) {
        requireText(path, "path");
        if (expected == null) throw new IllegalArgumentException("expected must not be null");
        return add(ruleSet.jsonPathEquals(path, expected));
    }

    /** Requires a JSON value at {@code path} to have the supplied JSON type. */
    public ResponseContract jsonPathType(String path, String expectedType) {
        requireText(path, "path");
        requireText(expectedType, "expectedType");
        return add(ruleSet.jsonPathType(path, expectedType));
    }

    /** Requires a JSON value to be present, including an explicit JSON null. */
    public ResponseContract jsonPathRequired(String path) {
        requireText(path, "path");
        return add(ruleSet.jsonPathRequired(path));
    }

    /** Requires a JSON value to be absent. */
    public ResponseContract jsonPathAbsent(String path) {
        requireText(path, "path");
        return add(ruleSet.jsonPathAbsent(path));
    }

    /** Requires an array at {@code path} with the exact cardinality. */
    public ResponseContract jsonArraySize(String path, int expectedSize) {
        requireText(path, "path");
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize must not be negative");
        return add(ruleSet.jsonArraySize(path, expectedSize));
    }

    /** Requires each array item to have a scalar property matching the text; an empty array satisfies this rule. */
    public ResponseContract everyArrayItemEquals(String path, String property, String expected) {
        requireText(path, "path");
        requireText(property, "property");
        if (expected == null) throw new IllegalArgumentException("expected must not be null");
        return add(ruleSet.everyArrayItemEquals(path, property, expected));
    }

    /** Semantically validates the JSON body against the contract schema. */
    public ResponseContract jsonSchema(String schemaPath) {
        requireText(schemaPath, "schemaPath");
        return add(ruleSet.jsonSchema(schemaPath));
    }

    /** @deprecated use {@link #jsonSchema(String)}; kept for source compatibility. */
    @Deprecated(forRemoval = false)
    public ResponseContract jsonSchemaFileExists(String schemaPath) {
        return jsonSchema(schemaPath);
    }

    public void evaluate(ApiResponse response, ValidationTable table) {
        if (response == null || table == null) throw new NullPointerException("response/table");
        if (evaluated) throw new IllegalStateException("ResponseContract can be evaluated only once");
        if (rules.isEmpty()) throw new IllegalStateException("ResponseContract must contain at least one rule");
        evaluated = true;
        var context = new ContractEvaluation(response, table);
        for (var rule : rules) {
            try {
                rule.rule().evaluate(context);
            } catch (RuntimeException error) {
                table.addFailure(rule.title(), "rule completed", error.getMessage(), error);
            }
        }
    }

    public List<String> ruleNames() {
        return rules.stream().map(NamedRule::title).toList();
    }

    private ResponseContract add(ResponseContractRuleDefinition definition) {
        if (evaluated) throw new IllegalStateException("ResponseContract cannot be changed after evaluation");
        if (!identities.add(definition.identity())) {
            throw new IllegalArgumentException("duplicate response validation rule: " + definition.identity());
        }
        rules.add(new NamedRule(definition.title(), definition.rule()));
        return this;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }

    private record NamedRule(String title, ContextualRule rule) { }
}

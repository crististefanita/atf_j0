package atf.validation;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfUnitTest;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ValidationEvaluator;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonSchemaContractTest {
    @TempDir Path directory;
    @Test void everyDeclaredTypeHasANominalAndAnIncompatibleInstance() throws Exception {
        for (var entry : Map.of("object", "{}", "array", "[]", "string", "\"ok\"", "integer", "1",
                "number", "1.25", "boolean", "true", "null", "null").entrySet()) {
            String schema = "{\"type\":\"" + entry.getKey() + "\"}";
            assertEquals(AtfReportStatus.PASS, evaluate(schema, entry.getValue(), false).status(), entry.getKey());
            assertEquals(AtfReportStatus.FAIL, evaluate(schema, entry.getKey().equals("null") ? "0" : "null", false).status(), entry.getKey());
        }
    }
    @ParameterizedTest @MethodSource("instances")
    void publicSchemaContractHonorsDeclaredConstraints(String schema, String body, boolean expected) throws Exception {
        var decision = evaluate(schema, body, false);
        assertEquals(expected ? AtfReportStatus.PASS : AtfReportStatus.FAIL, decision.status(), decision.rows().toString());
        assertEquals(expected, decision.rows().getFirst().passed());
    }
    static Stream<Arguments> instances() {
        return Stream.of(
                Arguments.of("{}", "null", true),
                Arguments.of("{\"const\":42}", "99", false),
                Arguments.of("{\"const\":42}", "42", false), Arguments.of("true", "{}", true), Arguments.of("false", "null", false),
                Arguments.of("{\"additionalProperties\":false}", "{\"extra\":1}", false),
                Arguments.of("{\"additionalProperties\":false}", "{}", true),
                Arguments.of("{\"additionalProperties\":{\"type\":\"integer\"}}", "{\"x\":1}", true),
                Arguments.of("{\"additionalProperties\":{\"type\":\"integer\"}}", "{\"x\":\"1\"}", false),
                Arguments.of("{\"properties\":{\"x\":true},\"additionalProperties\":false}", "{\"x\":null}", true),
                Arguments.of("{\"properties\":{\"x\":false}}", "{\"x\":null}", false),
                Arguments.of("{\"required\":[\"x\"]}", "{}", false),
                Arguments.of("{\"required\":[\"x\"]}", "{\"x\":null}", true),
                Arguments.of("{\"type\":[\"null\",\"string\"]}", "null", true),
                Arguments.of("{\"type\":\"integer\"}", "1.0", true),
                Arguments.of("{\"type\":\"integer\"}", "1.1", false),
                Arguments.of("{\"type\":\"object\"}", "[]", false),
                Arguments.of("{\"type\":\"array\",\"items\":false}", "[1]", false),
                Arguments.of("{\"items\":false}", "[]", true),
                Arguments.of("{\"items\":true}", "[1,null]", true),
                Arguments.of("{\"items\":{\"type\":\"string\"}}", "[\"ok\"]", true),
                Arguments.of("{\"items\":{\"type\":\"string\"}}", "[1]", false),
                Arguments.of("{\"minItems\":1,\"maxItems\":2}", "[]", false),
                Arguments.of("{\"minItems\":1,\"maxItems\":2}", "[1,2,3]", false),
                Arguments.of("{\"minItems\":1,\"maxItems\":2}", "[1]", true),
                Arguments.of("{\"minLength\":1,\"maxLength\":1}", "\"😀\"", true),
                Arguments.of("{\"maxLength\":1}", "\"ab\"", false),
                Arguments.of("{\"minLength\":2}", "\"a\"", false),
                Arguments.of("{\"pattern\":\"^[A-Z]+$\"}", "\"ATF\"", true),
                Arguments.of("{\"pattern\":\"^[A-Z]+$\"}", "\"atf\"", false),
                Arguments.of("{\"minimum\":1.01,\"maximum\":2}", "1.01", true),
                Arguments.of("{\"minimum\":1.01}", "1.0099999999999999999", false),
                Arguments.of("{\"maximum\":2}", "2.1", false),
                Arguments.of("{\"maxItems\":18446744073709551616}", "[1]", true),
                Arguments.of("{\"minItems\":18446744073709551616}", "[1]", false),
                Arguments.of("{\"enum\":[{\"x\":[1]}]}", "{\"x\":[1.0]}", true),
                Arguments.of("{\"enum\":[1,2]}", "3", false),
                Arguments.of("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"$comment\":\"note\",\"title\":\"t\",\"description\":\"d\",\"default\":{\"unknown\":true},\"examples\":[1]}", "{}", true));
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "1", "{} trailing", "{} {}", "{\"type\":\"number\",\"type\":\"string\"}",
            "{\"type\":\"not-a-type\"}", "{\"type\":[]}", "{\"type\":[\"string\",\"string\"]}", "{\"type\":1}",
            "{\"required\":[1]}", "{\"required\":[\"x\",\"x\"]}", "{\"properties\":[]}", "{\"items\":[]}",
            "{\"additionalProperties\":1}", "{\"minItems\":-1}", "{\"maxLength\":1.5}", "{\"maximum\":\"2\"}",
            "{\"enum\":[]}", "{\"enum\":[1,1.0]}", "{\"pattern\":\"[\"}", "{\"pattern\":3}",
            "{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}", "{\"format\":\"email\"}", "{\"$ref\":\"remote\"}",
            "{\"properties\":{\"notVisited\":{\"unsupported\":true}}}", "{\"items\":{\"type\":\"bad\"}}"})
    void malformedOrUnsupportedContractsNeverPass(String schema) throws Exception {
        for (boolean warning : new boolean[]{false, true}) {
            var decision = evaluate(schema, "{}", warning);
            assertEquals(warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL, decision.status());
            assertFalse(decision.rows().getFirst().passed());
            assertFalse(decision.rows().getFirst().actual().contains("schema matched"));
        }
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "not-json", "{} trailing", "{} {}", "{\"x\":1,\"x\":2}"})
    void evenUnconstrainedSchemasRequireOneCompleteDocument(String body) throws Exception {
        assertEquals(AtfReportStatus.FAIL, evaluate("{}", body, false).status());
        assertEquals(AtfReportStatus.FAIL, evaluate("true", body, false).status());
    }
    private atf.validation.evaluation.ValidationDecision evaluate(String schema, String body, boolean warning) throws Exception {
        Path path = directory.resolve("contract.json");
        Files.writeString(path, schema);
        return new ValidationEvaluator().evaluate(new ResponseContract().jsonSchema(path.toString()),
                new ApiResponse(200, body, Map.of(), Duration.ZERO, ""), warning);
    }
}

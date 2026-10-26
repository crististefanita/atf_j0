package atf.validation;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfReportStatus;
import atf.validation.contract.ContractValidators;
import atf.validation.contract.ResponseContract;
import atf.validation.contract.SchemaResourceResolver;
import atf.validation.evaluation.ValidationEvaluator;
import atf.validation.evaluation.ValidationTable;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class ResponseContractTest {
    private static ApiResponse response(String body) {
        return new ApiResponse(200, body, Map.of("Content-Type", "application/json"), Duration.ofMillis(3), "https://example.test/users");
    }

    @Test
    void contractAggregatesStatusHeadersBodyAndJsonRules() {
        var contract = new ResponseContract()
                .status(200)
                .contentTypeContains("application/json")
                .jsonPathEquals("$.id", "1")
                .jsonPathExists("$.name");

        var result = new ValidationEvaluator().evaluate(contract, response("{\"id\":1,\"name\":\"ATF\"}"), false);

        assertEquals(AtfReportStatus.PASS, result.status());
        assertEquals(4, result.rows().size());
        assertFalse(result.shouldThrow());
    }

    @Test
    void contractReadsArrayIndexJsonPathsUsedByApiCollectionContracts() {
        var result = new ValidationEvaluator().evaluate(
                new ResponseContract().status(200).jsonPathExists("$[0].id"),
                response("[{\"id\":1}]"), false);

        assertEquals(AtfReportStatus.PASS, result.status());
        assertTrue(result.rows().getFirst().passed());
    }

    @Test
    void postContractsUseTheLiveSnakeCaseUserIdField() {
        var item = new ValidationEvaluator().evaluate(
                ContractValidators.postItem(),
                response("{\"id\":1,\"user_id\":7}"), false);
        var relationship = new ValidationEvaluator().evaluate(
                ContractValidators.relationshipCollection("7"),
                response("[{\"id\":1,\"user_id\":7,\"title\":\"ATF post\"}]"), false);

        assertEquals(AtfReportStatus.PASS, item.status());
        assertEquals(AtfReportStatus.PASS, relationship.status());
        assertTrue(item.rows().stream().anyMatch(row -> row.name().equals("JSON path exists: $.user_id") && row.passed()));
        assertTrue(relationship.rows().stream().anyMatch(row -> row.name().equals("Every $ item user_id") && row.passed()));
    }

    @Test
    void resourceCollectionContractsRejectStructurallyIncompletePayloads() {
        var users = new ValidationEvaluator().evaluate(
                ContractValidators.userCollection(),
                response("[{\"id\":1}]"), false);
        var posts = new ValidationEvaluator().evaluate(
                ContractValidators.postCollection(),
                response("[{\"id\":101,\"user_id\":1,\"title\":\"ATF post\"}]"), false);

        assertEquals(AtfReportStatus.FAIL, users.status());
        assertEquals(AtfReportStatus.FAIL, posts.status());
        assertTrue(users.rows().stream().anyMatch(row -> row.name().equals("JSON path exists: $[0].email")
                && !row.passed()));
        assertTrue(posts.rows().stream().anyMatch(row -> row.name().equals("JSON path exists: $[0].body")
                && !row.passed()));
    }

    @Test
    void failedContractCanBeWarningWithoutThrowing() {
        var result = new ValidationEvaluator().evaluate(
                new ResponseContract().status(201).bodyContains("created"),
                response("{\"id\":1}"), true);

        assertEquals(AtfReportStatus.WARNING, result.status());
        assertFalse(result.shouldThrow());
        assertTrue(result.rows().stream().anyMatch(row -> row.resultStatus() == AtfReportStatus.WARNING));
    }

    @Test
    void contractRejectsDuplicateRulesAndSecondEvaluation() {
        var contract = new ResponseContract().status(200);
        assertThrows(IllegalArgumentException.class, () -> contract.status(200));
        var table = new ValidationTable();
        contract.evaluate(response("{}"), table);
        assertThrows(IllegalStateException.class, () -> contract.evaluate(response("{}"), new ValidationTable()));
    }

    @Test
    void schemaRuleValidatesRequiredPropertiesAndTypes() throws Exception {
        Path schema = Files.createTempFile("atf-schema", ".json");
        try {
            Files.writeString(schema, """
                    {"type":"object","required":["id","name"],"properties":{
                      "id":{"type":"integer"},"name":{"type":"string"}}}
                    """);

            var valid = new ValidationTable();
            new ResponseContract().jsonSchema(schema.toString())
                    .evaluate(response("{\"id\":1,\"name\":\"Ada\"}"), valid);
            assertTrue(valid.rows().getFirst().passed());

            var invalid = new ValidationTable();
            new ResponseContract().jsonSchema(schema.toString())
                    .evaluate(response("{\"id\":\"wrong\"}"), invalid);
            assertFalse(invalid.rows().getFirst().passed());
            assertTrue(invalid.rows().getFirst().actual().contains("required property is missing"));
        } finally {
            Files.deleteIfExists(schema);
        }
    }

    @Test
    void relativeSchemaResolutionUsesDeclaredRootAndRejectsTraversal() throws Exception {
        Path root = Files.createTempDirectory("atf-schema-root");
        Path schema = root.resolve("contracts/user.json");
        Files.createDirectories(schema.getParent());
        Files.writeString(schema, """
                {"type":"object","required":["id"],"properties":{"id":{"type":"integer"}}}
                """);
        Path outside = root.getParent().resolve("outside-schema.json");
        Files.writeString(outside, "{\"type\":\"object\"}");
        try {
            var resolver = new SchemaResourceResolver(root);
            assertEquals(schema.toRealPath(), resolver.resolve("contracts/user.json"));
            assertNull(resolver.resolve("../outside-schema.json"));

            var valid = new ValidationTable();
            new ResponseContract(resolver).jsonSchema("contracts/user.json")
                    .evaluate(response("{\"id\":1}"), valid);
            assertTrue(valid.rows().getFirst().passed());

            var escaped = new ValidationTable();
            new ResponseContract(resolver).jsonSchema("../outside-schema.json")
                    .evaluate(response("{}"), escaped);
            assertFalse(escaped.rows().getFirst().passed());
            assertEquals("schema file missing", escaped.rows().getFirst().actual());
        } finally {
            Files.deleteIfExists(outside);
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                });
            }
        }
    }
}

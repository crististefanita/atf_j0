package atf.validation.media;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfUnitTest;
import atf.validation.contract.ContractValidators;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ValidationEvaluator;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonMediaTypeContractTest {
    private record Canonical(Supplier<ResponseContract> contract, String body, int rows) { }
    private static final List<Canonical> CANONICAL = List.of(
            new Canonical(ContractValidators::userItem, "{\"id\":1,\"name\":\"ATF\"}", 4),
            new Canonical(ContractValidators::userCollection, "[{\"id\":1,\"name\":\"ATF\",\"email\":\"test@example.test\"}]", 5),
            new Canonical(ContractValidators::postItem, "{\"id\":2,\"user_id\":1}", 4),
            new Canonical(ContractValidators::postCollection, "[{\"id\":2,\"user_id\":1,\"title\":\"ATF\",\"body\":\"post\"}]", 6),
            new Canonical(() -> ContractValidators.relationshipCollection("1"), "[{\"id\":2,\"user_id\":1,\"title\":\"ATF\"}]", 5));

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "Application/JSON", "application/json; charset=UTF-8",
            " application/json ; profile=\"text/plain\"", "application/problem+json",
            "application/vnd.atf.user+JSON; charset=utf-8", "text/json", "text/vnd.atf+json"})
    void canonicalContractsAcceptConcreteJsonTypesIndependentlyOfParameters(String header) {
        for (var fixture : CANONICAL) for (boolean warning : List.of(false, true)) {
            var decision = evaluate(fixture.contract().get(), fixture.body(), header, warning);
            assertEquals(AtfReportStatus.PASS, decision.status(), header);
            assertFalse(decision.shouldThrow());
            assertEquals(fixture.rows(), decision.rows().size());
            assertTrue(decision.rows().stream().allMatch(row -> row.passed()));
        }
    }

    @ParameterizedTest @NullSource @EmptySource
    @ValueSource(strings = {" ", "text/plain", "text/plain; profile=\"application/json\"",
            "text/plain; profile=\"prefix; application/json\"", "text/html; example=application/problem+json",
            "application/jsonp", "application/json-invalid", "application/json/extra", "application/+json",
            "application /json", "application/ json", "application/json, text/plain", "application/json\r\ntext/plain",
            "*/*", "application/*+json", "; application/json"})
    void canonicalContractsRejectMissingMalformedAndImpersonatedJsonTypes(String header) {
        for (var fixture : CANONICAL) for (boolean warning : List.of(false, true)) {
            var decision = evaluate(fixture.contract().get(), fixture.body(), header, warning);
            assertEquals(warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL, decision.status(), header);
            assertEquals(!warning, decision.shouldThrow());
            assertEquals(fixture.rows(), decision.rows().size());
            var failures = decision.rows().stream().filter(row -> !row.passed()).toList();
            assertEquals(1, failures.size(), header);
            assertEquals("Content-Type JSON", failures.getFirst().name());
        }
    }

    @Test void literalContainsRemainsAnExplicitCompatibilityOperation() {
        var decision = evaluate(new ResponseContract().contentTypeContains("application/json"), "{}",
                "text/plain; profile=\"application/json\"", false);
        assertEquals(AtfReportStatus.PASS, decision.status());
    }

    @Test void semanticRuleHasIndependentIdentityAndRejectsDuplicates() {
        var contract = new ResponseContract().contentTypeJson().contentTypeContains("application/json");
        assertThrows(IllegalArgumentException.class, contract::contentTypeJson);
        var decision = evaluate(contract, "{}", "application/json; charset=utf-8", false);
        assertEquals(AtfReportStatus.PASS, decision.status());
        assertEquals(2, decision.rows().size());
        assertThrows(IllegalStateException.class, () -> evaluate(contract, "{}", "application/json", false));
    }

    @Test void mediaClassificationDoesNotSubstituteJsonBodyValidation() {
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().contentTypeJson(),
                "not-json", "application/json", false).status());
        var canonical = evaluate(ContractValidators.userItem(), "not-json", "application/json", false);
        assertEquals(AtfReportStatus.FAIL, canonical.status());
        assertTrue(canonical.rows().get(1).passed());
        assertEquals(2, canonical.rows().stream().filter(row -> !row.passed()).count());
    }

    @Test void invalidSubtypeIsRejectedBeforePreviewTruncation() {
        var decision = evaluate(new ResponseContract().contentTypeJson(), "{}", "application/json" + "x".repeat(260), false);
        assertEquals(AtfReportStatus.FAIL, decision.status());
        assertEquals(240, decision.rows().getFirst().actual().length());
    }

    @Test void headerNameRemainsCaseInsensitive() {
        var decision = new ValidationEvaluator().evaluate(new ResponseContract().contentTypeJson(),
                new ApiResponse(200, "{}", Map.of("cOnTeNt-TyPe", "APPLICATION/JSON"), Duration.ZERO, ""), false);
        assertEquals(AtfReportStatus.PASS, decision.status());
    }

    private static atf.validation.evaluation.ValidationDecision evaluate(
            ResponseContract contract, String body, String header, boolean warning) {
        var headers = header == null ? Map.<String, String>of() : Map.of("Content-Type", header);
        return new ValidationEvaluator().evaluate(contract,
                new ApiResponse(200, body, headers, Duration.ZERO, "/synthetic"), warning);
    }
}

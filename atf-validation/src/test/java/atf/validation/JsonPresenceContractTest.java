package atf.validation;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfUnitTest;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ValidationEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonPresenceContractTest {
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-json", "{", "{} trailing", "{} {}", "null true", "{\"x\":1,\"x\":2}"})
    void invalidDocumentsNeverProveAbsenceOrValues(String body) {
        for (boolean warning : new boolean[]{false, true}) {
            var decision = evaluate(new ResponseContract().jsonPathAbsent("$.secret")
                    .jsonPathEquals("$.x", "<missing>").jsonPathType("$.x", "null")
                    .jsonPathExists("$").jsonPathRequired("$.x").jsonArraySize("$", 0)
                    .everyArrayItemEquals("$", "x", "1"), body, warning);
            assertEquals(warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL, decision.status());
            assertEquals(!warning, decision.shouldThrow());
            assertEquals(7, decision.rows().size());
            assertTrue(decision.rows().stream().noneMatch(row -> row.passed()));
            assertTrue(decision.rows().stream().allMatch(row -> row.actual().contains("JSON document")));
        }
    }
    @Test void missingNullAndDiagnosticTextRemainDistinct() {
        var missing = evaluate(new ResponseContract().jsonPathAbsent("$.x").jsonPathType("$.x", "null")
                .jsonPathEquals("$.x", "<missing>"), "{}", false);
        assertTrue(missing.rows().get(0).passed());
        assertFalse(missing.rows().get(1).passed());
        assertFalse(missing.rows().get(2).passed());
        var explicitNull = evaluate(new ResponseContract().jsonPathRequired("$.x").jsonPathExists("$.x")
                .jsonPathType("$.x", "null").jsonPathAbsent("$.x"), "{\"x\":null}", false);
        assertEquals(3, explicitNull.rows().stream().filter(row -> row.passed()).count());
        assertFalse(explicitNull.rows().getLast().passed());
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().jsonPathEquals("$.x", "<missing>"),
                "{\"x\":\"<missing>\"}", false).status());
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().jsonPathType("$", "null"), "null", false).status());
    }
    @ParameterizedTest @ValueSource(strings = {"$..x", "$.*", "$[?(@.x)]", "$[01]", "$['x']", "/x~2y", "$.x/secret"})
    void unsupportedPathsCannotBecomeAbsence(String path) {
        var result = evaluate(new ResponseContract().jsonPathAbsent(path), "{}", false);
        assertEquals(AtfReportStatus.FAIL, result.status());
        assertEquals("<unsupported JSON path>", result.rows().getFirst().actual());
    }
    @Test void supportedPathsAndEvaluationIsolationPreserveFacts() {
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().jsonPathEquals("$[0].x", "7"), "[{\"x\":7}]", false).status());
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().jsonPathEquals("/a~1b/~0x", "7"), "{\"a/b\":{\"~x\":7}}", false).status());
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().jsonPathEquals("x", "7"), "{\"x\":7}", false).status());
        assertEquals(AtfReportStatus.FAIL, evaluate(new ResponseContract().jsonPathEquals("$.x", "7"), "{}", false).status());
        String prefix = "x".repeat(260);
        var longValue = evaluate(new ResponseContract().jsonPathEquals("$.x", prefix + "A"), "{\"x\":\"" + prefix + "B\"}", false);
        assertEquals(AtfReportStatus.FAIL, longValue.status());
        assertEquals(240, longValue.rows().getFirst().actual().length());
        assertThrows(IllegalArgumentException.class, () -> new ResponseContract().jsonPathType("$.x", "unsupported"));
    }
    @Test void containersCannotMasqueradeAsEmptyScalarValues() {
        for (String container : new String[]{"{}", "[]"}) {
            var scalar = evaluate(new ResponseContract().jsonPathEquals("$.x", ""), "{\"x\":" + container + "}", false);
            assertEquals(AtfReportStatus.FAIL, scalar.status());
            assertEquals(container, scalar.rows().getFirst().actual());
            assertEquals(AtfReportStatus.FAIL, evaluate(new ResponseContract().everyArrayItemEquals("$", "x", ""),
                    "[{\"x\":" + container + "}]", false).status());
        }
        assertEquals(AtfReportStatus.PASS, evaluate(new ResponseContract().jsonPathEquals("$.x", ""), "{\"x\":\"\"}", false).status());
    }
    private static atf.validation.evaluation.ValidationDecision evaluate(ResponseContract contract, String body, boolean warning) {
        return new ValidationEvaluator().evaluate(contract, new ApiResponse(200, body, Map.of(), Duration.ZERO, ""), warning);
    }
}

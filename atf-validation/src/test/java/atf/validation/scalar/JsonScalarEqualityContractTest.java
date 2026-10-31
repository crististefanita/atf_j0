package atf.validation.scalar;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.reporting.inventory.ValidationSink;
import atf.testkit.AtfUnitTest;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ApiValidationException;
import atf.validation.evaluation.ValidationEvaluator;
import atf.validation.publishing.ResponseContractReporter;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonScalarEqualityContractTest {
    static Stream<Arguments> scalarCases() {
        return Stream.of(
                Arguments.of("\"\"", "", true),
                Arguments.of("\" \"", " ", true),
                Arguments.of("\"{}\"", "{}", true),
                Arguments.of("\"[]\"", "[]", true),
                Arguments.of("\"null\"", "null", true),
                Arguments.of("null", "null", true),
                Arguments.of("null", "", false),
                Arguments.of("true", "true", true),
                Arguments.of("false", "false", true),
                Arguments.of("true", "TRUE", false),
                Arguments.of("0", "0", true),
                Arguments.of("-12", "-12", true),
                Arguments.of("9223372036854775808", "9223372036854775808", true),
                Arguments.of("1.25", "1.25", true),
                Arguments.of("1", "1.0", false),
                Arguments.of("{}", "", false),
                Arguments.of("[]", "", false),
                Arguments.of("{\"a\":1}", "", false),
                Arguments.of("[1]", "", false),
                Arguments.of("{}", "{}", false),
                Arguments.of("[]", "[]", false));
    }

    @ParameterizedTest @MethodSource("scalarCases")
    void bothRulesShareScalarSemanticsInStrictAndWarningModes(String json, String expected, boolean matches) {
        for (boolean warning : List.of(false, true)) {
            verify(new ResponseContract().jsonPathEquals("$.x", expected), "{\"x\":" + json + "}", matches, warning);
            verify(new ResponseContract().everyArrayItemEquals("$", "x", expected), "[{\"x\":" + json + "}]", matches, warning);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "[]", "{\"a\":1}", "[1]"})
    void rejectedContainersRemainVisibleAsJson(String json) {
        var decision = evaluate(new ResponseContract().jsonPathEquals("$.x", ""), "{\"x\":" + json + "}", false);
        assertEquals(AtfReportStatus.FAIL, decision.status());
        assertEquals(json, decision.rows().getFirst().actual());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "[]", "null", "", "not-json", "{} trailing"})
    void missingAndInvalidInputCannotSatisfyEmptyString(String body) {
        for (boolean warning : List.of(false, true))
            verify(new ResponseContract().jsonPathEquals("$.x", ""), body, false, warning);
    }

    @ParameterizedTest @ValueSource(strings = {"[{}]", "[null]", "[1]", "[\"\"]", "{}", "null",
            "[{\"x\":\"\"},{\"x\":{}}]", "[{\"x\":[]},{\"x\":\"\"}]"})
    void everyItemRequiresTheNamedPropertyToBeScalar(String body) {
        for (boolean warning : List.of(false, true))
            verify(new ResponseContract().everyArrayItemEquals("$", "x", ""), body, false, warning);
    }

    @Test void emptyArrayAndRootScalarCompatibilityRemainExplicit() {
        verify(new ResponseContract().everyArrayItemEquals("$", "x", ""), "[]", true, false);
        verify(new ResponseContract().everyArrayItemEquals("$", "x", ""), "[{\"x\":\"\"},{\"x\":\"\"}]", true, false);
        verify(new ResponseContract().jsonPathEquals("$", ""), "\"\"", true, false);
        verify(new ResponseContract().jsonPathEquals("$", "null"), "null", true, false);
    }

    @Test void fullComparisonPrecedesPreviewForBothRules() {
        String prefix = "audit-".repeat(45);
        for (boolean warning : List.of(false, true)) {
            var direct = evaluate(new ResponseContract().jsonPathEquals("$.x", prefix + "A"),
                    "{\"x\":\"" + prefix + "B\"}", warning);
            var items = evaluate(new ResponseContract().everyArrayItemEquals("$", "x", prefix + "A"),
                    "[{\"x\":\"" + prefix + "B\"}]", warning);
            assertFalse(direct.rows().getFirst().passed());
            assertFalse(items.rows().getFirst().passed());
            assertEquals(240, direct.rows().getFirst().actual().length());
            assertEquals(240, items.rows().getFirst().actual().length());
            assertEquals(direct.rows().getFirst().expected(), direct.rows().getFirst().actual());
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void neutralPortPublishesAllNegativeRowsBeforeStrictThrowOrWarningReturn(boolean warning) {
        var sink = new RecordingSink();
        var contract = new ResponseContract().jsonPathEquals("$.object", "").jsonPathEquals("$.array", "")
                .everyArrayItemEquals("$.items", "x", "");
        var response = response("{\"object\":{},\"array\":[],\"items\":[{\"x\":{}}]}");
        if (warning) {
            var decision = new ResponseContractReporter().validate(sink, "Scalar contract", response, contract, true);
            assertFalse(decision.shouldThrow());
        } else {
            assertThrows(ApiValidationException.class, () ->
                    new ResponseContractReporter().validate(sink, "Scalar contract", response, contract, false));
        }
        assertEquals(1, sink.tables);
        assertEquals(1, sink.completions);
        assertEquals(1, sink.closes);
        assertEquals(3, sink.rows.size());
        assertTrue(sink.rows.stream().noneMatch(AtfAssertion::passed));
        assertEquals(warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL, sink.status);
        assertTrue(sink.rows.stream().allMatch(row -> row.resultStatus() == sink.status));
    }

    private static void verify(ResponseContract contract, String body, boolean matches, boolean warning) {
        var decision = evaluate(contract, body, warning);
        var expectedStatus = matches ? AtfReportStatus.PASS : warning ? AtfReportStatus.WARNING : AtfReportStatus.FAIL;
        assertEquals(expectedStatus, decision.status(), body);
        assertEquals(!matches && !warning, decision.shouldThrow(), body);
        assertEquals(1, decision.rows().size());
        assertEquals(matches, decision.rows().getFirst().passed(), body);
        assertEquals(expectedStatus, decision.rows().getFirst().resultStatus(), body);
    }
    private static atf.validation.evaluation.ValidationDecision evaluate(ResponseContract contract, String body, boolean warning) {
        return new ValidationEvaluator().evaluate(contract, response(body), warning);
    }
    private static ApiResponse response(String body) {
        return new ApiResponse(200, body, Map.of(), Duration.ZERO, "");
    }
    private static final class RecordingSink implements ValidationSink {
        private List<AtfAssertion> rows = List.of();
        private AtfReportStatus status;
        private int tables, completions, closes;
        @Override public Scope startValidation(String title) {
            return new Scope() {
                @Override public void assertions(Collection<AtfAssertion> comparisons) { tables++; rows = List.copyOf(comparisons); }
                @Override public void complete(AtfReportStatus result) { completions++; status = result; }
                @Override public void close() { closes++; }
            };
        }
    }
}

package atf.core.json;

import atf.core.http.response.JsonBodyPolicy;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonCompleteDocumentTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={" ", "{} trailing", "{} {}", "[] false", "null true", "1 2", "{", "[1,]",
            "{\"x\":1,\"x\":2}", "{\"a\":{\"x\":1,\"x\":2}}", "[{\"x\":1,\"x\":2}]"})
    void refusesIncompleteAmbiguousAndMultipleDocuments(String body) {
        assertFalse(JsonBodyPolicy.validDocument(body), "one complete unambiguous JSON value");
    }
    @ParameterizedTest
    @ValueSource(strings={"{}", "[]", "null", " true ", "42", "\"\"", "{\"v\":[true,null]}",
            "{\"amount\":9007199254740993.0}", "1e309", "-0.0"})
    void acceptsCompleteValuesAndSeparatesNullFromWritePolicy(String body) {
        assertTrue(JsonBodyPolicy.validDocument(body));
        assertEquals(!body.strip().equals("null"), JsonBodyPolicy.validWriteDocument(body));
    }
    @ParameterizedTest
    @ValueSource(strings={"{} trailing", "{} {}", "{\"x\":1,\"x\":2}"})
    void checksBothWriteExchangeSides(String invalid) {
        assertEquals(java.util.List.of("request JSON body is not valid JSON", "response JSON body is not valid JSON"),
                JsonBodyPolicy.validateWriteExchange(invalid, invalid));
    }
}

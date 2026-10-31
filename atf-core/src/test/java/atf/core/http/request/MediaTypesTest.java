package atf.core.http.request;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class MediaTypesTest {
    @ParameterizedTest
    @ValueSource(strings = {"application/json", "APPLICATION/JSON; charset=UTF-8", " application/json ",
            "application/problem+json", "application/vnd.atf.order+json; profile=compact", "text/json"})
    void acceptsJsonOnlyFromTheConcreteTypeAndSubtype(String value) {
        assertTrue(MediaTypes.isJson(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "text/plain; profile=json", "text/plain; filename=sample.json",
            "application/notjson", "application/jsonp", "application/json-seq", "json", "/json",
            "application/+json", "application/*+json", "*/json", "application/ json", "application/json/extra"})
    void rejectsParameterSubstringsLookalikesAndInvalidMediaTypes(String value) {
        assertFalse(MediaTypes.isJson(value));
    }
}

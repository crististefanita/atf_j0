package atf.core.security.json;

import atf.core.security.SensitiveDataRedactor;
import atf.testkit.AtfUnitTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonRedactionFidelityTest {
    @ParameterizedTest
    @ValueSource(strings={"9007199254740993.0", "0.12345678901234567890123456789", "1e309", "1e-309", "-0.0", "1.2300"})
    void preservesPublicNumericTokensExactly(String value) throws Exception {
        String input="{\"value\":"+value+",\"password\":\"synthetic-secret\"}";
        String actual=new SensitiveDataRedactor().redactJson(input);
        assertEquals(List.of(value), numbers(actual));
        assertFalse(actual.contains("synthetic-secret"));
    }
    @Test void keepsEveryDuplicateWhileMaskingEachSensitiveValue() throws Exception {
        String input="{\"seen\":1,\"password\":{\"nested\":\"secret-a\"},\"seen\":2,\"password\":[\"secret-b\"]}";
        String actual=new SensitiveDataRedactor().redactJson(input);
        assertEquals(List.of("1","2"),numbers(actual));
        assertEquals(2,occurrences(actual,"\"seen\""));
        assertEquals(2,occurrences(actual,"***REDACTED***"));
        assertFalse(actual.contains("secret-a"));assertFalse(actual.contains("secret-b"));
    }
    private static List<String> numbers(String json) throws Exception {
        var values=new ArrayList<String>();
        try(var parser=new JsonFactory().createParser(json)) {
            for(JsonToken token; (token=parser.nextToken())!=null;) if(token.isNumeric()) values.add(parser.getText());
        }
        return values;
    }
    private static int occurrences(String value,String token) {return (value.length()-value.replace(token,"").length())/token.length();}
}

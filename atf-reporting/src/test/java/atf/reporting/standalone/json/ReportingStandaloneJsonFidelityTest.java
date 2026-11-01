package atf.reporting.standalone.json;

import atf.reporting.model.ReportJsonBodyFormatter;
import atf.reporting.security.SensitiveDataRedactor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ReportingStandaloneJsonFidelityTest {
    @ParameterizedTest
    @ValueSource(strings={"9007199254740993.0","0.12345678901234567890123456789","1e309","1e-309","-0.0","1.2300"})
    void redactionAndPrettyPrintKeepExactPublicNumericTokens(String value) throws Exception {
        String input="{\"value\":"+value+",\"password\":\"synthetic-secret\"}";
        String redacted=new SensitiveDataRedactor().redactJson(input);
        assertEquals(List.of(value),numbers(redacted),"redaction boundary");
        assertEquals(List.of(value),numbers(ReportJsonBodyFormatter.format(input)),"formatter boundary");
        String published=ReportJsonBodyFormatter.format(redacted);
        assertEquals(List.of(value),numbers(published),"combined projection");
        assertFalse(published.contains("synthetic-secret"));
    }
    @Test void keepsDuplicatePublicAndSensitiveFieldOccurrences() throws Exception {
        String input="{\"seen\":1,\"password\":{\"v\":\"secret-a\"},\"seen\":2,\"password\":[\"secret-b\"]}";
        String actual=ReportJsonBodyFormatter.format(new SensitiveDataRedactor().redactJson(input));
        assertEquals(List.of("1","2"),numbers(actual));
        assertEquals(2,occurrences(actual,"\"seen\""));
        assertEquals(2,occurrences(actual,"***REDACTED***"));
        assertFalse(actual.contains("secret-a"));assertFalse(actual.contains("secret-b"));
    }
    @Test void distinctShortJsonAssertionsStayDistinctAndInvalidTextStaysAuthored() {
        var redactor=new SensitiveDataRedactor();
        assertNotEquals(redactor.redactField("JSON value","{\"amount\":9007199254740992.0}"),
                redactor.redactField("JSON value","{\"amount\":9007199254740993.0}"));
        for(String invalid:List.of("{} {}","{bad","1 2")) assertEquals(invalid,ReportJsonBodyFormatter.format(invalid));
        assertEquals("null",ReportJsonBodyFormatter.format("null"));
    }
    private static List<String> numbers(String value) throws Exception {
        var result=new ArrayList<String>();
        try(var parser=new JsonFactory().createParser(value)) {
            for(JsonToken token;(token=parser.nextToken())!=null;) if(token.isNumeric())result.add(parser.getText());
        }
        return result;
    }
    private static int occurrences(String value,String token) {return (value.length()-value.replace(token,"").length())/token.length();}
}

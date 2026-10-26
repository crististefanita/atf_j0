package atf.reporting.model;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportJsonBodyFormatterTest {
    @Test
    void formatsObjectsAndArraysDeterministically() {
        assertEquals("{\n  \"user\" : {\n    \"id\" : 7,\n    \"roles\" : [\n      \"admin\",\n      \"auditor\"\n    ]\n  }\n}",
                ReportJsonBodyFormatter.format("{\"user\":{\"id\":7,\"roles\":[\"admin\",\"auditor\"]}}"));
    }

    @Test
    void preservesScalarsNullAndInvalidPayloadsWithoutInventingData() {
        assertEquals("null", ReportJsonBodyFormatter.format("null"));
        assertEquals("true", ReportJsonBodyFormatter.format("true"));
        assertEquals("\"text\"", ReportJsonBodyFormatter.format("\"text\""));
        assertEquals("not-json", ReportJsonBodyFormatter.format("not-json"));
        assertEquals("1 2", ReportJsonBodyFormatter.format("1 2"));
        assertTrue(ReportJsonBodyFormatter.format("  ").isBlank());
    }
}

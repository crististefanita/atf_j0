package atf.reporting.security;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class JsonEvidenceRedactorTest {
    @Test void validValuesAndDuplicateFieldsRemainIntactExceptForSensitiveValues() {
        var redactor=new SensitiveDataRedactor("MASK");
        String safe=redactor.redactJson("{\"keep\":1,\"keep\":2,\"password\":false,\"items\":[{\"api_key\":null}],\"text\":\"visible\"}");
        assertEquals("{\"keep\":1,\"keep\":2,\"password\":\"MASK\",\"items\":[{\"api_key\":\"MASK\"}],\"text\":\"visible\"}",safe);
    }
    @Test void invalidJsonEvidenceIsOmittedWithoutChangingUnrelatedTextOrLocatorSyntax() {
        var redactor=new SensitiveDataRedactor();
        assertEquals("[invalid JSON omitted]",redactor.redactJson("{\"password\":123456789,"));
        assertEquals("[class~='token']",redactor.redactText("[class~='token']"));
        assertEquals("plain diagnostic",redactor.redactJson("plain diagnostic"));
        assertNull(redactor.redactJson(null));
        assertEquals("",redactor.redactJson(""));
    }
    @Test void textAndNamedFieldsAlsoProtectCompleteJsonBeforePreviewBudgeting() {
        var redactor=new SensitiveDataRedactor();
        assertFalse(redactor.redactText("{\"to\\u006ben\":{\"nested\":\"canary\"}}").contains("canary"));
        assertEquals("***REDACTED***",redactor.redactField("Client-Secret","safe-looking"));
    }
}

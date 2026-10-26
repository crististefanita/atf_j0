package atf.core.http;
import atf.testkit.AtfUnitTest;

import atf.core.http.response.JsonBodyPolicy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class JsonBodyPolicyTest {
    @Test
    void appliesOnlyToPostAndPutAndRejectsNullOrMalformedWriteDocuments() {
        assertTrue(JsonBodyPolicy.appliesTo("POST"));
        assertTrue(JsonBodyPolicy.appliesTo("put"));
        assertFalse(JsonBodyPolicy.appliesTo("GET"));
        assertTrue(JsonBodyPolicy.validWriteDocument("{\"ok\":true}"));
        assertFalse(JsonBodyPolicy.validWriteDocument("null"));
        assertFalse(JsonBodyPolicy.validWriteDocument("{malformed"));
    }

    @Test
    void validatesBothSidesOfTheWriteExchangeWithOnePolicy() {
        assertEquals(
                java.util.List.of("response JSON body is not valid JSON"),
                JsonBodyPolicy.validateWriteExchange("{\"request\":true}", "{malformed"));
        assertTrue(JsonBodyPolicy.validateWriteExchange("{\"request\":true}", "{\"ok\":true}").isEmpty());
    }
}

package atf.reporting.semantic;

import atf.core.http.request.HttpMethod;
import atf.core.http.response.ApiOperationDescriptor;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ApiOperationDescriptorContractTest {
    @Test
    void keepsProtocolMetadataSeparateFromShortHumanIntent() {
        var descriptor = ApiOperationDescriptor.of(HttpMethod.GET, "/products/42",
                "read selected product");

        assertEquals(HttpMethod.GET.value(), descriptor.method());
        assertEquals("/products/42", descriptor.relativePath());
        assertEquals("GET /products/42 — read selected product", descriptor.label());
        assertTrue(descriptor.hasDescription());
    }

    @Test
    void rejectsIncompleteOperationMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiOperationDescriptor("GET", "", "read"));
        assertThrows(IllegalArgumentException.class,
                () -> new ApiOperationDescriptor("", "/users", "read"));
    }
}

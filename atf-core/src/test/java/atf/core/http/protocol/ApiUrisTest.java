package atf.core.http.protocol;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ApiUrisTest {
    @Test
    void buildsReusableHttpAndHttpsEndpoints() {
        assertEquals("http://localhost:8080", ApiUris.httpEndpoint("localhost:8080").toString());
        assertEquals("https://api.example.test", ApiUris.httpsEndpoint("api.example.test").toString());
    }

    @Test
    void resolvesPathsAndQueriesWithoutStringConcatenation() {
        assertEquals("https://api.example.test/products/search?q=Pliers",
                ApiUris.httpsUrl("api.example.test", "/products/search?q=Pliers"));
        assertEquals("https://api.example.test/checkout",
                ApiUris.resolve(ApiUris.httpsEndpoint("api.example.test"), "checkout").toString());
    }

    @Test
    void rejectsMissingProtocolParts() {
        assertThrows(IllegalArgumentException.class, () -> ApiUris.endpoint(" ", "api.example.test"));
        assertThrows(IllegalArgumentException.class, () -> ApiUris.httpsEndpoint(" "));
    }
}

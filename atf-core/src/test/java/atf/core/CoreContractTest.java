package atf.core;

import atf.core.http.request.ApiRoute;
import atf.core.http.request.ApiUrlBuilder;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.security.RedactionSettings;
import atf.core.security.SensitiveDataRedactor;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class CoreContractTest {
    @Test
    void statusPrecedenceKeepsFailAboveSkipWarningAndPass() {
        assertEquals(AtfReportStatus.FAIL, AtfReportStatus.worst(AtfReportStatus.WARNING, AtfReportStatus.FAIL));
        assertEquals(AtfReportStatus.SKIP, AtfReportStatus.worst(AtfReportStatus.SKIP, AtfReportStatus.PASS));
    }

    @Test
    void routeResolutionEncodesValuesAndRejectsUnresolvedTokens() {
        var route = new ApiRoute("user", "GET", "/users/{id}", "users", "item", "default");
        assertEquals("/users/a%20b", route.resolvePath(Map.of("id", "a b")));
        assertThrows(IllegalArgumentException.class, () -> route.resolvePath(Map.of()));
    }

    @Test
    void postAndPutRoutesRequireAValidNonNullJsonBody() {
        var post = new ApiRoute("create", "POST", "/users", "users", "create", "default");
        var put = new ApiRoute("update", "PUT", "/users/{id}", "users", "update", "default");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> post.request(Map.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> post.requestBody("", Map.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> put.requestBody("not-json", Map.of("id", 1))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> put.requestBody("null", Map.of("id", 1))),
                () -> assertEquals("application/json",
                        post.requestBody("{\"name\":\"ATF\"}", Map.of())
                                .headers().get("Content-Type")));
    }

    @Test
    void urlBuilderDoesNotDuplicateSlashes() {
        var route = new ApiRoute("users", "GET", "/users", "users", "collection", "none");
        var request = route.request(Map.of());
        assertEquals("https://example.test/users", new ApiUrlBuilder(URI.create("https://example.test/")).build(request));
    }

    @Test
    void redactionMasksSecretsWithoutChangingOtherEvidence() {
        var redactor = new SensitiveDataRedactor();
        var safe = redactor.redact("{\"name\":\"ATF\",\"token\":\"secret\"}");
        assertTrue(safe.contains("ATF"));
        assertFalse(safe.contains("secret"));
        assertTrue(safe.contains("REDACTED"));
    }

    @Test
    void structuredRedactionMasksNestedJsonArraysAndNamedFields() {
        var redactor = new SensitiveDataRedactor();
        var safe = redactor.redactJson("{\"user\":{\"email\":\"person@example.test\",\"password\":\"secret\"},\"items\":[{\"token\":\"abc\"}]}");

        assertTrue(safe.contains("person@example.test"));
        assertFalse(safe.contains("secret"));
        assertFalse(safe.contains("abc"));
        assertEquals("***REDACTED***", redactor.redactField("Authorization", "Bearer secret"));
    }

    @Test
    void evidenceRedactionMasksSecretsInQueryAndSensitivePathSegments() {
        var redactor = new SensitiveDataRedactor();
        var safe = redactor.redact("GET /users/secret/raw-secret?token=query-secret&mode=full");

        assertFalse(safe.contains("raw-secret"));
        assertFalse(safe.contains("query-secret"));
        assertTrue(safe.contains("mode=full"));
        assertTrue(safe.contains("***REDACTED***"));
    }

    @Test
    void piiRedactionIsExplicitAndDoesNotChangeTransportDefaults() {
        var redactor = new SensitiveDataRedactor(new RedactionSettings(
                true, "[hidden]", false, true, Set.of("authorization")));

        assertEquals("[hidden] [hidden]", redactor.redact("person@example.test +40 (721) 123-456"));
        assertEquals("safe@example.test", new SensitiveDataRedactor().redact("safe@example.test"));
    }
}

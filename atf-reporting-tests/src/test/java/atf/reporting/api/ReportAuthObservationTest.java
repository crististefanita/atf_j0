package atf.reporting.api;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component contract for the renderer-neutral, credential-free auth vocabulary. */
@AtfUnitTest
class ReportAuthObservationTest {
    @Test
    void classifiesTypedProfileWithoutExposingCredentialValues() {
        var request = new ReportRequest("GET", "/session", Map.of(),
                Map.of(), "bearer-token-profile", "");

        var observation = request.authObservation();

        assertAll(
                () -> assertEquals(ReportAuthMethod.BEARER, observation.method()),
                () -> assertEquals(ReportAuthSource.PROFILE, observation.source()),
                () -> assertEquals("bearer (source=profile)", observation.summary()),
                () -> assertFalse(observation.summary().contains("token")));
    }

    @Test
    void classifiesHeaderAndQuerySourcesWithoutReadingTheirValuesIntoTheProjection() {
        var headerRequest = new ReportRequest("GET", "/users", Map.of(),
                Map.of("Authorization", "Bearer real-secret", "X-Trace", "one"), "none", "");
        var queryRequest = new ReportRequest("GET", "/download", Map.of("X-Amz-Signature", "secret"),
                Map.of(), "none", "");
        var unknownRequest = new ReportRequest("GET", "/private", Map.of(),
                Map.of("Authorization", "Digest opaque"), "none", "");

        assertAll(
                () -> assertEquals(ReportAuthMethod.BEARER, headerRequest.authMethod()),
                () -> assertEquals(ReportAuthSource.HEADER, headerRequest.authObservation().source()),
                () -> assertEquals(ReportAuthMethod.SIGNED_URL, queryRequest.authMethod()),
                () -> assertEquals(ReportAuthSource.QUERY, queryRequest.authObservation().source()),
                () -> assertEquals(ReportAuthMethod.OTHER_UNKNOWN, unknownRequest.authMethod()),
                () -> assertTrue(headerRequest.authObservation().summary().contains("bearer")),
                () -> assertFalse(headerRequest.authObservation().summary().contains("real-secret")),
                () -> assertFalse(queryRequest.authObservation().summary().contains("secret")));
    }

    @Test
    void exposesTheCompleteStableTaxonomy() {
        assertEquals(
                java.util.Set.of("none", "bearer", "cookie", "api/function-key", "signed-url",
                        "token-acquisition", "basic", "other/unknown"),
                java.util.Arrays.stream(ReportAuthMethod.values())
                        .map(ReportAuthMethod::wireValue).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void observedSchemeWinsOverAnArbitraryOrMisleadingProfileName() {
        for (String profile : List.of("shop-user", "basic-profile", "none")) {
            var request = new ReportRequest("GET", "/private", Map.of(),
                    Map.of("aUtHoRiZaTiOn", "Bearer private-token"), profile, "");
            assertEquals(profile, request.authProfile());
            assertEquals(ReportAuthMethod.BEARER, request.authMethod());
            assertEquals(ReportAuthSource.HEADER, request.authObservation().source());
        }
        var unknown = new ReportRequest("GET", "/private", Map.of(),
                Map.of("Authorization", "Bearerish private-token"), "bearer-profile", "");
        assertEquals(ReportAuthMethod.OTHER_UNKNOWN, unknown.authMethod());
    }

    @Test
    void typedObservationSurvivesRedactionWithoutReclassifyingTheMaskedHeader() {
        var original = new ReportRequest("GET", "/private", Map.of(),
                Map.of("Authorization", "Basic private-credential"), "shop-user", "");
        var redacted = new ReportRequest(original.method(), original.path(), original.query(),
                Map.of("Authorization", "<redacted>"), original.authProfile(), "",
                original.authObservation());
        assertEquals(ReportAuthMethod.BASIC, redacted.authMethod());
        assertEquals(ReportAuthSource.HEADER, redacted.authObservation().source());
        assertEquals(original.authObservation(), ReportAuthObservation.from(redacted));
        assertFalse(redacted.toString().contains("private-credential"));
    }
}

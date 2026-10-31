package atf.core.http.auth;

import atf.testkit.AtfUnitTest;
import atf.core.http.request.ApiRoute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
@Isolated("Exercises authentication protocol names under the Turkish JVM locale")
class AuthProfilesTest {
    @ParameterizedTest
    @CsvSource({"BEARER, Authorization, Bearer profile-token", "BASIC, Authorization, Basic dXNlcjpwYXNz",
            "API_KEY, X-Api-Key, profile-token", "CUSTOM, X-Custom, profile-value", "NONE, Authorization, caller-value"})
    void aSelectedProfileReplacesOnlyItsOwnedHeader(AuthType type, String header, String expected) {
        var profile = new AuthProfile("fixture", type, "profile-token", "user", "pass", "X-Api-Key",
                Map.of("X-Custom", "profile-value"));
        var builder = HttpRequest.newBuilder(URI.create("https://fixture.invalid"))
                .header(header.toLowerCase(Locale.ROOT), "caller-value").header("X-Unrelated", "unchanged");
        new ConfiguredApiAuthenticator(Map.of("fixture", profile)).apply(builder, "fixture");
        var request = builder.build();
        assertEquals(List.of(expected), request.headers().allValues(header));
        assertEquals(List.of("unchanged"), request.headers().allValues("X-Unrelated"));
    }

    @Test
    void authenticatorAndRoutePolicyUseLocaleIndependentProtocolNames() {
        Locale previous = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var authenticator = new ConfiguredApiAuthenticator(Map.of("fixture",
                    new AuthProfile("fixture", AuthType.API_KEY, "unit-value", null, null, "X-Api-Key", Map.of())));
            var route = new ApiRoute("fixture", "GET", "/fixture", "fixture", "read", "fixture",
                    AuthType.API_KEY, AuthRequirement.REQUIRED, null, Set.of());
            assertEquals("api_key", authenticator.describeMode("fixture"));
            assertEquals("required", route.authPolicy());
        } finally {
            Locale.setDefault(previous);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }

    @Test
    void normalizesMissingProfilesToNoAuthentication() {
        assertEquals(AuthProfiles.NONE, AuthProfiles.normalize(null));
        assertEquals(AuthProfiles.NONE, AuthProfiles.normalize("  "));
        assertEquals("customer", AuthProfiles.normalize(" customer "));
    }

    @Test
    void derivesStableNamesForStandardAuthenticationTypes() {
        assertEquals("bearer", AuthProfiles.standard(AuthType.BEARER));
        assertEquals("api-key", AuthProfiles.standard(AuthType.API_KEY));
    }
}

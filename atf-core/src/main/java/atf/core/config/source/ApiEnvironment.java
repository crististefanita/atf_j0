package atf.core.config.source;

import atf.core.http.auth.AuthProfiles;

import java.net.URI;

/** Stable acceptance defaults shared by API composition roots and mock-backed consumers. */
public final class ApiEnvironment {
    public static final URI MOCK_BASE_URI = URI.create("https://atf.local");
    public static final String NO_AUTH_PROFILE = AuthProfiles.NONE;

    private ApiEnvironment() {
    }
}

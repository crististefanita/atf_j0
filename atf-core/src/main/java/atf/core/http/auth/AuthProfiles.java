package atf.core.http.auth;

import java.util.Locale;
import java.util.Objects;

/** Shared names and normalization rules for API authentication profiles. */
public final class AuthProfiles {
    public static final String NONE = "none";

    private AuthProfiles() { }

    /** Returns the conventional profile name for a standard authentication type. */
    public static String standard(AuthType type) {
        return Objects.requireNonNull(type, "type").name()
                .toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** Converts an optional configured profile to the stable no-auth sentinel. */
    public static String normalize(String profile) {
        return profile == null || profile.isBlank() ? NONE : profile.trim();
    }
}

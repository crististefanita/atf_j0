package atf.core.http.protocol;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

/** Canonical HTTP(S) endpoint and URL construction shared by ATF modules. */
public final class ApiUris {
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";

    private ApiUris() { }

    public static URI httpEndpoint(String authority) {
        return endpoint(HTTP_SCHEME, authority);
    }

    public static URI httpsEndpoint(String authority) {
        return endpoint(HTTPS_SCHEME, authority);
    }

    public static URI endpoint(String scheme, String authority) {
        String requiredScheme = required(scheme, "scheme").toLowerCase(Locale.ROOT);
        String requiredAuthority = required(authority, "authority");
        try {
            return new URI(requiredScheme, requiredAuthority, null, null, null);
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException(
                    "invalid " + requiredScheme.toUpperCase(Locale.ROOT)
                            + " authority: " + requiredAuthority,
                    error);
        }
    }

    public static String httpsUrl(String authority, String target) {
        return resolve(httpsEndpoint(authority), target).toASCIIString();
    }

    public static URI resolve(URI endpoint, String target) {
        URI requiredEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        String normalizedTarget = Objects.requireNonNull(target, "target").trim();
        if (normalizedTarget.isEmpty()) normalizedTarget = "/";
        if (!normalizedTarget.startsWith("/")) normalizedTarget = "/" + normalizedTarget;
        return requiredEndpoint.resolve(normalizedTarget);
    }

    private static String required(String value, String name) {
        String requiredValue = Objects.requireNonNull(value, name).trim();
        if (requiredValue.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return requiredValue;
    }
}

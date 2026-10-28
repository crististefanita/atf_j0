package atf.ui.recovery;

import java.net.URI;
import java.util.*;

/** Consumer-owned semantic classification; an HTTP verb alone never grants read-only permission. */
public record UiRecoveryOperation(String name, Safety safety, URI pageOrigin, URI endpoint, String method) {
    public enum Safety { READ_ONLY, MUTATION, UNKNOWN }
    public UiRecoveryOperation {
        if (name == null || !name.matches("[a-z][a-z0-9-]{0,79}")) throw new IllegalArgumentException("Invalid semantic recovery operation");
        Objects.requireNonNull(safety); origin(pageOrigin); origin(endpoint);
        if (endpoint.getRawQuery() != null || endpoint.getRawFragment() != null) throw new IllegalArgumentException("Recovery route must exclude query/payload identity");
        method = Objects.requireNonNull(method).toUpperCase(Locale.ROOT);
        if (!Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS").contains(method)) throw new IllegalArgumentException("Invalid recovery request method");
        if (safety == Safety.READ_ONLY && !Set.of("GET", "HEAD").contains(method)) {
            throw new IllegalArgumentException("This recovery increment requires an explicitly classified GET/HEAD read");
        }
    }
    public boolean matches(String verb, String url) {
        try { var actual = URI.create(url); return method.equals(verb) && origin(endpoint).equals(origin(actual))
                && Objects.equals(endpoint.getRawPath(), actual.getRawPath()); }
        catch (IllegalArgumentException invalid) { return false; }
    }
    public static String origin(URI uri) {
        Objects.requireNonNull(uri);
        String scheme = Objects.toString(uri.getScheme(), "").toLowerCase(Locale.ROOT);
        if (!Set.of("http", "https").contains(scheme) || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException("Recovery requires a credential-free HTTP(S) origin");
        int port = uri.getPort() >= 0 ? uri.getPort() : scheme.equals("https") ? 443 : 80;
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }
}

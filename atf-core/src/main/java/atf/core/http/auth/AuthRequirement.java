package atf.core.http.auth;

    /** Declares whether a route accepts, recommends, or requires authentication. */
public enum AuthRequirement {
    NONE,
    OPTIONAL,
    REQUIRED
}

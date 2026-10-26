package atf.core.http.auth;
import atf.core.http.request.HttpHeader;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class ConfiguredApiAuthenticator implements ApiAuthenticator {
    private final Map<String, AuthProfile> profiles;

    public ConfiguredApiAuthenticator(Map<String, AuthProfile> profiles) {
        this.profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
    }

    @Override
    public void apply(HttpRequest.Builder request, String profileName) {
        var profile = profile(profileName);
        switch (profile.type()) {
            case NONE -> { }
            case BEARER -> request.header(HttpHeader.AUTHORIZATION.value(), "Bearer " + required(profile.token(), profile.name()));
            case API_KEY -> request.header(required(profile.headerName(), profile.name()), required(profile.token(), profile.name()));
            case BASIC -> {
                String raw = required(profile.username(), profile.name()) + ":" + required(profile.password(), profile.name());
                String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                request.header(HttpHeader.AUTHORIZATION.value(), "Basic " + encoded);
            }
            case CUSTOM -> profile.customHeaders().forEach(request::header);
        }
    }

    @Override
    public String describeMode(String profileName) {
        return profile(profileName).type().name().toLowerCase();
    }

    @Override
    public String describeTokenState(String profileName) {
        var profile = profile(profileName);
        return switch (profile.type()) {
            case NONE -> "not required";
            case CUSTOM -> profile.customHeaders().isEmpty() ? "missing" : "configured";
            default -> hasCredentials(profile) ? "configured" : "missing";
        };
    }

    private AuthProfile profile(String name) {
        String key = AuthProfiles.normalize(name);
        return profiles.getOrDefault(key, AuthProfile.none(key));
    }

    private static boolean hasCredentials(AuthProfile profile) {
        return switch (profile.type()) {
            case BASIC -> !profile.username().isBlank() && !profile.password().isBlank();
            case API_KEY, BEARER -> !profile.token().isBlank();
            default -> true;
        };
    }

    private static String required(String value, String profile) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Credentials missing for auth profile '" + profile + "'");
        return value;
    }
}

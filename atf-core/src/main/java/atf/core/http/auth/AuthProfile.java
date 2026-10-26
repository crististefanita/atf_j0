package atf.core.http.auth;

import java.util.Map;

public record AuthProfile(
        String name,
        AuthType type,
        String token,
        String username,
        String password,
        String headerName,
        Map<String, String> customHeaders) {
    public AuthProfile {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        type = type == null ? AuthType.NONE : type;
        token = token == null ? "" : token;
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        headerName = headerName == null ? "" : headerName;
        customHeaders = customHeaders == null ? Map.of() : Map.copyOf(customHeaders);
    }

    public static AuthProfile none(String name) {
        return new AuthProfile(name, AuthType.NONE, "", "", "", "", Map.of());
    }
}

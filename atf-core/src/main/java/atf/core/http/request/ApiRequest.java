package atf.core.http.request;

import atf.core.http.auth.AuthProfiles;

import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApiRequest(
        HttpRequest.BodyPublisher body,
        String method,
        String path,
        Map<String, String> query,
        Map<String, String> headers,
        String authProfile,
        ApiRoute route,
        String bodyText,
        ApiQueryParameters queryParameters) {

    public ApiRequest(HttpRequest.BodyPublisher body, String method, String path,
                      Map<String, String> query, Map<String, String> headers,
                      String authProfile, ApiRoute route) {
        this(body, method, path, query, headers, authProfile, route, null);
    }

    public ApiRequest(HttpRequest.BodyPublisher body, String method, String path,
                      Map<String, String> query, Map<String, String> headers,
                      String authProfile, ApiRoute route, String bodyText) {
        this(body, method, path, query, headers, authProfile, route, bodyText,
                ApiQueryParameters.fromMap(query));
    }

    public ApiRequest(HttpRequest.BodyPublisher body, String method, String path,
                      ApiQueryParameters queryParameters, Map<String, String> headers,
                      String authProfile, ApiRoute route, String bodyText) {
        this(body, method, path, queryParameters == null ? Map.of() : queryParameters.asMap(),
                headers, authProfile, route, bodyText, queryParameters);
    }

    public ApiRequest {
        if (method == null || method.isBlank()) throw new IllegalArgumentException("method must not be blank");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path must not be blank");
        query = query == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(query));
        queryParameters = queryParameters == null ? ApiQueryParameters.fromMap(query) : queryParameters;
        if (!queryParameters.asMap().equals(query)) {
            query = queryParameters.asMap();
        }
        headers = headers == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(headers));
        query.forEach((key, value) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("query names must not be blank");
            if (value == null) throw new IllegalArgumentException("query values must not be null");
        });
        authProfile = AuthProfiles.normalize(authProfile);
        bodyText = bodyText == null ? "" : bodyText;
    }

    public static ApiRequest of(String method, String path) {
        return new ApiRequest(HttpRequest.BodyPublishers.noBody(), method, path,
                Map.of(), Map.of(), AuthProfiles.NONE, null);
    }
}

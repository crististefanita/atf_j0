package atf.core.http.request;
import atf.core.http.auth.AuthRequirement;
import atf.core.http.auth.AuthType;
import atf.core.http.auth.AuthProfiles;
import atf.core.http.response.JsonBodyPolicy;
import atf.core.routing.ApiPathTemplate;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record ApiRoute(
        String name,
        String method,
        String pathTemplate,
        String resource,
        String operation,
        String authProfile,
        AuthType authType,
        AuthRequirement authRequirement,
        String authPolicy,
        java.util.Set<String> tags) {

    public ApiRoute(String name, String method, String pathTemplate, String resource, String operation,
                    String authProfile) {
        this(name, method, pathTemplate, resource, operation, authProfile,
                AuthType.NONE, AuthRequirement.NONE, "anonymous", java.util.Set.of());
    }

    public ApiRoute {
        require(name, "name");
        require(method, "method");
        require(pathTemplate, "pathTemplate");
        require(resource, "resource");
        require(operation, "operation");
        authProfile = AuthProfiles.normalize(authProfile);
        authType = authType == null ? AuthType.NONE : authType;
        authRequirement = authRequirement == null ? AuthRequirement.NONE : authRequirement;
        authPolicy = authPolicy == null || authPolicy.isBlank() ? authRequirement.name().toLowerCase(Locale.ROOT) : authPolicy;
        tags = tags == null ? java.util.Set.of() : java.util.Set.copyOf(tags);
    }

    public String resolvePath(Map<String, ?> values) {
        return ApiPathTemplate.of(pathTemplate).resolve(values);
    }

    public ApiRequest request(Map<String, ?> values) {
        return request(values, Map.of(), Map.of());
    }

    public ApiRequest request(Map<String, ?> values, Map<String, String> query,
                              Map<String, String> headers) {
        return request(values, ApiQueryParameters.fromMap(query), headers);
    }

    public ApiRequest request(Map<String, ?> values, ApiQueryParameters query,
                              Map<String, String> headers) {
        if (JsonBodyPolicy.appliesTo(method)) {
            throw new IllegalArgumentException(method.toUpperCase(Locale.ROOT)
                    + " routes require requestBody(...) with a non-empty JSON body");
        }
        return new ApiRequest(java.net.http.HttpRequest.BodyPublishers.noBody(), method,
                resolvePath(values), query, headers, authProfile, this, "");
    }

    public ApiRequest requestBody(String bodyJson, Map<String, ?> values) {
        return requestBody(bodyJson, values, Map.of(), Map.of());
    }

    public ApiRequest requestBody(String bodyJson, Map<String, ?> values,
                                  Map<String, String> query, Map<String, String> headers) {
        return requestBody(bodyJson, values, ApiQueryParameters.fromMap(query), headers);
    }

    public ApiRequest requestBody(String bodyJson, Map<String, ?> values,
                                  ApiQueryParameters query, Map<String, String> headers) {
        JsonBodyPolicy.requireWriteDocument(bodyJson, "bodyJson");
        var effectiveHeaders = new LinkedHashMap<>(headers == null ? Map.of() : headers);
        effectiveHeaders.put(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON);
        return new ApiRequest(java.net.http.HttpRequest.BodyPublishers.ofString(bodyJson), method,
                resolvePath(values), query, effectiveHeaders, authProfile, this, bodyJson);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}

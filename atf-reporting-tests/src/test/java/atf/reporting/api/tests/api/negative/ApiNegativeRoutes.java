package atf.reporting.api.tests.api.negative;
import atf.core.config.source.ApiEnvironment;

import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.mock.MockRoutePaths;

import java.net.http.HttpRequest;
import java.util.Map;

/** Wire-level request shapes for the negative API matrix. */
public final class ApiNegativeRoutes {
    private static final String INVALID_JSON_OUTCOME_BODY =
            "{\"rawBody\":\"not-json\",\"declaredFormat\":\"invalid-json\"}";
    private static final String MISSING_BODY_OUTCOME_BODY =
            "{\"body\":null,\"outcome\":\"missing-body\"}";
    private static final String WRONG_CONTENT_TYPE_OUTCOME_BODY =
            "{\"body\":\"{}\",\"declaredContentType\":\"text/plain\"}";

    private ApiNegativeRoutes() { }

    public static ApiRequest invalidJson() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_INVALID_JSON,
                // The negative outcome is modeled as a valid JSON document;
                // the response status/code remains the behavior under test.
                INVALID_JSON_OUTCOME_BODY,
                MediaTypes.APPLICATION_JSON, ApiEnvironment.NO_AUTH_PROFILE);
    }

    public static ApiRequest missingBody() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_MISSING_BODY,
                MISSING_BODY_OUTCOME_BODY,
                MediaTypes.APPLICATION_JSON, ApiEnvironment.NO_AUTH_PROFILE);
    }

    public static ApiRequest wrongContentType() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_WRONG_CONTENT_TYPE,
                WRONG_CONTENT_TYPE_OUTCOME_BODY,
                MediaTypes.TEXT_PLAIN, ApiEnvironment.NO_AUTH_PROFILE);
    }

    public static ApiRequest missingTitle() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_MISSING_TITLE,
                "{\"body\":\"contract\",\"user_id\":1}",
                MediaTypes.APPLICATION_JSON, ApiEnvironment.NO_AUTH_PROFILE);
    }

    public static ApiRequest conflict() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_CONFLICT,
                "{\"title\":\"duplicate\",\"body\":\"contract\",\"user_id\":1}",
                MediaTypes.APPLICATION_JSON, ApiEnvironment.NO_AUTH_PROFILE);
    }

    public static ApiRequest forbiddenUpdate() {
        return raw(HttpMethod.PUT, MockRoutePaths.PROFILE_FORBIDDEN,
                "{\"profile\":{\"locale\":\"ro-RO\"}}", MediaTypes.APPLICATION_JSON,
                ApiEnvironment.NO_AUTH_PROFILE);
    }

    public static ApiRequest unknownAuth() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_UNKNOWN_AUTH,
                "{\"title\":\"auth\",\"body\":\"contract\",\"user_id\":1}",
                MediaTypes.APPLICATION_JSON, "unknown");
    }

    public static ApiRequest serverError() {
        return raw(HttpMethod.POST, MockRoutePaths.POSTS_SERVER_ERROR,
                "{\"title\":\"failure\",\"body\":\"contract\",\"user_id\":1}",
                MediaTypes.APPLICATION_JSON, ApiEnvironment.NO_AUTH_PROFILE);
    }

    private static ApiRequest raw(HttpMethod method, String path, String body,
                                  String contentType, String authProfile) {
        return new ApiRequest(HttpRequest.BodyPublishers.ofString(body), method.value(), path,
                Map.of(), Map.of(HttpHeader.CONTENT_TYPE.value(), contentType), authProfile, null, body);
    }
}

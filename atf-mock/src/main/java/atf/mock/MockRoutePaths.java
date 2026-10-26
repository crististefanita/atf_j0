package atf.mock;

import atf.core.routing.ApiPathTemplate;

import java.util.Map;

/** Concrete route vocabulary owned by the mock boundary, not by core or reporting. */
public final class MockRoutePaths {
    private static final ApiPathTemplate API_V2 = ApiPathTemplate.of("/public/v2");
    public static final String USERS = API_V2.child("users").value();
    public static final String POSTS = API_V2.child("posts").value();
    public static final String PROFILES = API_V2.child("profiles").value();
    public static final String USER_BY_ID = ApiPathTemplate.of(USERS).child("{id}").value();
    public static final String POST_BY_ID = ApiPathTemplate.of(POSTS).child("{id}").value();
    public static final String USER_POSTS = ApiPathTemplate.of(USERS).child("{userId}").child("posts").value();
    public static final String PROFILE_BY_ID = ApiPathTemplate.of(PROFILES).child("{id}").value();
    public static final String CATALOG = API_V2.child("catalog").value();
    public static final String CATALOG_EXPORT = ApiPathTemplate.of(CATALOG).child("export").value();
    public static final String CATALOG_ITEM_42 = ApiPathTemplate.of(CATALOG).child("42").value();
    public static final String SEARCH = API_V2.child("search").value();
    public static final String BULK_IMPORT = API_V2.child("bulk-import").value();
    public static final String PREFERENCES = API_V2.child("preferences").value();
    public static final String POSTS_INVALID_JSON = ApiPathTemplate.of(POSTS).child("invalid-json").value();
    public static final String POSTS_MISSING_BODY = ApiPathTemplate.of(POSTS).child("missing-body").value();
    public static final String POSTS_WRONG_CONTENT_TYPE =
            ApiPathTemplate.of(POSTS).child("wrong-content-type").value();
    public static final String POSTS_MISSING_TITLE = ApiPathTemplate.of(POSTS).child("missing-title").value();
    public static final String POSTS_CONFLICT = ApiPathTemplate.of(POSTS).child("conflict").value();
    public static final String POSTS_UNKNOWN_AUTH = ApiPathTemplate.of(POSTS).child("unknown-auth").value();
    public static final String POSTS_SERVER_ERROR = ApiPathTemplate.of(POSTS).child("server-error").value();
    public static final String PROFILE_FORBIDDEN = ApiPathTemplate.of(PROFILES).child("1").child("forbidden").value();

    private MockRoutePaths() {
    }

    public static String userById(String id) {
        return ApiPathTemplate.of(USER_BY_ID).resolve(Map.of("id", id));
    }

    public static String postById(String id) {
        return ApiPathTemplate.of(POST_BY_ID).resolve(Map.of("id", id));
    }

    public static String userPosts(String userId) {
        return ApiPathTemplate.of(USER_POSTS).resolve(Map.of("userId", userId));
    }

    public static String profileById(String id) {
        return ApiPathTemplate.of(PROFILE_BY_ID).resolve(Map.of("id", id));
    }
}

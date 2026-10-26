package atf.reporting.api.execution.routing;
import atf.core.http.auth.AuthRequirement;
import atf.core.http.auth.AuthType;
import atf.core.http.request.ApiRoute;
import atf.core.http.request.HttpMethod;
import java.util.Set;

public final class ApiRoutes {
    private ApiRoutes() { }

    public static ApiRoute usersCollection() {
        return new ApiRoute("users.collection", HttpMethod.GET.value(), RoutePaths.USERS, "users", "collection", "default",
                AuthType.NONE, AuthRequirement.OPTIONAL, "anonymous or configured", Set.of("api", "users", "contract"));
    }

    public static ApiRoute userItem() {
        return new ApiRoute("users.item", HttpMethod.GET.value(), RoutePaths.USER_BY_ID, "users", "item", "default",
                AuthType.NONE, AuthRequirement.OPTIONAL, "anonymous or configured", Set.of("api", "users", "contract"));
    }

    public static ApiRoute postsCollection() {
        return new ApiRoute("posts.collection", HttpMethod.GET.value(), RoutePaths.POSTS, "posts", "collection", "default",
                AuthType.NONE, AuthRequirement.OPTIONAL, "anonymous or configured", Set.of("api", "posts", "contract"));
    }

    public static ApiRoute postItem() {
        return new ApiRoute("posts.item", HttpMethod.GET.value(), RoutePaths.POST_BY_ID, "posts", "item", "default",
                AuthType.NONE, AuthRequirement.OPTIONAL, "anonymous or configured", Set.of("api", "posts", "contract"));
    }

    public static ApiRoute userPosts() {
        return new ApiRoute("users.posts", HttpMethod.GET.value(), RoutePaths.USER_POSTS, "posts", "relationship", "default",
                AuthType.NONE, AuthRequirement.OPTIONAL, "anonymous or configured", Set.of("api", "users", "posts", "relationship"));
    }

    public static ApiRoute userCreate() {
        return new ApiRoute("users.create", HttpMethod.POST.value(), RoutePaths.USERS, "users", "create", "default",
                AuthType.BEARER, AuthRequirement.REQUIRED, "bearer required", Set.of("api", "users", "write"));
    }

    public static ApiRoute postCreate() {
        return new ApiRoute("posts.create", HttpMethod.POST.value(), RoutePaths.POSTS, "posts", "create", "default",
                AuthType.BEARER, AuthRequirement.REQUIRED, "bearer required", Set.of("api", "posts", "write"));
    }

    public static ApiRoute profileItem() {
        return new ApiRoute("profiles.item", HttpMethod.GET.value(), RoutePaths.PROFILE_BY_ID, "profiles", "item", "basic",
                AuthType.BASIC, AuthRequirement.REQUIRED, "basic required", Set.of("api", "profiles", "contract"));
    }

    public static ApiRoute profileUpdate() {
        return new ApiRoute("profiles.update", HttpMethod.PUT.value(), RoutePaths.PROFILE_BY_ID, "profiles", "update", "basic",
                AuthType.BASIC, AuthRequirement.REQUIRED, "basic required", Set.of("api", "profiles", "write"));
    }
}

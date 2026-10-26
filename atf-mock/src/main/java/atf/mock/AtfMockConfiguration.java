package atf.mock;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable route catalog and fallback policy for one mock execution. */
public record AtfMockConfiguration(List<MockRoute> routes, MockRoute fallback) {
    public AtfMockConfiguration {
        routes = routes == null ? List.of() : List.copyOf(routes);
        fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public Optional<MockRoute> routeFor(ApiRequest request) {
        return routes.stream()
                .filter(route -> route.matches(request))
                .sorted(java.util.Comparator.comparingInt(
                        (MockRoute route) -> route.queryParameters().values().size()).reversed())
                .findFirst();
    }

    /** Resolves the method/path identity rendered in an API report. */
    public Optional<MockRoute> routeFor(String method, String path) {
        if (method == null || method.isBlank() || path == null || path.isBlank()) {
            return Optional.empty();
        }
        String normalizedMethod = method.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedPath = path.trim().startsWith("/") ? path.trim() : "/" + path.trim();
        return routes.stream()
                .filter(route -> (route.method().equals(MockRoute.ANY_METHOD)
                        || route.method().equals(normalizedMethod))
                        && (route.path().equals(MockRoute.ANY_PATH)
                        || route.path().equals(normalizedPath)))
                .findFirst();
    }

    /** Built-in compatibility fixture used when no mock YAML is supplied. */
    public static AtfMockConfiguration defaults() {
        Map<String, String> json = Map.of(HttpHeader.CONTENT_TYPE.value(), MediaTypes.APPLICATION_JSON);
        Duration duration = Duration.ofMillis(12);
        return new AtfMockConfiguration(List.of(
                new MockRoute(HttpMethod.GET.value(), MockRoutePaths.USERS, 200,
                        "[{\"id\":1,\"name\":\"ATF User\",\"email\":\"atf@example.test\"}]", json, duration),
                new MockRoute(HttpMethod.GET.value(), MockRoutePaths.userById("1"), 200,
                        "{\"id\":1,\"name\":\"ATF User\",\"email\":\"atf@example.test\"}", json, duration),
                new MockRoute(HttpMethod.POST.value(), MockRoutePaths.USERS, 401,
                        "{\"message\":\"Authentication required\"}", json, duration),
                new MockRoute(HttpMethod.POST.value(), MockRoutePaths.POSTS, 201,
                        "{\"id\":102,\"user_id\":1,\"title\":\"ATF created post\",\"body\":\"contract\"}",
                        json, duration),
                new MockRoute(HttpMethod.PUT.value(), MockRoutePaths.profileById("1"), 200,
                        "{\"id\":1,\"updated\":true,\"profile\":{\"locale\":\"ro-RO\"},\"permissions\":{\"inherited\":[]},\"changes\":[]}",
                        json, duration),
                new MockRoute(HttpMethod.GET.value(), MockRoutePaths.POSTS, 200,
                        "[{\"id\":101,\"user_id\":1,\"title\":\"ATF post\",\"body\":\"contract\"}]", json, duration),
                new MockRoute(HttpMethod.GET.value(), MockRoutePaths.postById("101"), 200,
                        "{\"id\":101,\"user_id\":1,\"title\":\"ATF post\",\"body\":\"contract\"}", json, duration),
                new MockRoute(HttpMethod.GET.value(), MockRoutePaths.userPosts("1"), 200,
                        "[{\"id\":101,\"user_id\":1,\"title\":\"ATF post\"}]", json, duration),
                new MockRoute(HttpMethod.GET.value(), MockRoutePaths.postById("999999"), 404,
                        "{\"message\":\"Not found\"}", json, duration)),
                new MockRoute(MockRoute.ANY_METHOD, MockRoute.ANY_PATH, 500,
                        "{\"message\":\"Unexpected mock route\"}", json, duration));
    }
}

package atf.mock;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Renderer-neutral deterministic transport for offline ATF acceptance tests. */
public final class AtfMockTransport implements ApiTransport {
    private final AtfMockConfiguration configuration;

    public AtfMockTransport(AtfMockConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public static AtfMockTransport defaults() {
        return new AtfMockTransport(AtfMockConfiguration.defaults());
    }

    public static AtfMockTransport fromProjectConfiguration() {
        return new AtfMockTransport(AtfMockConfigurationLoader.load());
    }

    public static AtfMockTransport fromConfiguration(java.nio.file.Path path) {
        return new AtfMockTransport(AtfMockConfigurationLoader.load(path));
    }

    public AtfMockConfiguration configuration() {
        return configuration;
    }

    @Override
    public CompletableFuture<ApiResponse> send(ApiRequest request, String url, String authProfile) {
        MockRoute route = configuration.routeFor(request).orElse(configuration.fallback());
        return CompletableFuture.completedFuture(route.response(url));
    }
}

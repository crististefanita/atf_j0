package atf.core;
import atf.testkit.AtfUnitTest;

import atf.core.http.auth.AuthProfile;
import atf.core.http.auth.AuthType;
import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.http.transport.RetryPolicy;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class AuthenticationAndRetryTest {
    @Test
    void typedAuthenticatorSupportsAllDeclaredProfilesWithoutExposingSecretsInDescriptions() {
        var auth = new ConfiguredApiAuthenticator(Map.of(
                "bearer", new AuthProfile("bearer", AuthType.BEARER, "secret", "", "", "", Map.of()),
                "key", new AuthProfile("key", AuthType.API_KEY, "value", "", "", "X-Api-Key", Map.of()),
                "basic", new AuthProfile("basic", AuthType.BASIC, "", "user", "pass", "", Map.of()),
                "custom", new AuthProfile("custom", AuthType.CUSTOM, "", "", "", "", Map.of("X-Trace", "demo"))));

        var bearer = HttpRequest.newBuilder(URI.create("https://example.test"));
        auth.apply(bearer, "bearer");
        assertEquals("Bearer secret", bearer.build().headers().firstValue("Authorization").orElseThrow());
        assertEquals("configured", auth.describeTokenState("bearer"));
        assertEquals("api_key", auth.describeMode("key"));
        assertEquals("Basic dXNlcjpwYXNz", basicHeader(auth));
        var key = HttpRequest.newBuilder(URI.create("https://example.test"));
        auth.apply(key, "key");
        assertEquals("value", key.build().headers().firstValue("X-Api-Key").orElseThrow());
        var custom = HttpRequest.newBuilder(URI.create("https://example.test"));
        auth.apply(custom, "custom");
        assertEquals("demo", custom.build().headers().firstValue("X-Trace").orElseThrow());
    }

    private static String basicHeader(ConfiguredApiAuthenticator auth) {
        var builder = HttpRequest.newBuilder(URI.create("https://example.test"));
        auth.apply(builder, "basic");
        return builder.build().headers().firstValue("Authorization").orElseThrow();
    }

    @Test
    void executorPreservesEveryRetryAttempt() {
        AtomicInteger calls = new AtomicInteger();
        ApiTransport transport = (request, url, auth) -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(new ApiResponse(429, "busy", Map.of("Retry-After", "0"), Duration.ZERO, url));
        };
        var executor = new ApiExecutor(transport, URI.create("https://example.test"),
                RetryPolicy.onStatuses(Set.of(429), 3, Duration.ZERO));

        var result = executor.execute(ApiRequest.of("GET", "/users"));

        assertEquals(3, calls.get());
        assertEquals(3, result.attempts());
        assertEquals(2, result.retryEvents().size());
        assertEquals(429, result.retryEvents().getFirst().statusCode());
    }

    @Test
    void retryAfterHeaderBecomesTheEffectiveDelay() {
        var response = new ApiResponse(429, "busy", Map.of("Retry-After", "3"), Duration.ZERO,
                "https://example.test/users");
        var event = RetryPolicy.onStatuses(Set.of(429), 2, Duration.ofMillis(1))
                .next(response, 1).orElseThrow();

        assertEquals(Duration.ofSeconds(3), event.delay());
        assertEquals("3", event.header());
    }
}

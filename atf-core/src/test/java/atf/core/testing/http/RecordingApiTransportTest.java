package atf.core.testing.http;
import atf.testkit.AtfUnitTest;


import atf.core.routing.ApiPathTemplate;
import atf.core.http.request.ApiRoute;
import atf.core.http.request.HttpMethod;
import atf.core.config.source.ApiEnvironment;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class RecordingApiTransportTest {
    private static final String CREATE_POST_PATH = ApiPathTemplate.of("/posts").value();

    @Test
    void capturesSemanticRequestAndLeavesDelegateExecutionUntouched() {
        ApiTransport delegate = (request, url, authProfile) -> CompletableFuture.completedFuture(
                new ApiResponse(200, "{}", Map.of(), Duration.ZERO, url));
        var recording = new RecordingApiTransport(delegate);
        var route = new ApiRoute("create post", HttpMethod.POST.value(), CREATE_POST_PATH,
                "posts", "create", "none");
        var request = route.requestBody("{\"title\":\"ATF\"}", Map.of());

        var response = recording.send(request, ApiEnvironment.MOCK_BASE_URI.resolve(CREATE_POST_PATH).toString(), "none").join();

        assertSame(request, recording.lastRequest());
        assertTrue(response.status() >= 200 && response.status() < 300);
    }
}

package atf.reporting.api.execution;

import atf.core.http.auth.AuthRequirement;
import atf.core.http.auth.AuthType;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.reporting.api.execution.model.UserRequest;
import atf.reporting.api.execution.routing.ApiRoutes;
import atf.reporting.api.execution.service.UsersService;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest

class WriteRouteContractTest {
    @Test
    void semanticCreateUsesTypedRouteMetadataAndJsonBody() {
        var seen = new ApiResponse[1];
        ApiTransport transport = (request, url, auth) -> {
            seen[0] = new ApiResponse(201, "{\"id\":7}", Map.of(), Duration.ZERO, url);
            assertEquals("POST", request.method());
            assertEquals("default", auth);
            assertTrue(request.body().contentLength() > 0);
            return CompletableFuture.completedFuture(seen[0]);
        };

        var result = new UsersService(new ApiExecutor(transport, URI.create("https://example.test")))
                .create(new UserRequest("ATF", "atf@example.test", "male", "active"));

        assertEquals(201, result.response().status());
        var route = ApiRoutes.userCreate();
        assertEquals(AuthRequirement.REQUIRED, route.authRequirement());
        assertEquals(AuthType.BEARER, route.authType());
        assertTrue(route.tags().contains("write"));
    }
}

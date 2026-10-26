package atf.core.http.transport;
import atf.core.http.request.ApiRequest;
import atf.core.http.response.ApiResponse;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface ApiTransport {
    CompletableFuture<ApiResponse> send(ApiRequest request, String url, String authProfile);
}

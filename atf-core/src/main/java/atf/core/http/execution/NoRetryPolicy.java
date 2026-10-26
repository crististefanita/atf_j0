package atf.core.http.execution;
import atf.core.http.response.ApiResponse;

public final class NoRetryPolicy {
    public boolean shouldRetry(ApiResponse response, int attempt) {
        return false;
    }
}

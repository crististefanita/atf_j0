package atf.reporting.api.tests.api.flows;
import atf.testkit.AtfUnitTest;
import atf.core.config.source.ApiEnvironment;

import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ApiFlowAssertionsTest {
    @Test
    void rejectsBrokenSelfLinkAfterCollectionCorrelation() {
        var created = result("{\"id\":102,\"status\":\"published\"}");
        var collection = result("[{\"id\":102,\"status\":\"published\"}]");
        var brokenRead = result("{\"id\":102,\"status\":\"published\",\"links\":{\"self\":\"/wrong/102\"}}");

        assertThrows(AssertionError.class,
                () -> ApiFlowAssertions.postReadBack(created, collection, brokenRead, null));
    }

    @Test
    void rejectsStaleVersionFromReadModel() {
        var updated = result("{\"id\":1,\"version\":7}");
        var staleRead = result("{\"id\":1,\"version\":6,\"audit\":{\"lastMutation\":\"profile-update\",\"lastChangedFields\":[\"locale\"]}}");

        assertThrows(AssertionError.class, () -> ApiFlowAssertions.profileReadBack(updated, staleRead, null));
    }

    private static ApiExecutionResult result(String body) {
        return new ApiExecutionResult(new ApiResponse(200, body, Map.of(), Duration.ZERO,
                        ApiEnvironment.MOCK_BASE_URI.toString()),
                1, Duration.ZERO, List.of());
    }
}

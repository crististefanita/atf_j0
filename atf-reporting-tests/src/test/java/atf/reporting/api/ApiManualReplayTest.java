package atf.reporting.api;
import atf.testkit.AtfIntegrationTest;

import atf.core.http.request.ApiRoute;
import atf.core.http.transport.ApiTransportProvenance;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;
import atf.reporting.support.ApiReportTestUrls;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfIntegrationTest
class ApiManualReplayTest {
    @Test
    void acceptanceReplayNeverPretendsThatTheSyntheticUrlWasContacted() {
        var replay = ApiManualReplay.describe(ApiTransportProvenance.ACCEPTANCE_MOCK, null,
                ApiReportTestUrls.atf(MockRoutePaths.userById("1")), "null");

        assertAll(
                () -> assertTrue(replay.contains("NOT EXECUTABLE")),
                () -> assertTrue(replay.contains("ACCEPTANCE / MOCK")),
                () -> assertTrue(replay.contains("network was not executed")),
                () -> assertFalse(replay.contains("gorest.co.in")));
    }

    @Test
    void liveReplayContainsUrlHeadersAndJsonWithoutPublishingSecrets() {
        var request = new ApiRoute("create-user", HttpMethod.POST.value(), MockRoutePaths.USERS,
                "users", "create", "bearer")
                .requestBody("{\"name\":\"ATF\",\"token\":\"secret\"}", Map.of(),
                        Map.of(), Map.of("X-Client-Version", "2.4",
                                "Authorization", "Bearer secret"));

        String replay = ApiManualReplay.describe(ApiTransportProvenance.LIVE_HTTP, request,
                ApiReportTestUrls.apiExample(MockRoutePaths.USERS), request.bodyText());

        assertAll(
                () -> assertTrue(replay.startsWith("curl --request 'POST' --url '"
                        + ApiReportTestUrls.apiExample(MockRoutePaths.USERS) + "'")),
                () -> assertTrue(replay.contains("Accept: application/json")),
                () -> assertTrue(replay.contains("X-Client-Version: 2.4")),
                () -> assertTrue(replay.contains("--data-raw")),
                () -> assertTrue(replay.contains("***REDACTED***")),
                () -> assertTrue(replay.contains("auth profile 'bearer'")),
                () -> assertFalse(replay.contains("Bearer secret")));
    }
}

package atf.mock;
import atf.testkit.AtfComponentTest;

import atf.core.http.request.HttpMethod;
import atf.mock.fixtures.MockTransportTestFixtures;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest

class MockRouteIdentityTest {
    @Test
    void routeKeyIncludesSortedQueryIdentityForSameMethodAndPathVariants() {
        var first = new MockRoute(HttpMethod.GET.value(), MockTransportTestFixtures.SEARCH_PATH, 200,
                MockTransportTestFixtures.EMPTY_JSON_BODY, Map.of(), Duration.ZERO,
                MockTransportTestFixtures.BEARER_PROFILE,
                Map.of(MockTransportTestFixtures.PAGE_QUERY, MockTransportTestFixtures.SECOND_PAGE_VALUE,
                        MockTransportTestFixtures.INCLUDE_QUERY,
                        MockTransportTestFixtures.DETAILS_QUERY_VALUE));
        var second = new MockRoute(HttpMethod.GET.value(), MockTransportTestFixtures.SEARCH_PATH, 400,
                MockTransportTestFixtures.EMPTY_JSON_BODY, Map.of(), Duration.ZERO,
                MockTransportTestFixtures.BEARER_PROFILE,
                Map.of(MockTransportTestFixtures.INCLUDE_QUERY,
                        MockTransportTestFixtures.DETAILS_QUERY_VALUE,
                        MockTransportTestFixtures.PAGE_QUERY,
                        MockTransportTestFixtures.INVALID_PAGE_VALUE));

        assertEquals("%s %s?%s=%s&%s=%s".formatted(
                HttpMethod.GET.value(), MockTransportTestFixtures.SEARCH_PATH,
                MockTransportTestFixtures.INCLUDE_QUERY, MockTransportTestFixtures.DETAILS_QUERY_VALUE,
                MockTransportTestFixtures.PAGE_QUERY, MockTransportTestFixtures.SECOND_PAGE_VALUE), first.key());
        assertEquals("%s %s?%s=%s&%s=%s".formatted(
                HttpMethod.GET.value(), MockTransportTestFixtures.SEARCH_PATH,
                MockTransportTestFixtures.INCLUDE_QUERY, MockTransportTestFixtures.DETAILS_QUERY_VALUE,
                MockTransportTestFixtures.PAGE_QUERY, MockTransportTestFixtures.INVALID_PAGE_VALUE), second.key());
    }
}

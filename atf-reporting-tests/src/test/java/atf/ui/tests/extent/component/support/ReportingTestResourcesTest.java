package atf.ui.tests.extent.component.support;
import atf.testkit.AtfComponentTest;

import atf.core.http.request.HttpMethod;
import atf.mock.MockRoute;
import atf.mock.MockRequestBodyMode;
import atf.ui.tests.extent.support.config.ReportingTestResources;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps reporting tests independent from the project-root fixture discovery policy. */
@AtfComponentTest
class ReportingTestResourcesTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void reportingOwnsTheUiAndMockResources() {
        assertTrue(ReportingTestResources.resourcePath(ReportingTestResources.UI_RESOURCE)
                .toString().endsWith(ReportingTestResources.UI_RESOURCE));
        assertTrue(ReportingTestResources.resourcePath(ReportingTestResources.MOCK_RESOURCE)
                .toString().endsWith(ReportingTestResources.MOCK_RESOURCE));

        var configuration = ReportingTestResources.mockConfiguration();
        assertFalse(configuration.routes().isEmpty());
        configuration.routes().stream()
                .filter(ReportingTestResourcesTest::isJsonWrite)
                .forEach(ReportingTestResourcesTest::assertJsonContract);
    }

    private static boolean isJsonWrite(MockRoute route) {
        return HttpMethod.POST.value().equals(route.method())
                || HttpMethod.PUT.value().equals(route.method());
    }

    private static void assertJsonContract(MockRoute route) {
        assertDoesNotThrow(() -> {
            MockRequestBodyMode.from(route.requestBodyMode());
            assertNotNull(route.requestBodyMode());
            assertNotNull(JSON.readTree(route.body()));
        }, () -> "Invalid JSON contract for " + route.method() + " " + route.path());
    }
}

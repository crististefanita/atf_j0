package atf.shop.tests.api.component.reporting;

import atf.api.core.http.protocol.ApiUris;
import atf.api.core.http.protocol.HttpHeaderProfiles;
import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.request.ApiRequest;
import atf.api.core.http.response.ApiExecutionResult;
import atf.api.core.http.response.ApiResponse;
import atf.api.core.http.transport.ApiTransportProvenance;
import atf.api.core.reporting.console.AtfConsoleSink;
import atf.api.testkit.AtfComponentTest;
import atf.shop.support.reporting.ShopApiReportPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopApiBodyRedactionReportTest {
    @Test
    void publishedHtmlDoesNotExposePersonalValuesFromValidOrMalformedBodies(@TempDir Path temp)
            throws Exception {
        List<ApiExecutionResult> executions = List.of(
                execution("/users/register",
                        "{\"first_name\":\"PrivateFirstName731\",\"email\":\"ada.private@example.test\","
                                + "\"address\":{\"street\":\"Secret Road\",\"postal_code\":\"10101\"}}",
                        "{\"first_name\":\"PrivateFirstName731\",\"email\":\"ada.private@example.test\","
                                + "\"phone\":\"0700000000\"}"),
                execution("/users/register",
                        "{\"email\":\"malformed.private@example.test\",\"address\":\"Hidden Lane\",",
                        "{\"message\":\"response.private@example.test\"}"));
        Path report = ShopApiReportPublisher.publishStandalone(
                temp.resolve("practice-shop-redaction.html"),
                getClass().getSimpleName(), getClass().getPackageName(),
                "field-aware body redaction", List.of("component"), executions, AtfConsoleSink.noOp());

        String html = Files.readString(report);

        assertTrue(Files.isRegularFile(report));
        for (String privateValue : List.of("PrivateFirstName731", "ada.private@example.test", "Secret Road", "10101",
                "0700000000", "header.private@example.test", "malformed.private@example.test", "Hidden Lane",
                "response.private@example.test")) {
            assertFalse(html.contains(privateValue), "published HTML leaked: " + privateValue);
        }
        assertTrue(html.contains("body omitted"));
        assertTrue(html.contains("<redacted>") || html.contains("&lt;redacted&gt;"));
    }

    private static ApiExecutionResult execution(String path, String requestBody, String responseBody) {
        ApiRequest request = new ApiRequest(HttpRequest.BodyPublishers.ofString(requestBody),
                HttpMethod.POST.value(), path, Map.of(), Map.of("X-User-Email", "header.private@example.test"),
                "none", null, requestBody);
        ApiResponse response = new ApiResponse(201, responseBody,
                HttpHeaderProfiles.JSON_CONTENT, Duration.ZERO,
                ApiUris.httpsUrl("api.practicesoftwaretesting.com", path));
        return new ApiExecutionResult(response, 1, Duration.ZERO, List.of(), List.of(), request,
                ApiTransportProvenance.LIVE_HTTP);
    }
}

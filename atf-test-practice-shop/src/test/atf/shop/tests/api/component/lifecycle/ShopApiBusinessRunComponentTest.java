package atf.shop.tests.api.component.lifecycle;

import atf.core.config.ApiTransportMode;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransport;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.extent.session.ExtentReportSession;
import atf.shop.api.ShopApiClient;
import atf.shop.api.ShopApiRoutes;
import atf.shop.api.contract.ShopPaymentMethod;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.support.api.run.ShopApiBusinessRun;
import atf.shop.support.api.run.ShopApiRunReport;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import atf.shop.tests.support.ShopComponentTest;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopApiBusinessRunComponentTest {
    private enum Case { SUCCESS, LOGIN_TIMEOUT, LOGIN_REJECTED, LOGIN_ABSENT, CATALOG_INVALID, CART_TIMEOUT, BUSINESS_FAIL, CLEANUP_FAILURE }

    @Test void publishesEveryFailureExitAndTheSuccessfulBusinessArtifact() throws Exception {
        for (Case selected : Case.values()) {
            var report = output(selected);
            var client = ShopApiClient.forTransport(ShopApiScenarioFixture.API_ENDPOINT, transport(selected),
                    ApiTransportMode.ACCEPTANCE, AtfConsoleSink.noOp());
            var run = new ShopApiBusinessRun(() -> client,
                    () -> new ShopApiRunReport(new ExtentReportSession(report), "ShopApiRunFixture", "fixture",
                            selected.name(), List.of("api", "component"), false, AtfConsoleSink.noOp()), false);
            boolean throwsFailure = List.of(Case.LOGIN_TIMEOUT, Case.CATALOG_INVALID,
                    Case.CART_TIMEOUT, Case.CLEANUP_FAILURE).contains(selected);
            if (throwsFailure) {
                assertThrows(RuntimeException.class, () -> run.execute(inputs(), ShopApiScenarioFixture::validCredentials));
            } else {
                assertEquals(selected == Case.SUCCESS,
                        run.execute(inputs(), ShopApiScenarioFixture::validCredentials).passed(), selected.name());
            }
            assertTrue(Files.isRegularFile(report), selected + " must retain its artifact");
            String html = Files.readString(report);
            assertTrue(html.contains(selected.name()), selected.name());
            assertFalse(html.contains("\"password\":\"secret\""));
            if (selected == Case.LOGIN_TIMEOUT || selected == Case.CART_TIMEOUT) {
                assertTrue(html.contains("No HTTP response received"), selected.name());
                assertTrue(html.contains("Failed API execution"), selected.name());
                assertTrue(html.contains(selected == Case.LOGIN_TIMEOUT
                        ? "controlled login timeout" : "controlled cart timeout"), "root transport cause must remain visible");
            }
            if (selected != Case.LOGIN_TIMEOUT) assertTrue(html.contains("Authentication business contract"));
            if (selected == Case.LOGIN_ABSENT) {
                assertTrue(html.contains("Empty response body"));
                assertFalse(html.contains("body unavailable to Playwright transport"));
            }
            if (selected == Case.CATALOG_INVALID) assertTrue(html.contains("Catalog search did not return product"));
            if (selected == Case.SUCCESS) {
                for (String title : List.of("Catalog business contract", "Live category and price-sort contract",
                        "Cart add business contract", "Live cart mutation and cleanup contract", "Checkout business contract"))
                    assertTrue(html.contains(title), title);
            }
            assertTrue(client.bearerToken().isEmpty(), "client must close even after execution failure");
        }
    }

    @Test void publicationFailureIsSuppressedUnderTheOriginalBusinessFailure() {
        var businessFailure = new IllegalStateException("controlled client acquisition failure");
        var publicationFailure = new IllegalStateException("controlled publication failure");
        var closes = new AtomicInteger();
        ShopApiBusinessRun.Audit audit = new ShopApiBusinessRun.Audit() {
            public void observed(atf.core.http.response.ApiExecutionResult execution) { }
            public void requestFailed(atf.core.http.request.ApiRequest request, Throwable failure) { }
            public void validation(atf.shop.support.api.ShopApiReportValidation validation) { }
            public void failed(Throwable failure) { assertSame(businessFailure, failure); }
            public void close() { closes.incrementAndGet(); throw publicationFailure; }
        };
        var run = new ShopApiBusinessRun(() -> { throw businessFailure; }, () -> audit, false);
        var actual = assertThrows(IllegalStateException.class,
                () -> run.execute(inputs(), ShopApiScenarioFixture::validCredentials));
        assertSame(businessFailure, actual);
        assertEquals(List.of(publicationFailure), List.of(actual.getSuppressed()));
        assertEquals(1, closes.get());
    }

    private static ShopApiBusinessRun.Inputs inputs() {
        return new ShopApiBusinessRun.Inputs(ShopApiScenarioFixture.PRODUCT_NAME, ShopApiScenarioFixture.QUANTITY,
                ShopConfigurationLoader.load().checkout().billing(), ShopPaymentMethod.CASH_ON_DELIVERY);
    }
    private static Path output(Case selected) throws Exception {
        var directory = ShopConfigurationLoader.load().apiReportDirectory().resolve("run-lifecycle");
        Files.createDirectories(directory);
        return directory.resolve(selected.name().toLowerCase() + "-" + java.util.UUID.randomUUID() + ".html");
    }

    private static ApiTransport transport(Case selected) {
        var ordinary = ShopApiComponentFixture.transport();
        var carts = ShopApiComponentFixture.statefulCartTransport();
        var cartReads = new AtomicInteger();
        return (request, url, auth) -> {
            String path = request.path();
            if (path.equals(ShopApiRoutes.LOGIN)) {
                if (selected == Case.LOGIN_TIMEOUT) return CompletableFuture.failedFuture(new IllegalStateException("controlled login timeout"));
                if (selected == Case.LOGIN_REJECTED) return json(401, "{\"message\":\"Unauthorized\"}", url);
                if (selected == Case.LOGIN_ABSENT) return json(401, "", url);
            }
            if (path.equals(ShopApiRoutes.PRODUCT_SEARCH) && selected == Case.CATALOG_INVALID)
                return json(200, "{\"data\":[]}", url);
            String product = "{\"id\":\"" + ShopApiScenarioFixture.PRODUCT_ID + "\",\"name\":\""
                    + ShopApiScenarioFixture.PRODUCT_NAME + "\",\"price\":12.01,\"category\":{\"id\":\"category-1\"}}";
            if (path.equals(ShopApiRoutes.product(ShopApiScenarioFixture.PRODUCT_ID))) return json(200, product, url);
            if (path.equals(ShopApiRoutes.PRODUCT_COLLECTION)) return json(200,
                    selected == Case.BUSINESS_FAIL ? "{\"data\":[]}" : "{\"data\":[" + product + "]}", url);
            if (path.equals(ShopApiRoutes.CARTS) && selected == Case.CART_TIMEOUT)
                return CompletableFuture.failedFuture(new IllegalStateException("controlled cart timeout"));
            if (path.startsWith(ShopApiRoutes.CARTS)) {
                if (request.method().equals("GET") && selected == Case.CLEANUP_FAILURE && cartReads.incrementAndGet() == 4)
                    return CompletableFuture.failedFuture(new IllegalStateException("controlled cleanup timeout"));
                return carts.send(request, url, auth);
            }
            return ordinary.send(request, url, auth);
        };
    }
    private static CompletableFuture<ApiResponse> json(int status, String body, String url) {
        return CompletableFuture.completedFuture(new ApiResponse(status, body,
                Map.of("Content-Type", "application/json"), Duration.ZERO, url));
    }
}

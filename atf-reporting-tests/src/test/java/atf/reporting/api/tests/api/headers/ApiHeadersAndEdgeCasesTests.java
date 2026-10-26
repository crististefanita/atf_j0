package atf.reporting.api.tests.api.headers;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.AtfConsoleMessages;

import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.console.junit.AtfConsoleTestExtension;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.api.tests.support.junit.ApiTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deep offline API cases for sent headers, response headers and edge statuses. */
@ExtendWith(AtfConsoleTestExtension.class)
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfModuleTest(requires = {AtfModule.API, AtfModule.REPORTING})
@AtfIntegrationTest
class ApiHeadersAndEdgeCasesTests {
    private ApiTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ApiTestFixture();
    }

    @Test
    void preservesSentCorrelationAndAcceptHeadersAndValidatesPaginationHeaders() {
        var request = ApiHeaderRoutes.catalogPageRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, ApiHeaderRoutes.catalogPageContract());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertEquals(MediaTypes.APPLICATION_JSON, request.headers().get(HttpHeader.ACCEPT.value())),
                () -> assertEquals("corr-headers-001", request.headers().get(HttpHeader.X_CORRELATION_ID.value())),
                () -> assertEquals("req-catalog-003", execution.response().header(HttpHeader.X_REQUEST_ID.value())),
                () -> assertEquals("97", execution.response().header(HttpHeader.X_RATE_LIMIT_REMAINING.value())),
                () -> assertEquals("W/catalog-3", execution.response().header(HttpHeader.ETAG.value())),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void preservesRateLimitResponseAsAnExplicitContractOutcome() {
        var request = ApiHeaderRoutes.exportRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, ApiHeaderRoutes.exportContract());

        assertAll(
                () -> assertEquals(429, execution.response().status()),
                () -> assertEquals(MediaTypes.TEXT_CSV, request.headers().get(HttpHeader.ACCEPT.value())),
                () -> assertEquals("2.4", request.headers().get(HttpHeader.X_CLIENT_VERSION.value())),
                () -> assertEquals("2", execution.response().header(HttpHeader.RETRY_AFTER.value())),
                () -> assertEquals("0", execution.response().header(HttpHeader.X_RATE_LIMIT_REMAINING.value())),
                () -> assertTrue(execution.response().body().contains("RATE_LIMITED")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void preservesConditionalRequestHeadersAndNotModifiedResponse() {
        var request = ApiHeaderRoutes.notModifiedRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, ApiHeaderRoutes.notModifiedContract());

        assertAll(
                () -> assertEquals(304, execution.response().status()),
                () -> assertEquals("W/catalog-42", request.headers().get(HttpHeader.IF_NONE_MATCH.value())),
                () -> assertEquals("W/catalog-42", execution.response().header(HttpHeader.ETAG.value())),
                () -> assertEquals("max-age=60", execution.response().header(HttpHeader.CACHE_CONTROL.value())),
                () -> assertTrue(execution.response().body().isBlank()),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }
}

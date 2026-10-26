package atf.reporting.api.tests.api.requests;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.AtfConsoleMessages;

import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.core.http.auth.AuthType;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.reporting.api.tests.support.junit.ApiTestFixture;
import atf.console.junit.AtfConsoleTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Focused request-shape tests: query parameters, large JSON and auth profiles. */
@ExtendWith(AtfConsoleTestExtension.class)
@AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS)
@AtfModuleTest(requires = {AtfModule.API, AtfModule.REPORTING})
@AtfIntegrationTest
class RequestVariantsApiTests {
    private ApiTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ApiTestFixture();
    }

    @Test
    void sendsQueryParametersWithBearerProfileAndValidatesNestedResponse() {
        var request = RequestVariantRoutes.searchRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.searchContract());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("page=2")),
                () -> assertTrue(execution.response().url().contains("include=details")),
                () -> assertEquals(MediaTypes.APPLICATION_JSON, request.headers().get(HttpHeader.ACCEPT.value())),
                () -> assertEquals("bearer", request.authProfile()),
                () -> assertEquals(AuthType.BEARER, request.route().authType()),
                () -> assertEquals("complex-search", execution.response().header(HttpHeader.X_MOCK_SCENARIO.value())),
                () -> assertTrue(execution.response().body().contains("history")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void rejectsAnInvalidQueryWithADeepJsonErrorContract() {
        var request = RequestVariantRoutes.invalidSearchQueryRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.invalidSearchQueryContract());

        assertAll(
                () -> assertEquals(400, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("page=invalid")),
                () -> assertEquals("bearer", request.authProfile()),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void validatesMissingQueryParameterAsASeparateMockContract() {
        var request = RequestVariantRoutes.missingPageSearchRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.missingPageSearchContract());

        assertAll(
                () -> assertEquals(422, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("include=details")),
                () -> assertTrue(!execution.response().url().contains("page=")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void validatesAnExplicitEmptyQueryParameterWithoutCollapsingItToMissing() {
        var request = RequestVariantRoutes.emptyPageSearchRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.emptyPageSearchContract());

        assertAll(
                () -> assertEquals(422, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("page=")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void preservesDuplicateQueryOccurrencesThroughUrlAndMockMatching() {
        var request = RequestVariantRoutes.duplicateTagSearchRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.duplicateTagSearchContract());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("tag=owner&tag=quality")),
                () -> assertEquals(2, request.queryParameters().values().size()),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void sendsLargeNestedBodyWithApiKeyProfileAndValidatesAcceptedJob() {
        var request = RequestVariantRoutes.bulkImportRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.bulkImportContract());

        assertAll(
                () -> assertEquals(202, execution.response().status()),
                () -> assertEquals("api-key", request.authProfile()),
                () -> assertEquals(AuthType.API_KEY, request.route().authType()),
                () -> assertEquals(MediaTypes.APPLICATION_JSON, request.headers().get(HttpHeader.CONTENT_TYPE.value())),
                () -> assertEquals("bulk-import", execution.response().header(HttpHeader.X_MOCK_SCENARIO.value())),
                () -> assertTrue(request.bodyText().length() > 700),
                () -> assertTrue(request.bodyText().contains("records")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void sendsNestedUpdateWithBasicProfileAndQueryParameter() {
        var request = RequestVariantRoutes.profileUpdateRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.profileUpdateContract());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("expand=permissions")),
                () -> assertEquals("basic", request.authProfile()),
                () -> assertEquals(AuthType.BASIC, request.route().authType()),
                () -> assertEquals("profile-update", execution.response().header(HttpHeader.X_MOCK_SCENARIO.value())),
                () -> assertTrue(execution.response().body().contains("inherited")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void sendsCustomAuthProfileWithParameterRichBody() {
        var request = RequestVariantRoutes.preferencesRequest();
        var execution = fixture.api().executor().execute(request);
        var decision = fixture.validate(execution, RequestVariantRoutes.preferencesContract());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().url().contains("locale=ro-RO")),
                () -> assertEquals("custom", request.authProfile()),
                () -> assertEquals(AuthType.CUSTOM, request.route().authType()),
                () -> assertEquals("custom-preferences", execution.response().header(HttpHeader.X_MOCK_SCENARIO.value())),
                () -> assertTrue(request.bodyText().contains("notifications")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }
}

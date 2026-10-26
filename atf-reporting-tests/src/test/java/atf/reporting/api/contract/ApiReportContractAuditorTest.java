package atf.reporting.api.contract;
import atf.testkit.AtfComponentTest;

import atf.reporting.support.MockApiReportContractFixtures.ViolationSignal;
import atf.mock.MockRoute;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static atf.reporting.support.MockApiReportContractFixtures.JSON;
import static atf.reporting.support.MockApiReportContractFixtures.PLAIN_TEXT;

import static atf.reporting.support.MockApiReportContractFixtures.OK_BODY;
import static atf.reporting.support.MockApiReportContractFixtures.REQUEST_BODY;
import static atf.reporting.support.MockApiReportContractFixtures.acceptedInput;
import static atf.reporting.support.MockApiReportContractFixtures.bodyDriftInput;
import static atf.reporting.support.MockApiReportContractFixtures.putInput;
import static atf.reporting.support.MockApiReportContractFixtures.responseTreeInput;
import static atf.reporting.support.MockApiReportContractFixtures.responseTreePostRoute;
import static atf.reporting.support.MockApiReportContractFixtures.transportDriftInput;
import static atf.reporting.support.MockApiReportContractFixtures.transportGetRoute;
import static atf.reporting.support.MockApiReportContractFixtures.acceptedPostRoute;
import static atf.reporting.support.MockApiReportContractFixtures.bodyDriftGetRoute;
import static atf.reporting.support.MockApiReportContractFixtures.putRoute;
import static atf.reporting.support.MockApiReportContractFixtures.unresolvedInput;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the reporting module directly against the mock catalog port. */
@AtfComponentTest
class ApiReportContractAuditorTest {
    @Test
    void acceptsCanonicalAndLegacyContentTypeFieldsWithoutHidingContradictions() {
        var input = acceptedInput();
        var auditor = new ApiReportContractAuditor(catalog(acceptedPostRoute()));
        for (String field : List.of(ApiReportEvidenceField.CONTENT_TYPE.key(),
                "CONTENT-TYPE", "response.content.type", "content type")) {
            var response = new LinkedHashMap<>(input.responseFields());
            response.remove("CONTENT-TYPE");
            response.put(field, JSON);
            var result = auditor.inspect(withMetadata(input, input.requestUrl(), response, input.executionFields()));
            assertTrue(result.compliant(), () -> field + ": " + result.violations());
        }
        for (Map<String, String> fields : List.of(Map.<String, String>of(),
                Map.of(ApiReportEvidenceField.CONTENT_TYPE.key(), PLAIN_TEXT),
                Map.of(ApiReportEvidenceField.CONTENT_TYPE.key(), JSON, "CONTENT-TYPE", PLAIN_TEXT))) {
            var response = new LinkedHashMap<>(input.responseFields());
            response.remove("CONTENT-TYPE");
            response.putAll(fields);
            var result = auditor.inspect(withMetadata(input, input.requestUrl(), response, input.executionFields()));
            assertFalse(result.compliant(), "missing, wrong or contradictory evidence must fail");
            assertTrue(result.violations().stream().anyMatch(value -> value.contains("expected CONTENT-TYPE")));
        }
    }

    @Test
    void queryMustComeFromTheRequestUrlEvenWhenExecutionRetainsTheExpectedUrl() {
        var input = acceptedInput();
        var auditor = new ApiReportContractAuditor(catalog(acceptedPostRoute()));
        for (String url : List.of(input.requestUrl().split("\\?")[0],
                input.requestUrl().replace("validate", "wrong"), "")) {
            var result = auditor.inspect(withMetadata(input, url, input.responseFields(), input.executionFields()));
            assertFalse(result.compliant());
            assertTrue(result.violations().stream().anyMatch(value -> value.contains("expected query mode")));
        }
    }

    @Test
    void canonicalExecutionFieldsRemainStrictIncludingConflictingAliases() {
        var input = acceptedInput();
        var auditor = new ApiReportContractAuditor(catalog(acceptedPostRoute()));
        var canonical = Map.of(ApiReportEvidenceField.RESPONSE_STATUS.key(), String.valueOf(input.actualStatus()),
                ApiReportEvidenceField.RESPONSE_URL.key(), input.requestUrl());
        assertTrue(auditor.inspect(withMetadata(input, input.requestUrl(), input.responseFields(), canonical)).compliant());
        for (Map<String, String> drift : List.of(
                Map.of(ApiReportEvidenceField.RESPONSE_STATUS.key(), "500"),
                Map.of(ApiReportEvidenceField.RESPONSE_URL.key(), input.requestUrl() + "&unexpected=true"),
                Map.of(ApiReportEvidenceField.RESPONSE_STATUS.key(), String.valueOf(input.actualStatus()),
                        "response.status", "500"))) {
            assertFalse(auditor.inspect(withMetadata(input, input.requestUrl(), input.responseFields(), drift)).compliant());
        }
    }

    @Test
    void reconcilesRenderedRequestResponseAndExecutionMetadataWithMockCatalog() {
        var result = new ApiReportContractAuditor(catalog(acceptedPostRoute())).inspect(acceptedInput());

        assertTrue(result.compliant(), () -> result.violations().toString());
    }

    @Test
    void rejectsTransportMetadataDriftEvenWhenJsonBodiesStillMatch() {
        var result = new ApiReportContractAuditor(catalog(transportGetRoute())).inspect(transportDriftInput());

        assertAll(
                () -> assertFalse(result.compliant()),
                () -> assertTrue(ViolationSignal.AUTH_PROFILE.occursIn(result.violations())),
                () -> assertTrue(ViolationSignal.QUERY.occursIn(result.violations())),
                () -> assertTrue(ViolationSignal.REQUEST_HEADER.occursIn(result.violations())),
                () -> assertTrue(ViolationSignal.RESPONSE_HEADER.occursIn(result.violations())),
                () -> assertTrue(ViolationSignal.EXECUTION_STATUS.occursIn(result.violations())));
    }

    @Test
    void comparesResponseAsJsonTreeIndependentOfFormattingAndFieldOrder() {
        var result = new ApiReportContractAuditor(catalog(responseTreePostRoute())).inspect(responseTreeInput());

        assertAll(
                () -> assertTrue(result.compliant(), () -> result.violations().toString()),
                () -> assertTrue(result.route().isPresent()));
    }

    @Test
    void reportsStatusAndBodyDriftWithoutHardcodedPayloadTokens() {
        var result = new ApiReportContractAuditor(catalog(bodyDriftGetRoute())).inspect(bodyDriftInput());

        assertAll(
                () -> assertFalse(result.compliant()),
                () -> assertTrue(ViolationSignal.EXPECTED_STATUS.occursIn(result.violations())),
                () -> assertTrue(ViolationSignal.BODY_DRIFT.occursIn(result.violations())));
    }

    @Test
    void requiresJsonRequestAndResponseEvidenceForPutRoutes() {
        var auditor = new ApiReportContractAuditor(catalog(putRoute()));
        var missingRequest = auditor.inspect(putInput("", OK_BODY));
        var missingResponse = auditor.inspect(putInput(REQUEST_BODY, ""));

        assertAll(
                () -> assertFalse(missingRequest.compliant()),
                () -> assertTrue(ViolationSignal.REQUEST_JSON_BODY.occursIn(missingRequest.violations())),
                () -> assertFalse(missingResponse.compliant()),
                () -> assertTrue(ViolationSignal.RESPONSE_JSON_BODY.occursIn(missingResponse.violations())));
    }

    @Test
    void literalNullIsCapturedJsonButMustStillMatchTheConfiguredResponse() {
        var auditor = new ApiReportContractAuditor(catalog(putRoute()));
        assertTrue(auditor.inspect(putInput("null", OK_BODY)).compliant());
        assertFalse(auditor.inspect(putInput(REQUEST_BODY, "null")).compliant());
    }

    @Test
    void rejectsRequestsThatCannotBeResolvedToConfiguredCatalog() {
        var result = new ApiReportContractAuditor(catalog()).inspect(unresolvedInput());

        assertAll(
                () -> assertFalse(result.compliant()),
                () -> assertTrue(ViolationSignal.NO_RESPONSE_CONTRACT.occursIn(result.violations())));
    }

    private static ApiReportContractCatalog catalog(MockRoute... routes) {
        return () -> List.of(routes).stream()
                .map(route -> new ApiReportRouteContract(route.method(), route.path(), route.status(),
                        route.body(), route.headers(), route.authProfile(), route.queryParameters(),
                        route.requiredHeaders(), route.requestBodyMode(), route.expectedRequestBody()))
                .toList();
    }

    private static ApiReportContractInput withMetadata(ApiReportContractInput input, String requestUrl,
                                                       Map<String, String> response, Map<String, String> execution) {
        return new ApiReportContractInput(input.requestTitle(), requestUrl, input.actualStatus(),
                input.requestBody(), input.responseBody(), input.evidenceViolations(),
                input.requestFields(), response, execution);
    }
}

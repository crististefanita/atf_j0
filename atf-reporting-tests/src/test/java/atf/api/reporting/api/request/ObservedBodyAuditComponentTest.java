package atf.api.reporting.api.request;

import atf.api.reporting.audit.ReportAuditSnapshot;
import atf.api.reporting.api.contract.ApiReportContractAuditor;
import atf.api.reporting.api.contract.ApiReportContractInput;
import atf.api.reporting.api.contract.ApiReportRouteContract;
import atf.api.reporting.api.evidence.ReportBodyAvailability;
import atf.api.reporting.support.request.ObservedRequestFixture;
import atf.api.reporting.support.request.ObservedRequestFixture.Renderer;
import atf.api.reporting.support.request.ObservedRequestFixture.Sample;
import atf.api.reporting.ui.audit.section.JsonEvidenceValidator;
import atf.api.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

import static atf.api.reporting.api.evidence.ReportBodyAvailability.AVAILABLE;
import static atf.api.reporting.api.evidence.ReportBodyAvailability.ABSENT;
import static atf.api.reporting.api.evidence.ReportBodyAvailability.NOT_CAPTURED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AtfComponentTest
class ObservedBodyAuditComponentTest {
    @ParameterizedTest(name = "{0}: audit {1}")
    @MethodSource("cases")
    void browserAuditorUsesCaptureMetadataWithoutInventingNull(Renderer renderer, Sample sample) throws Exception {
        var published = ObservedRequestFixture.publish(renderer, List.of(sample));
        var result = ObservedRequestFixture.inspectBody(published, renderer);
        assertEquals(sample.availability, result.requestAvailability());
        assertEquals(sample != Sample.OBSERVED_UNCAPTURED, result.compliant(), result.violations().toString());
        assertEquals(sample.availability == AVAILABLE, result.requestHasPayload());
        if (sample == Sample.OBSERVED_NULL) assertEquals("null", result.requestBody());
        if (sample.availability != AVAILABLE) assertEquals("", result.requestBody());
    }

    @ParameterizedTest
    @EnumSource(ReportBodyAvailability.class)
    void availabilityCannotHideContradictoryOrMissingPayload(ReportBodyAvailability availability) {
        var json = new ReportAuditSnapshot.JsonEvidence("", "null", List.of(), availability, AVAILABLE);
        assertEquals(availability == ABSENT, new JsonEvidenceValidator().validate(json).valid());
        var contradiction = new ReportAuditSnapshot.JsonEvidence("null", "null", List.of(), ABSENT, AVAILABLE);
        assertFalse(new JsonEvidenceValidator().validate(contradiction).valid());
        assertTrue(new ReportAuditSnapshot.JsonEvidence("null", "null", List.of()).requestHasPayload());
        assertFalse(new ReportAuditSnapshot.JsonEvidence("", "", List.of()).compliant());
    }

    @Test
    void catalogCannotConfuseNoResponseWithMissingCaptureOrLiteralNull() {
        var route = new ApiReportRouteContract("GET", "/empty", 204, "", Map.of(), "*",
                null, Map.of(), "JSON", null);
        var auditor = new ApiReportContractAuditor(() -> List.of(route));
        var absent = input("", ABSENT);
        assertTrue(auditor.inspect(absent).compliant(), auditor.inspect(absent).violations().toString());
        assertFalse(auditor.inspect(input("", NOT_CAPTURED)).compliant());
        assertFalse(auditor.inspect(input("null", AVAILABLE)).compliant());
        assertFalse(auditor.inspect(input("null", ABSENT)).compliant());
    }

    private static ApiReportContractInput input(String response, ReportBodyAvailability availability) {
        return new ApiReportContractInput("GET /empty", "https://fixture.invalid/empty", 204,
                "", response, List.of(), Map.of(), Map.of(), Map.of(), ABSENT, availability);
    }

    private static Stream<Arguments> cases() {
        return Stream.of(Renderer.values()).flatMap(renderer ->
                Stream.of(Sample.values()).map(sample -> Arguments.of(renderer, sample)));
    }
}

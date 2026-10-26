package atf.validation;
import atf.testkit.AtfUnitTest;

import atf.core.http.response.ApiResponse;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportPorts;
import atf.core.reporting.contract.AtfReportStatus;
import atf.validation.contract.ResponseContract;
import atf.validation.evaluation.ApiValidationException;
import atf.validation.publishing.ResponseContractReporter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest

class ResponseContractReporterTest {
    @Test
    void oneResponseProducesOneValidationTableWithAllRows() {
        var request = new RecordingRequest();
        var response = new ApiResponse(200, "{\"id\":1,\"name\":\"ATF\"}",
                Map.of("Content-Type", "application/json"), Duration.ZERO, "/users/1");

        var decision = new ResponseContractReporter().validate(request, "Contract validation", response,
                new ResponseContract().status(200).contentTypeContains("application/json")
                        .jsonPathEquals("$.id", "1").jsonPathExists("$.name"), false);

        assertEquals(AtfReportStatus.PASS, decision.status());
        assertEquals(1, request.validationCount);
        assertEquals(4, request.assertions.size());
        assertEquals(AtfReportStatus.PASS, request.validationStatus);
    }

    @Test
    void failedResponsePublishesRowsBeforeThrowing() {
        var request = new RecordingRequest();
        var response = new ApiResponse(500, "{}", Map.of(), Duration.ZERO, "/users/1");

        assertThrows(ApiValidationException.class, () -> new ResponseContractReporter().validate(request,
                "Contract validation", response, new ResponseContract().status(200), false));
        assertEquals(1, request.validationCount);
        assertEquals(AtfReportStatus.FAIL, request.validationStatus);
        assertEquals(1, request.assertions.size());
    }

    private static final class RecordingRequest implements AtfReportPorts.Request {
        private int validationCount;
        private List<AtfAssertion> assertions = List.of();
        private AtfReportStatus validationStatus;

        @Override public void detail(String name, String value) { }
        @Override public void json(String label, String json) { }
        @Override public AtfReportPorts.Validation startValidation(String title) {
            validationCount++;
            return new AtfReportPorts.Validation() {
                @Override public void assertions(Collection<AtfAssertion> rows) { assertions = List.copyOf(rows); }
                @Override public void complete(AtfReportStatus status) { validationStatus = status; }
                @Override public void close() { }
            };
        }
        @Override public void complete(AtfReportStatus status) { }
        @Override public void close() { }
    }
}

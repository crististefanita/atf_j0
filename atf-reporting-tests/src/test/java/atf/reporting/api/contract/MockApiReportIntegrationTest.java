package atf.reporting.api.contract;
import atf.testkit.AtfIntegrationTest;

import atf.reporting.support.MockApiReportContractFixtures;
import atf.reporting.support.MockApiReportExecution;
import org.junit.jupiter.api.Test;

import static atf.reporting.support.MockApiReportContractFixtures.BASE_URL;
import static atf.reporting.support.MockApiReportContractFixtures.REQUEST_BODY;
import static atf.reporting.support.MockApiReportContractFixtures.acceptedPostRoute;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration gate for the reporting module's mock-backed execution port. */
@AtfIntegrationTest
class MockApiReportIntegrationTest {
    @Test
    void executesConfiguredMockAndAuditsTheSameWireEvidence() {
        var route = acceptedPostRoute();
        var exchange = MockApiReportExecution.execute(
                MockApiReportContractFixtures.configuration(route), route, REQUEST_BODY, BASE_URL);

        var result = new ApiReportContractAuditor(
                MockApiReportContractFixtures.catalog(route)).inspect(exchange.contractInput());

        assertAll(
                () -> assertTrue(result.compliant(), () -> result.violations().toString()),
                () -> assertEquals(REQUEST_BODY, exchange.requestBody()),
                () -> assertEquals(MockApiReportContractFixtures.OK_BODY, exchange.responseBody()),
                () -> assertEquals("ACCEPTANCE_MOCK", exchange.execution().provenance().name()),
                () -> assertEquals(202, exchange.execution().response().status()));
    }
}

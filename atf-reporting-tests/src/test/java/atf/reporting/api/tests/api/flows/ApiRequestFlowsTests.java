package atf.reporting.api.tests.api.flows;
import atf.testkit.AtfIntegrationTest;

import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.mock.MockRoutePaths;
import atf.core.json.JsonPayloads;
import atf.reporting.api.tests.support.junit.ApiResourceTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Multi-request API flows: mutate, read back, then validate each response. */
@AtfModuleTest(requires = {AtfModule.API, AtfModule.REPORTING})
@AtfIntegrationTest
class ApiRequestFlowsTests extends ApiResourceTestBase {
    @Test
    void createsPostReadsItBackAndValidatesBothResponses() {
        var created = fixture().api().posts().create(ApiRequestFlowFixtures.CREATED_POST);
        var createdDecision = validate(operation(HttpMethod.POST, MockRoutePaths.POSTS, "flow create"),
                created, ApiRequestFlowFixtures.createdPostMutationContract());
        String createdId = ApiFlowAssertions.resourceId(created, HttpMethod.POST);

        var collection = fixture().api().posts().getCollection();
        var collectionDecision = validate(operation(HttpMethod.GET, MockRoutePaths.POSTS, "flow collection correlation"),
                collection, ApiRequestFlowFixtures.createdPostCollectionContract(createdId));
        var readBack = fixture().api().posts().get(Integer.parseInt(createdId));
        var readDecision = validate(operation(HttpMethod.GET, MockRoutePaths.postById(createdId), "flow read-back"),
                readBack, ApiRequestFlowFixtures.createdPostReadModelContract(createdId));
        ApiFlowAssertions.postReadBack(created, collection, readBack, null);

        assertAll(
                () -> assertTrue(JsonPayloads.write(ApiRequestFlowFixtures.CREATED_POST).startsWith("{")),
                () -> assertEquals(201, created.response().status()),
                () -> assertEquals(AtfReportStatus.PASS, collectionDecision.status()),
                () -> assertEquals(200, readBack.response().status()),
                () -> assertEquals(AtfReportStatus.PASS, createdDecision.status()),
                () -> assertEquals(AtfReportStatus.PASS, readDecision.status()));
    }

    @Test
    void updatesProfileReadsItBackAndValidatesBothResponses() {
        var updated = fixture().api().executor().execute(ApiRequestFlowFixtures.profileUpdateRequest());
        var updatedDecision = validate(operation(HttpMethod.PUT, MockRoutePaths.profileById("1"), "flow update"),
                updated, ApiRequestFlowFixtures.updatedProfileMutationContract());

        var readBack = fixture().api().executor().execute(ApiRequestFlowFixtures.profileReadRequest());
        var readDecision = validate(operation(HttpMethod.GET, MockRoutePaths.profileById("1"), "flow read-back"),
                readBack, ApiRequestFlowFixtures.profileReadModelContract());
        ApiFlowAssertions.profileReadBack(updated, readBack, null);

        assertAll(
                () -> assertEquals(200, updated.response().status()),
                () -> assertEquals(200, readBack.response().status()),
                () -> assertEquals(AtfReportStatus.PASS, updatedDecision.status()),
                () -> assertEquals(AtfReportStatus.PASS, readDecision.status()));
    }
}

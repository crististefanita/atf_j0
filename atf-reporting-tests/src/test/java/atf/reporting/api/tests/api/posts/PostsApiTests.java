package atf.reporting.api.tests.api.posts;
import atf.testkit.AtfIntegrationTest;

import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.core.json.JsonPayloads;
import atf.reporting.api.tests.api.data.ApiFixtureData;
import atf.mock.MockRoutePaths;
import atf.reporting.api.tests.support.junit.ApiResourceTestBase;
import atf.validation.contract.ContractValidators;
import atf.validation.contract.ResponseContract;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Readable post-resource API tests: collections, items and relationships. */
@AtfModuleTest(requires = {AtfModule.API, AtfModule.REPORTING})
@AtfIntegrationTest
class PostsApiTests extends ApiResourceTestBase {

    @Test
    void getsPostCollectionAndValidatesItsContract() {
        var execution = fixture().api().posts().getCollection();
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.POSTS, "collection contract"), execution,
                ContractValidators.postCollection());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().body().contains("\"id\":101")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void getsPostByIdAndValidatesTheItemContract() {
        var execution = fixture().api().posts().get(ApiFixtureData.EXISTING_POST_ID);
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.postById(String.valueOf(ApiFixtureData.EXISTING_POST_ID)), "item contract"), execution,
                ContractValidators.postItem());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().body().contains("\"user_id\":1")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void getsPostsForAUserAndValidatesTheRelationshipContract() {
        var execution = fixture().api().posts().forUser(1);
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.userPosts("1"), "relationship contract"), execution,
                ContractValidators.relationshipCollection("1"));

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().url().endsWith(MockRoutePaths.userPosts("1"))),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void preservesNotFoundAsAnExplicitNegativeContract() {
        var execution = fixture().api().posts().get(ApiFixtureData.UNKNOWN_POST_ID);
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.postById(String.valueOf(ApiFixtureData.UNKNOWN_POST_ID)), "not found contract"), execution,
                new ResponseContract().status(404)
                        .contentTypeContains(MediaTypes.APPLICATION_JSON)
                        .bodyContains("Not found"));

        assertAll(
                () -> assertEquals(404, execution.response().status()),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void createsPostWithJsonRequestAndJsonResponseContract() {
        var payload = ApiFixtureData.createdPostRequest();
        var execution = fixture().api().posts().create(payload);
        var decision = validate(operation(HttpMethod.POST, MockRoutePaths.POSTS, "create contract"), execution,
                new ResponseContract().status(201)
                        .contentTypeContains(MediaTypes.APPLICATION_JSON)
                        .jsonPathEquals("$.id", String.valueOf(ApiFixtureData.CREATED_POST_ID))
                        .jsonPathEquals("$.user_id", String.valueOf(ApiFixtureData.USER_ID))
                        .jsonPathEquals("$.title", ApiFixtureData.CREATED_POST_TITLE)
                        .jsonPathEquals("$.body", ApiFixtureData.POST_BODY));

        assertAll(
                () -> assertTrue(JsonPayloads.write(payload).startsWith("{")),
                () -> assertEquals(201, execution.response().status()),
                () -> assertTrue(execution.response().body().contains("\"id\":102")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }
}

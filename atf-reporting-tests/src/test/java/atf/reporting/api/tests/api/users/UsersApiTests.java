package atf.reporting.api.tests.api.users;
import atf.testkit.AtfIntegrationTest;

import atf.core.config.module.AtfModule;
import atf.reporting.test.AtfModuleTest;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.mock.MockRoutePaths;
import atf.reporting.api.execution.model.UserRequest;
import atf.reporting.api.tests.support.junit.ApiResourceTestBase;
import atf.validation.contract.ContractValidators;
import atf.validation.contract.ResponseContract;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Readable user-resource API tests: action, response contract and verdict. */
@AtfModuleTest(requires = {AtfModule.API, AtfModule.REPORTING})
@AtfIntegrationTest
class UsersApiTests extends ApiResourceTestBase {

    @Test
    void getsUserCollectionAndValidatesItsContract() {
        var execution = fixture().api().users().getCollection();
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.USERS, "collection contract"), execution,
                ContractValidators.userCollection());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().body().contains("\"id\":1")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void getsUserByIdAndValidatesTheItemContract() {
        var execution = fixture().api().users().get(1);
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.userById("1"), "item contract"), execution,
                ContractValidators.userItem());

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertTrue(execution.response().url().endsWith(MockRoutePaths.userById("1"))),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void rejectsCreateWithoutAuthentication() {
        var execution = fixture().api().users().create(
                new UserRequest("ATF user", "atf@example.test", "male", "active"));
        var decision = validate(operation(HttpMethod.POST, MockRoutePaths.USERS, "missing authentication"), execution,
                new ResponseContract().status(401)
                        .contentTypeContains(MediaTypes.APPLICATION_JSON)
                        .bodyContains("Authentication required"));

        assertAll(
                () -> assertEquals(401, execution.response().status()),
                () -> assertTrue(execution.response().body().contains("Authentication required")),
                () -> assertEquals(AtfReportStatus.PASS, decision.status()));
    }

    @Test
    void keepsAContractMismatchAsWarningWhenWarningModeIsExplicit() {
        var execution = fixture().api().users().getCollection();
        var decision = validate(operation(HttpMethod.GET, MockRoutePaths.USERS, "soft contract warning"), execution,
                new ResponseContract().status(201).contentTypeContains(MediaTypes.APPLICATION_JSON), true);

        assertAll(
                () -> assertEquals(200, execution.response().status()),
                () -> assertEquals(AtfReportStatus.WARNING, decision.status()),
                () -> assertFalse(decision.shouldThrow()));
    }
}

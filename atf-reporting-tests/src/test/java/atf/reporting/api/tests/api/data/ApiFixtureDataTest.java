package atf.reporting.api.tests.api.data;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Protects the semantic fixture boundary used by direct tests and report flows. */
@AtfUnitTest
class ApiFixtureDataTest {
    @Test
    void exposesOneTypedSourceForCrossFamilyDomainValues() {
        var request = ApiFixtureData.createdPostRequest();
        var user = ApiFixtureData.unauthenticatedUserRequest();

        assertAll(
                () -> assertEquals(ApiFixtureData.CREATED_POST_TITLE, request.title()),
                () -> assertEquals(ApiFixtureData.POST_BODY, request.body()),
                () -> assertEquals(ApiFixtureData.USER_ID, request.userId()),
                () -> assertEquals(ApiFixtureData.CREATED_POST_ID, ApiFixtureData.CREATED_POST.id()),
                () -> assertEquals(ApiFixtureData.EXISTING_POST_TITLE, ApiFixtureData.EXISTING_POST.title()),
                () -> assertEquals(ApiFixtureData.PROFILE_ID, ApiFixtureData.PROFILE.id()),
                () -> assertEquals(ApiFixtureData.LOCALE, ApiFixtureData.PROFILE.locale()),
                () -> assertEquals(ApiFixtureData.TIMEZONE, ApiFixtureData.PROFILE.timezone()),
                () -> assertEquals(ApiFixtureData.USER_NAME, user.name()),
                () -> assertEquals(ApiFixtureData.USER_EMAIL, user.email()));
    }
}

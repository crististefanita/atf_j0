package atf.reporting.api.tests.api.data;

import atf.reporting.api.execution.model.PostRequest;
import atf.reporting.api.execution.model.UserRequest;

/**
 * Typed semantic fixtures shared by API tests and report scenarios.
 *
 * <p>These values are intentionally limited to domain identities and payloads
 * that cross more than one API family. One-off negative examples stay local to
 * the scenario that gives them meaning.</p>
 */
public final class ApiFixtureData {
    public static final int USER_ID = 1;
    public static final int EXISTING_POST_ID = 101;
    public static final int CREATED_POST_ID = 102;
    public static final int PROFILE_ID = USER_ID;
    public static final int UNKNOWN_POST_ID = 999999;

    public static final String CREATED_POST_TITLE = "ATF created post";
    public static final String EXISTING_POST_TITLE = "ATF post";
    public static final String POST_BODY = "contract";
    public static final String USER_NAME = "ATF User";
    public static final String USER_EMAIL = "atf@example.test";
    public static final String USER_GENDER = "male";
    public static final String USER_STATUS = "active";
    public static final String LOCALE = "ro-RO";
    public static final String TIMEZONE = "Europe/Bucharest";

    public static final PostFixture EXISTING_POST = new PostFixture(
            EXISTING_POST_ID, USER_ID, EXISTING_POST_TITLE, POST_BODY);
    public static final PostFixture CREATED_POST = new PostFixture(
            CREATED_POST_ID, USER_ID, CREATED_POST_TITLE, POST_BODY);
    public static final ProfileFixture PROFILE = new ProfileFixture(
            PROFILE_ID, LOCALE, TIMEZONE);

    private ApiFixtureData() { }

    public static PostRequest createdPostRequest() {
        return CREATED_POST.request();
    }

    public static UserRequest unauthenticatedUserRequest() {
        return new UserRequest(USER_NAME, USER_EMAIL, USER_GENDER, USER_STATUS);
    }

    public record PostFixture(int id, int userId, String title, String body) {
        public PostRequest request() {
            return new PostRequest(title, body, userId);
        }
    }

    public record ProfileFixture(int id, String locale, String timezone) { }
}

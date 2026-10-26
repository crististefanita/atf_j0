package atf.reporting.api.execution.routing;
import atf.testkit.AtfComponentTest;

import atf.mock.MockRoutePaths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfComponentTest

class RouteCatalogTest {
    @Test
    void routeMetadataIsComposedFromCanonicalTemplates() {
        assertEquals(MockRoutePaths.USERS, ApiRoutes.usersCollection().pathTemplate());
        assertEquals(MockRoutePaths.USER_BY_ID, ApiRoutes.userItem().pathTemplate());
        assertEquals(MockRoutePaths.POSTS, ApiRoutes.postsCollection().pathTemplate());
        assertEquals(MockRoutePaths.POST_BY_ID, ApiRoutes.postItem().pathTemplate());
        assertEquals(MockRoutePaths.USER_POSTS, ApiRoutes.userPosts().pathTemplate());
        assertEquals(MockRoutePaths.USERS, ApiRoutes.userCreate().pathTemplate());
        assertEquals(MockRoutePaths.POSTS, ApiRoutes.postCreate().pathTemplate());
    }
}

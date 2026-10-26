package atf.ui.tests.extent.support.api;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.testkit.setup.api.ApiTestRef;
import atf.reporting.api.tests.api.posts.PostsApiScenarios;


/** Annotation-only classes used to test API-family setup resolution. */
public final class ApiReportSetupDeclarations {
    private ApiReportSetupDeclarations() {
    }

    @ApiScenarioSetup(families = {"users", "posts"})
    public static final class UsersPosts {
        private UsersPosts() {
        }
    }

    @ApiScenarioSetup
    public static final class Empty {
        private Empty() {
        }
    }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = PostsApiScenarios.class, method = PostsApiScenarios.CREATE_POST)
    })
    public static final class ExactPostCreate {
        private ExactPostCreate() {
        }
    }
}

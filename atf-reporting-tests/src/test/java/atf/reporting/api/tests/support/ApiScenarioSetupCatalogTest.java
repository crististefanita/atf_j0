package atf.reporting.api.tests.support;
import atf.testkit.AtfUnitTest;
import atf.core.testing.api.ApiScenarioSelection;
import atf.testkit.setup.api.ApiScenarioSetup;
import atf.testkit.setup.api.ApiTestRef;
import atf.reporting.api.tests.support.setup.ApiScenarioSetupCatalog;

import atf.reporting.api.tests.api.posts.PostsApiScenarios;
import atf.reporting.api.tests.api.users.UsersApiScenarios;
import atf.reporting.api.tests.api.requests.RequestVariantsApiScenarios;
import atf.reporting.api.tests.api.headers.ApiHeadersAndEdgeCaseScenarios;
import atf.reporting.api.tests.api.flows.ApiRequestFlowScenarios;
import atf.reporting.api.tests.api.negative.ApiNegativeScenarios;
import atf.reporting.api.tests.api.lifecycle.LifecycleApiScenarios;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ApiScenarioSetupCatalogTest {
    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION)
    })
    private static final class OneReference { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION),
            @ApiTestRef(owner = PostsApiScenarios.class, method = PostsApiScenarios.CREATE_POST)
    })
    private static final class TwoReferences { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION),
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_BY_ID),
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.CREATE_UNAUTHENTICATED)
    })
    private static final class ManyReferences { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION),
            @ApiTestRef(owner = UsersApiScenarios.class, method = UsersApiScenarios.GET_COLLECTION)
    })
    private static final class DuplicateReferences { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = String.class, method = "unknown")
    })
    private static final class InvalidReference { }

    @ApiScenarioSetup(tests = {
            @ApiTestRef(owner = RequestVariantsApiScenarios.class, method = RequestVariantsApiScenarios.SEARCH),
            @ApiTestRef(owner = ApiHeadersAndEdgeCaseScenarios.class, method = ApiHeadersAndEdgeCaseScenarios.EXPORT),
            @ApiTestRef(owner = ApiRequestFlowScenarios.class, method = ApiRequestFlowScenarios.PROFILE_UPDATE),
            @ApiTestRef(owner = ApiNegativeScenarios.class, method = ApiNegativeScenarios.NEGATIVE_MATRIX),
            @ApiTestRef(owner = LifecycleApiScenarios.class, method = LifecycleApiScenarios.LIFECYCLE_MATRIX)
    })
    private static final class AllPublisherFamilies { }

    @Test
    void resolvesOneTwoAndManyReferencesWithoutLearningFromDom() {
        assertEquals(1, resolve(OneReference.class).size());
        assertEquals(2, resolve(TwoReferences.class).size());
        assertEquals(3, resolve(ManyReferences.class).size());
        assertEquals(List.of("users", "posts"),
                ApiScenarioSetupCatalog.familiesFor(resolve(TwoReferences.class)));
    }

    @Test
    void rejectsDuplicateReferencesWithActionableIdentity() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolve(DuplicateReferences.class));
        assertTrue(error.getMessage().contains("UsersApiScenarios.getCollection"));
    }

    @Test
    void rejectsUnknownReferenceOwnerBeforeBrowserSetup() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> resolve(InvalidReference.class));
        assertTrue(error.getMessage().contains("Unsupported API scenario owner"));
    }

    @Test
    void resolvesEveryPublisherFamilyThroughTheSameTypedContract() {
        var resolved = resolve(AllPublisherFamilies.class);
        assertEquals(List.of("requests", "headers", "flows", "negative", "lifecycle"),
                ApiScenarioSetupCatalog.familiesFor(resolved));
        assertEquals(List.of("RequestVariantsApiTests", "ApiHeadersAndEdgeCasesTests",
                        "ApiRequestFlowsTests", "ApiNegativeMatrixTests", "LifecycleApiScenarios"),
                resolved.stream().map(ApiScenarioSelection::reportOwner).toList());
    }

    private static List<ApiScenarioSelection> resolve(Class<?> owner) {
        return ApiScenarioSetupCatalog.resolve(owner.getAnnotation(ApiScenarioSetup.class).tests());
    }
}

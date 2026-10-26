package atf.ui.tests.extent.support.data;

import atf.reporting.api.tests.api.flows.ApiRequestFlowScenarios;
import atf.reporting.api.tests.api.headers.ApiHeadersAndEdgeCaseScenarios;
import atf.reporting.api.tests.api.lifecycle.LifecycleApiScenarios;
import atf.reporting.api.tests.api.negative.ApiNegativeScenarios;
import atf.reporting.api.tests.api.posts.PostsApiScenarios;
import atf.reporting.api.tests.api.requests.RequestVariantsApiScenarios;
import atf.reporting.api.tests.api.users.UsersApiScenarios;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Shared typed data for configurable report UI tests. */
public final class UiTestData {
    public static final String API_FAMILIES_PROPERTY = "atf.ui.api.families";
    public static final String AUDIT_SUITE_PROPERTY = "atf.ui.audit.suite";
    public static final String USERS_FAMILY = "users";
    public static final String POSTS_FAMILY = "posts";
    public static final String REQUESTS_FAMILY = "requests";
    public static final String HEADERS_FAMILY = "headers";
    public static final String LIFECYCLE_FAMILY = "lifecycle";
    public static final String FLOWS_FAMILY = "flows";
    public static final String NEGATIVE_FAMILY = "negative";
    public static final String USERS_OWNER = UsersApiScenarios.REPORT_OWNER;
    public static final String POSTS_OWNER = PostsApiScenarios.REPORT_OWNER;
    public static final String REQUESTS_OWNER = RequestVariantsApiScenarios.REPORT_OWNER;
    public static final String HEADERS_OWNER = ApiHeadersAndEdgeCaseScenarios.REPORT_OWNER;
    public static final String LIFECYCLE_OWNER = LifecycleApiScenarios.REPORT_OWNER;
    public static final String FLOWS_OWNER = ApiRequestFlowScenarios.REPORT_OWNER;
    public static final String NEGATIVE_OWNER = ApiNegativeScenarios.REPORT_OWNER;
    public static final String TAG_UI = "ui";
    public static final String TAG_API = "api";
    public static final String TAG_MOCK_CONTRACT = "mock-contract";
    public static final String TAG_EXPLORATORY = "exploratory";
    public static final String TAG_REPORTING = "reporting";

    private UiTestData() { }

    public enum ApiFamily {
        USERS(USERS_FAMILY, USERS_OWNER, UsersApiScenarios.REPORT_SCENARIO_TITLES),
        POSTS(POSTS_FAMILY, POSTS_OWNER, PostsApiScenarios.REPORT_SCENARIO_TITLES),
        REQUESTS(REQUESTS_FAMILY, REQUESTS_OWNER, RequestVariantsApiScenarios.REPORT_SCENARIO_TITLES),
        HEADERS(HEADERS_FAMILY, HEADERS_OWNER, ApiHeadersAndEdgeCaseScenarios.REPORT_SCENARIO_TITLES),
        FLOWS(FLOWS_FAMILY, FLOWS_OWNER, ApiRequestFlowScenarios.REPORT_SCENARIO_TITLES),
        NEGATIVE(NEGATIVE_FAMILY, NEGATIVE_OWNER, ApiNegativeScenarios.REPORT_SCENARIO_TITLES),
        LIFECYCLE(LIFECYCLE_FAMILY, LIFECYCLE_OWNER, LifecycleApiScenarios.REPORT_SCENARIO_TITLES);

        private final String key;
        private final String ownerName;
        private final List<String> scenarioTitles;

        ApiFamily(String key, String ownerName, List<String> scenarioTitles) {
            this.key = key;
            this.ownerName = ownerName;
            this.scenarioTitles = List.copyOf(scenarioTitles);
        }

        public String key() { return key; }

        public String ownerName() { return ownerName; }

        public List<String> scenarioTitles() { return scenarioTitles; }

        public static Optional<ApiFamily> fromKey(String value) {
            if (value == null || value.isBlank()) return Optional.empty();
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (ApiFamily family : values()) {
                if (family.key.equals(normalized)) return Optional.of(family);
            }
            return Optional.empty();
        }
    }

    public static List<String> defaultTags() {
        return List.of(TAG_UI, TAG_API, TAG_MOCK_CONTRACT);
    }
}

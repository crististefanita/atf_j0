package atf.reporting.extent.session;

import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.support.TestReportPaths;
import atf.reporting.support.ApiReportTestUrls;
import atf.core.http.request.HttpMethod;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract tests for tab identity, filtering and category deep links. */
@AtfUnitTest
class ExtentReportNavigationContractTest {
    private static final String USERS = ApiReportTestUrls.USERS;
    private static final String POSTS = ApiReportTestUrls.POSTS;
    private static final String USERS_OPERATION = ApiReportTestUrls.operation(HttpMethod.GET, USERS, null);
    private static final String POSTS_OPERATION = ApiReportTestUrls.operation(HttpMethod.GET, POSTS, null);

    @Test
    void testsTabPublishesIndependentNavigationAndFilterContracts() throws Exception {
        Path report = TestReportPaths.report("extent-tests-tab-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", ApiReportTestUrls.operation(HttpMethod.GET, USERS, "collection contract"),
                    List.of("api", "users", "contract", "smoke"));
            var body = scenario.startPhase("TEST BODY");
            body.startRequest(USERS_OPERATION, HttpMethod.GET.value(), USERS)
                    .complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("id=\"nav-test\" class=\"dropdown-toggle\" href=\"#test-view\"")),
                () -> assertTrue(html.contains("id=\"nav-category\" class=\"dropdown-toggle\" href=\"#category-view\"")),
                () -> assertTrue(html.contains("id=\"nav-fails\" class=\"dropdown-toggle\" href=\"#atf-fails-view\"")),
                () -> assertTrue(html.contains("id=\"nav-overview\" class=\"dropdown-toggle\" href=\"#dashboard-view\"")),
                () -> assertTrue(html.contains("data-atf-tab=\"overview\"")),
                () -> assertTrue(html.contains("data-atf-tab=\"fails\"")),
                () -> assertTrue(html.contains("var atfFilterState={status:'',tag:''}")),
                () -> assertTrue(html.contains("function filterMatches(item)")),
                () -> assertTrue(html.contains("function applyTestFilters()")),
                () -> assertTrue(html.contains("function setTestFilter(kind,value)")),
                () -> assertTrue(html.contains("function normalizeTagFilter()")),
                () -> assertTrue(html.contains("function normalizeCategorySummaryLabels()"),
                        "Category summaries must use Warning instead of the native Others label"),
                () -> assertTrue(html.contains("data-atf-filter-hidden")),
                () -> assertTrue(html.contains("e.preventDefault();e.stopPropagation()")),
                () -> assertTrue(html.contains("aria-pressed")),
                () -> assertTrue(html.contains("data-atf-tab=\"fails\"")),
                () -> assertTrue(html.contains("function activateFromHash()")),
                () -> assertTrue(html.contains("window.addEventListener('hashchange',activateFromHash)")),
                () -> assertFalse(html.contains("onclick=\"if(typeof atfActivate"),
                        "navigation must not embed JavaScript handlers in generated HTML")
        );
    }

    @Test
    void testsFiltersPreserveOwnerScopeAndTabIdentity() throws Exception {
        Path report = TestReportPaths.report("extent-tests-filter-owner-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var users = session.startScenario("UsersApiTests", ApiReportTestUrls.operation(HttpMethod.GET, USERS, "collection contract"),
                    List.of("api", "users", "contract"));
            var body = users.startPhase("TEST BODY");
            body.startRequest(USERS_OPERATION, HttpMethod.GET.value(), USERS)
                    .complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            users.complete(AtfReportStatus.PASS);

            var posts = session.startScenario("PostsApiTests", ApiReportTestUrls.operation(HttpMethod.GET, POSTS, "collection contract"),
                    List.of("api", "posts", "contract"));
            var postsBody = posts.startPhase("TEST BODY");
            postsBody.startRequest(POSTS_OPERATION, HttpMethod.GET.value(), POSTS)
                    .complete(AtfReportStatus.PASS);
            postsBody.complete(AtfReportStatus.PASS);
            posts.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int tests = html.indexOf("href=\"#test-view\"");
        int category = html.indexOf("href=\"#category-view\"");
        int fails = html.indexOf("href=\"#atf-fails-view\"");
        int overview = html.indexOf("href=\"#dashboard-view\"");

        assertAll(
                () -> assertTrue(tests >= 0 && tests < category && category < fails && fails < overview,
                        "tab order must remain Tests, Category, Fails, Overview"),
                () -> assertTrue(html.contains(
                        "rootStatusMatches=!atfFilterState.status||statusOf(root)===atfFilterState.status"),
                        "status filtering must use the class owner verdict"),
                () -> assertTrue(html.contains(
                        "testTags(item).some(function(tag){return tag.toLowerCase()===atfFilterState.tag;})"),
                        "tag filtering must use the scenario owner tags"),
                () -> assertTrue(html.contains("atfFilterState[kind]===normalized?'':normalized"),
                        "reselecting a filter must deselect it"),
                () -> assertTrue(html.contains("if(!selectedVisible&&firstVisible)"),
                        "filtering must move the detail pane to a visible owner"),
                () -> assertTrue(html.contains("e.preventDefault();e.stopPropagation()")
                                && html.contains("window.location.hash"),
                        "filter clicks must not navigate away from Tests"),
                () -> assertTrue(html.contains("data-atf-filter-hidden")
                                && html.contains("aria-pressed"),
                        "filter state must be inspectable and accessible")
        );
    }

    @Test
    void categoriesUseIndependentProjectionAndExactScenarioDeepLinks() throws Exception {
        Path report = TestReportPaths.report("extent-category-tab-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var users = session.startScenario("UsersApiTests", ApiReportTestUrls.operation(HttpMethod.GET, USERS, "collection contract"),
                    List.of("api", "users", "contract"));
            var usersBody = users.startPhase("TEST BODY");
            usersBody.startRequest(USERS_OPERATION, HttpMethod.GET.value(), USERS)
                    .complete(AtfReportStatus.PASS);
            usersBody.complete(AtfReportStatus.PASS);
            users.complete(AtfReportStatus.PASS);

            var posts = session.startScenario("PostsApiTests", ApiReportTestUrls.operation(HttpMethod.GET, POSTS, "collection contract"),
                    List.of("api", "posts", "contract"));
            var postsBody = posts.startPhase("TEST BODY");
            postsBody.startRequest(POSTS_OPERATION, HttpMethod.GET.value(), POSTS)
                    .complete(AtfReportStatus.PASS);
            postsBody.complete(AtfReportStatus.PASS);
            posts.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("var atfCategoryState={selectedName:''}")),
                () -> assertTrue(html.contains("function categoryItems()")),
                () -> assertTrue(html.contains("function renderCategoryDetail(item)")),
                () -> assertTrue(html.contains("function selectCategory(item)")),
                () -> assertTrue(html.contains("function bindCategorySelection()")),
                () -> assertTrue(html.contains("function ensureCategoryDetail()")),
                () -> assertTrue(html.contains("detail.innerHTML=''"),
                        "a category selection must clear the previous category projection"),
                () -> assertTrue(html.contains("projection.classList.add('atf-category-detail')")),
                () -> assertTrue(html.contains("data-atf-category-title")),
                () -> assertTrue(html.contains("data-atf-scenario-index")),
                () -> assertTrue(html.contains("&scenario-index='+index"),
                        "category links must preserve the exact scenario inside a class root"),
                () -> assertTrue(html.contains("function openScenarioCard(card)"),
                        "a category deep-link must open the exact scenario card, not only its class root"),
                () -> assertTrue(html.contains("var detailCards=directScenarioCards(document.querySelector('#test-view .test-content-detail'))"),
                        "scenario selection must target the rendered detail projection"),
                () -> assertTrue(html.contains("openScenarioCard(card)")),
                () -> assertTrue(html.contains("title','Open this test in Tests")),
                () -> assertTrue(html.contains("if(id==='category-view'){ensureCategoryFilters();ensureCategoryDetail();normalizeCategorySummaryLabels();normalizeCategoryLinks();}")),
                () -> assertTrue(html.contains("'category-view':'nav-category'")),
                () -> assertTrue(html.contains("window.addEventListener('hashchange',activateFromHash)"))
        );
    }
}

package atf.reporting.extent.session;
import atf.testkit.AtfComponentTest;

import atf.core.http.request.HttpMethod;
import atf.reporting.support.TestReportPaths;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.support.publication.CleanupPublicationFixture;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.evidence.ReportEvidenceStyleContract;
import atf.mock.MockRoutePaths;
import atf.core.http.response.ApiAttemptOutcome;
import atf.core.http.response.ApiAttemptRecord;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.response.RetryEvent;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.security.SensitiveDataRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class ExtentReportSessionContractTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupFailurePreservesTheCompleteOfflineArtifact(boolean sharingViolation) throws Exception {
        String kind = sharingViolation ? "sharing-violation" : "access-denied";
        Path report = CleanupPublicationFixture.rawReport(
                TestReportPaths.report("cleanup-publication-" + kind + "/report.html"));
        Path spark = report.getParent().resolve(ExtentReportAssets.SPARK_DIRECTORY);
        IOException original = sharingViolation
                ? new FileSystemException(spark.toString(), null, "simulated sharing violation")
                : new AccessDeniedException(spark.toString(), null, "simulated access denial");
        assertTrue(Files.isDirectory(spark), "the real vendor must supply assets to inline");
        assertTrue(Files.readString(report).contains("spark/"));

        IOException actual = assertThrows(IOException.class, () ->
                ExtentReportPublicationPipeline.publish(report, assetDirectory -> {
                    assertEquals(spark, assetDirectory);
                    throw original;
                }));

        String html = Files.readString(report);
        assertAll(
                () -> assertSame(original, actual),
                () -> assertTrue(html.contains("AssetCleanupContract")),
                () -> assertTrue(html.contains("<body class=\"spa -report dark atf-overview-default\">")),
                () -> assertTrue(html.contains("id=\"atf-report-theme\"")),
                () -> assertTrue(html.contains("id=\"atf-report-behaviour\"")),
                () -> assertTrue(html.contains("data-atf-inline=\"spark-style.css\"")),
                () -> assertFalse(html.contains("spark/"), "the final HTML must not depend on retained assets"),
                () -> assertTrue(html.stripTrailing().endsWith("</html>")),
                () -> assertTrue(Files.isDirectory(spark), "a rejected cleanup must not pretend assets were removed"),
                () -> assertEquals(List.of(), CleanupPublicationFixture.temporaryOutputs(report)));
    }

    @ParameterizedTest
    @CsvSource({
            "Browser automation client,Browser automation client",
            "Consumer client token=raw-client-secret,Consumer client token=***REDACTED***"
    })
    void explicitConsumerClientIsPreservedAndRedactedInSystem(String client, String displayed) throws Exception {
        Path report = TestReportPaths.report("extent-explicit-client-" + (client.contains("token=") ? "redacted" : "plain") + ".html");
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.CLIENT, client).build();
        try (var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                new SensitiveDataRedactor(), AtfConsoleSink.noOp(), environment)) {
            session.startScenario("ClientContract", "consumer metadata", List.of("component"))
                    .complete(AtfReportStatus.PASS);
        }
        String html = Files.readString(report);
        assertTrue(html.matches("(?s).*<td>Client</td>\\s*<td>" +
                Pattern.quote(displayed) + "</td>.*"));
        assertEquals(displayed, systemClient(html));
        assertFalse(html.contains("raw-client-secret"));
        assertFalse(html.contains("ATF ApiTransport"));
    }

    @Test
    void typedEnvironmentSnapshotIsPublishedRedactedAndWithoutArbitraryKeys() throws Exception {
        Path report = TestReportPaths.report("extent-typed-environment.html");
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "LOCAL")
                .put(ReportEnvironmentField.UI_TIMEOUT, "20 s")
                .put(ReportEnvironmentField.AUTHENTICATION, "Bearer raw-report-token")
                .build();
        try (var session = new ExtentReportSession(report,
                ReportingSettings.defaults(),
                new SensitiveDataRedactor(),
                AtfConsoleSink.noOp(), environment)) {
            var scenario = session.startScenario("EnvironmentTests", "typed metadata", List.of("reporting"));
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                 () -> assertTrue(html.contains("UI action timeout")),
                () -> assertTrue(html.contains("20 s")),
                () -> assertTrue(html.contains("***REDACTED***")),
                () -> assertFalse(html.contains("raw-report-token")),
                () -> assertFalse(html.contains("raw.yaml.key")));
    }

    private static String systemClient(String html) {
        var row = Pattern.compile("<td>Client</td>\\s*<td>([^<]*)</td>").matcher(html);
        assertTrue(row.find(), "System must publish a Client row");
        String value = row.group(1);
        assertFalse(row.find(), "System must publish Client exactly once");
        return value;
    }

    private static final String USERS_PATH = MockRoutePaths.USERS;
    private static final String USER_ONE_PATH = MockRoutePaths.userById("1");
    private static final String UNKNOWN_USER_PATH = MockRoutePaths.userById("999");
    private static final String GET_USERS = ApiReportTestUrls.operation(HttpMethod.GET, USERS_PATH, null);
    private static final String GET_USER_ONE = ApiReportTestUrls.operation(HttpMethod.GET, USER_ONE_PATH, null);
    private static final String SECURE_PATH = ApiReportTestUrls.GENERIC_SECURE;

    @Test
    void requestCannotCloseBeforeItsValidation() throws Exception {
        Path report = TestReportPaths.report("extent-api-request-validation-order.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", "validation order", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("contract", HttpMethod.GET.value(), ApiReportTestUrls.GENERIC_USER_ONE);
            var validation = request.startValidation("Validate response");

            assertThrows(IllegalStateException.class, () -> request.complete(AtfReportStatus.PASS));

            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
        assertTrue(Files.exists(report));
    }

    @Test
    void routeAlreadyUsedAsRequestTitleIsNotDuplicated() throws Exception {
        Path report = TestReportPaths.report("extent-api-request-title-normalization.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", "route title", List.of("api"));
            var phase = scenario.startPhase("TEST BODY");
            phase.startRequest(GET_USERS, HttpMethod.GET.value(), USERS_PATH)
                    .complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertTrue(html.contains(GET_USERS));
        assertFalse(html.contains(GET_USERS + " — " + GET_USERS));
    }

    @Test
    void extentBackendPublishesLifecycleAndAggregateAssertions() throws Exception {
        Path report = TestReportPaths.report("extent-api-report.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", "GET user — item contract", List.of("api", "contract", "smoke"));
            var setup = scenario.startPhase("SETUP");
            var setupRequest = setup.startRequest("health check", HttpMethod.GET.value(), USER_ONE_PATH);
            setupRequest.detail("auth.mode", "none");
            setupRequest.complete(AtfReportStatus.PASS);
            setup.complete(AtfReportStatus.PASS);

            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest("item contract", HttpMethod.GET.value(), USER_ONE_PATH);
            request.detail("response.status", "200");
            request.detail("response.duration.ms", "12");
            request.json("RESPONSE JSON", "{\"id\":1}");
            var validation = request.startValidation("Validate user contract");
            validation.assertions(List.of(
                    new AtfAssertion("HTTP status", "200", "200", true),
                    new AtfAssertion("JSON schema", "matched", "matched", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);

            var teardown = scenario.startPhase("TEARDOWN");
            teardown.startRequest("cleanup", HttpMethod.DELETE.value(), USER_ONE_PATH).complete(AtfReportStatus.PASS);
            teardown.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        assertTrue(Files.exists(report));
        String html = Files.readString(report);
        int requestIndex = html.indexOf(GET_USER_ONE);
        int validationIndex = html.indexOf("Validate user contract");
        assertTrue(requestIndex >= 0 && validationIndex > requestIndex);
        assertTrue(html.contains(GET_USER_ONE + " — item contract"));
        assertTrue(divDepthAt(html, validationIndex) > divDepthAt(html, requestIndex),
                "validation must be nested inside the Gherkin request step");
        assertAll(
                () -> assertTrue(html.contains("UsersApiTests")),
                () -> assertTrue(html.contains("SETUP")),
                () -> assertTrue(html.contains("TEST BODY")),
                () -> assertTrue(html.contains("TEARDOWN")),
                () -> assertTrue(html.contains(GET_USER_ONE)),
                () -> assertTrue(html.contains("Validate user contract")),
                () -> assertTrue(html.contains("HTTP status")),
                () -> assertTrue(html.contains("JSON schema")),
                () -> assertTrue(html.contains("RESPONSE JSON")),
                () -> assertTrue(html.contains("TECHNICAL TRACE")),
                 () -> assertTrue(html.toUpperCase(java.util.Locale.ROOT).contains("RESPONSE DURATION")),
                 () -> assertTrue(html.contains("Request details · 1 field")),
                 () -> assertTrue(html.contains("Response details · 1 field")),
                 () -> assertTrue(html.contains("helm-request-table")),
                () -> assertTrue(html.contains("helm-evidence-table helm-validation-table")),
                () -> assertTrue(html.contains("function normalizeEvidenceDisclosures()")),
                () -> assertTrue(html.contains("function normalizePercentages()")),
                () -> assertTrue(html.contains("function removeUnsupportedOverviewCards()")),
                () -> assertTrue(html.contains("#events-analysis")),
                () -> assertTrue(html.contains("(column||card||canvas).remove()")),
                () -> assertTrue(html.contains("setTimeout(removeUnsupportedOverviewCards,150)")),
                () -> assertTrue(containsJavaScriptSignature(html,
                                "function collapseScenarioCards(force,root,open)")),
                () -> assertTrue(html.contains("data-atf-collapse-bound")),
                () -> assertTrue(html.contains("data-atf-scenario-collapse-bound")),
                () -> assertTrue(html.contains("function watchScenarioCards()")),
                () -> assertTrue(html.contains("response-evidence")),
                () -> assertTrue(html.contains("json-evidence")),
                () -> assertTrue(html.contains("assertion-evidence")),
                () -> assertTrue(html.contains("helm-scroll-block")),
                 () -> assertTrue(html.contains(".step-detail .se-toggle")),
                () -> assertTrue(html.contains("id=\"nav-fails\"")),
                () -> assertTrue(html.contains("id=\"nav-overview\"")),
                () -> assertTrue(html.contains("id=\"test-view\"")),
                () -> assertTrue(html.contains("id=\"category-view\"")),
                () -> assertTrue(html.contains("id=\"dashboard-view\"")),
                 () -> assertTrue(html.contains("class=\"test-list atf-fails-list\"")),
                 () -> assertTrue(html.contains("id=\"atf-failure-list\" class=\"test-list-item\"")),
                 () -> assertTrue(html.contains("id=\"atf-failure-detail\" class=\"detail-body\"")),
                 () -> assertTrue(html.contains("function selectFailure(index)")),
                () -> assertTrue(html.contains("function statusDisplay(value)")),
                () -> assertTrue(html.contains("function failureContext(item)")),
                () -> assertTrue(html.contains("var indexMarkup=row.getAttribute('data-atf-index-html')||''")),
                () -> assertTrue(html.contains("detail.insertAdjacentHTML('beforeend',indexMarkup)")),
                 () -> assertTrue(html.contains("function failureIndexMarkup(name,ctx,id,scenarioIndex,ordinal)")),
                 () -> assertTrue(html.contains("data-atf-failure-phase")),
                () -> assertTrue(html.contains("q('.card',item)")),
                () -> assertTrue(Boolean.TRUE.equals(ReportEvidenceStyleContract.inspectHtml(report, html, false)
                                .checks().get("specialized evidence")),
                        "the artifact must contain the real aggregate assertion table"),
                () -> assertTrue(html.contains("Array.prototype.slice.call(row.cells||[])")),
                () -> assertTrue(html.contains("Validate: status=")),
                () -> assertTrue(html.contains("ctx.validation")),
                () -> assertTrue(html.contains("FAILURE CAUSE")),
                () -> assertTrue(html.contains("class=\"test-wrapper row view test-view hide\"")),
                () -> assertTrue(html.contains("atf-overview-default")),
                () -> assertTrue(html.contains("data-atf-tab=\"overview\"")),
                () -> assertTrue(html.contains("data-atf-tab=\"fails\"")),
                () -> assertTrue(html.contains("atfOpenTest")),
                () -> assertTrue(html.contains("atf-failure-evidence")),
                () -> assertTrue(html.contains("data-atf-failure-phase")),
                () -> assertTrue(html.contains("normalizeCategoryLinks")),
                () -> assertTrue(html.contains("v.classList.toggle('d-none',!active)")),
                () -> assertTrue(html.contains("traceTables")),
                () -> assertTrue(html.contains("v==null?'':v")),
                () -> assertFalse(html.contains("url('fontawesome-webfont.woff2")),
                () -> assertFalse(html.contains("url('fontawesome-webfont.woff")),
                () -> assertFalse(html.contains("spark/")),
                () -> assertFalse(html.contains("class=\"collapse show\"")),
                () -> assertFalse(Files.exists(report.getParent().resolve("spark"))));
    }

    @Test
    void publishesTheSharedVisualContract() throws Exception {
        Path report = TestReportPaths.report("extent-api-theme-contract.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ThemeTests", "theme contract", List.of("reporting"));
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("--atf-surface:#101723")),
                () -> assertTrue(html.contains("line-height:1.55")),
                () -> assertTrue(html.contains("font-size:12.6px")),
                () -> assertTrue(html.contains("padding:7px 9px")),
                () -> assertTrue(html.contains("font-weight:400")),
                () -> assertTrue(html.contains("font-family:ui-monospace,Consolas,monospace")),
                () -> assertTrue(html.contains("pre.code-block")),
                () -> assertTrue(html.contains("padding:10px 12px!important")),
                () -> assertTrue(html.contains("--atf-json-text:#e6edf3")
                        && html.contains("color:var(--atf-json-text)!important")),
                () -> assertTrue(html.contains(".dark .test-contents pre.code-block")),
                () -> assertTrue(html.contains(".dark .card-header")),
                () -> assertTrue(html.contains("System/Environment")),
                 () -> assertTrue(html.contains("Runtime")),
                () -> assertEquals("Configured per execution", systemClient(html)),
                () -> assertFalse(html.contains("ATF ApiTransport"),
                        "A consumer without transport metadata must not claim an API client"),
                () -> assertTrue(html.contains("Extent · embedded offline HTML")),
                () -> assertTrue(html.contains(".card:has(>.card-body #grandchild-analysis)")),
                () -> assertTrue(html.contains("data-atf-node-role=\"phase\"]>.card-header .node-attr")),
                () -> assertTrue(html.contains("function normalizeDurations()")),
                () -> assertTrue(html.contains("function normalizeTestNavigatorSeparators()")
                        && html.contains("node.nodeType===3")
                        && html.contains("node.remove()"),
                        "Tests navigation must show duration without the visual slash separator"),
                 () -> assertTrue(html.contains("function atfFormatDuration(ms)")
                                 && html.contains("return String(value)+' ms'"),
                         "the shared duration formatter must carry the ms unit"),
                () -> assertTrue(html.contains("function aggregateClassDurations()")),
                 () -> assertTrue(html.contains("function markDuration(element,value,source)")
                                 && html.contains("data-duration-source',source")),
                () -> assertTrue(html.contains(".test-view .test-content-detail .card-toolbar .badge,.category-view .test-content-detail .card-toolbar .badge")
                                 && html.contains("/^(pass|fail|warning|skip)$/i.test(raw)")
                                 && html.contains("e.textContent=atfFormatDuration(ms)"),
                         "scenario and lifecycle toolbar durations must carry the ms unit without changing status badges"),
                () -> assertTrue(html.contains(".test-detail .text-sm span")),
                () -> assertTrue(html.contains("function collapseDirectScenarioCards()")),
                () -> assertTrue(html.contains("function directScenarioCards(root){return q('.test-contents > .mt-4 > .accordion > .card',root);}")),
                () -> assertTrue(html.contains("function testTags(item){var stored=item.getAttribute('data-atf-scenario-tags')")),
                () -> assertTrue(html.contains("function readTagValues(container)")),
                () -> assertTrue(html.contains(".trim().toLowerCase();}):readTagValues(item)")),
                () -> assertTrue(html.contains("replace(/^\\s*[^a-zA-Z0-9_-]\\s*/,'').trim().toLowerCase()")),
                () -> assertTrue(html.contains("data-atf-scenario-tags")),
                () -> assertTrue(html.indexOf("var captured=readTagValues(card)") < html.indexOf("source.remove()")),
                () -> assertTrue(html.contains("auditRows('atf-audit-tags',tags,false,total,false)")),
                () -> assertTrue(html.contains("data-atf-scenario-open")),
                () -> assertTrue(html.contains("data-atf-phase-open")),
                () -> assertTrue(containsJavaScriptSignature(html,
                                "function collapseScenarioCards(force,root,open)")
                                && html.contains("scenario.querySelectorAll(':scope [data-atf-node-role]")),
                () -> assertFalse(html.contains("--atf-surface:#141d2b")),
                // The Fails navigation intentionally uses the compact 1.45
                // line-height from the .NET reference; only the old global
                // theme line-height is forbidden here.
                () -> assertTrue(html.contains(".exception-view.atf-fails-view .test-item .test-detail .name")));
    }

    @Test
    void overviewAuditMaterializesScenarioTagsBeforeProjectingTheOtherTagsTable() throws Exception {
        Path report = TestReportPaths.report("extent-api-tag-audit.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("UsersApiTests", "GET users — collection contract",
                    List.of("api", "users", "contract"));
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        int populate = html.indexOf("function populateAudit()");
        int items = html.indexOf("items=testItems()", populate);
        int normalize = html.indexOf("normalizeScenarioAnnotations();", items);
        int total = html.indexOf("var total=items.length", normalize);

        assertTrue(populate >= 0, "the overview must contain the tag-audit projection");
        assertTrue(items >= 0 && normalize > items && normalize < total,
                "scenario annotations must be materialized after scenario discovery and before the audit reads scenario tags");
        assertTrue(html.contains("data-atf-scenario-tags"),
                "the audit must have a stable scenario-tag projection after native annotations are normalized");
        assertTrue(html.contains("auditRows('atf-audit-tags',tags,false,total,false)"),
                "the overview must render the projected tag rows");
    }

    @Test
    void failurePublicationKeepsEveryFailureAndADeepLinkContract() throws Exception {
        Path report = TestReportPaths.report("extent-api-failure-report.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("FailingApiTests", "GET user — invalid contract", List.of("api", "failure"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest("invalid contract", HttpMethod.GET.value(), UNKNOWN_USER_PATH);
            request.detail("response.status", "500");
            request.detail("response.duration.ms", "12");
            var validation = request.startValidation("Validate status=404");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "404", "500", false)));
            validation.complete(AtfReportStatus.FAIL);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("FailingApiTests")),
                () -> assertTrue(html.contains("status=\"fail\"")),
                () -> assertTrue(html.contains("function renderFails()")),
                () -> assertTrue(html.contains("function failedAssertionRows(item)")),
                () -> assertTrue(html.contains("function failureContextFor(item,anchor)")),
                 () -> assertTrue(html.contains("var durationMs=atfDurationValue(rawDuration)")
                        && html.contains("duration=durationMs===null?rawDuration:atfFormatDuration(durationMs)")),
                 () -> assertTrue(html.contains("function failedUiEvidence(card)")
                                 && html.contains("function failureTransportEvidence(card,state)"),
                         "the failure projection must detect concrete UI evidence separately from transport failures"),
                 () -> assertTrue(html.contains("ctx.ui?ctx.step:ctx.request"),
                         "the Fails index must use STEP for UI failures and REQUEST for API failures"),
                 () -> assertTrue(html.contains("(ctx.ui?'':'<div class=\"atf-failure-index-row\"><span class=\"atf-failure-label\">DURATION"),
                         "response duration is an API-only failure field; UI failures must not receive a transport placeholder"),
                 () -> assertTrue(html.contains("if(!context.ui&&requests.length)"),
                         "phase scoping must keep transport fields out of UI failure diagnostics"),
                () -> assertTrue(html.contains("data-atf-failure-ordinal")),
                () -> assertTrue(html.contains("data-atf-failure-id")),
                 () -> assertTrue(html.contains("failureIndexMarkup(name,ctx,id,scenarioIndex,record.ordinal)"),
                         "Fails must expose selected scenario evidence"),
                 () -> assertTrue(html.contains("detail.insertAdjacentHTML('beforeend',indexMarkup)"),
                         "Fails must expose concrete index evidence"),
                 () -> assertFalse(html.contains("sourceScenario?sourceScenario.cloneNode(true)"),
                         "Fails must not duplicate the full test tree below Open in Tests"),
                () -> assertTrue(html.contains("function activateFromHash()")),
                () -> assertTrue(html.contains("window.addEventListener('hashchange',activateFromHash)")),
                () -> assertTrue(html.contains("function normalizeNavigationLinks()")),
                () -> assertTrue(html.contains("activateFromHash();},0)")),
                () -> assertTrue(html.contains("var hash='test-id='+id")),
                () -> assertTrue(html.contains("scenario-index=")),
                () -> assertTrue(html.contains("data-atf-scenario-index")),
                () -> assertTrue(html.indexOf("id=\"atf-report-behaviour\"") < html.indexOf("</head>")),
                () -> assertTrue(html.contains("id=\"atf-report-behaviour\" type=\"text/javascript\"")),
                () -> assertTrue(html.contains("Validate status=404")),
                () -> assertTrue(html.contains("EXPECTED")),
                () -> assertTrue(html.contains("ACTUAL")),
                () -> assertFalse(html.contains("spark/")),
                () -> assertFalse(Files.exists(report.getParent().resolve("spark"))));
    }

    @Test
    void evidenceBoundaryRedactsDetailsJsonAndAssertionValues() throws Exception {
        Path report = TestReportPaths.report("extent-api-redaction-report.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("SecurityTests", "redaction boundary", List.of("security"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest("secret response", HttpMethod.GET.value(), SECURE_PATH);
            request.detail("Authorization", "Bearer raw-token-123");
            request.json("RESPONSE JSON", "{\"token\":\"raw-json-token\",\"name\":\"safe\"}");
            var validation = request.startValidation("Validate secure response");
            validation.assertions(List.of(new AtfAssertion(
                    "token", "raw-expected-token", "raw-actual-token", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertFalse(html.contains("raw-token-123")),
                () -> assertFalse(html.contains("raw-json-token")),
                () -> assertFalse(html.contains("raw-expected-token")),
                () -> assertFalse(html.contains("raw-actual-token")),
                () -> assertTrue(html.contains("***REDACTED***")));
    }

    @Test
    void retryEvidenceIsRenderedByTheEvidenceBoundaryAndRedactedBeforePublication() throws Exception {
        Path report = TestReportPaths.report("extent-api-retry-evidence.html");
        var response = new ApiResponse(200, "{\"status\":\"ok\"}", Map.of(),
                Duration.ofMillis(8), ApiReportTestUrls.apiExample(SECURE_PATH + "?token=raw-final-token"));
        var execution = new ApiExecutionResult(response, 2, Duration.ofMillis(13),
                List.of(new RetryEvent(2, 503, "Retry-After", Duration.ZERO, "retry token=raw-event-token")),
                List.of(
                        new ApiAttemptRecord(1, 503, Duration.ofMillis(5), ApiAttemptOutcome.RETRYABLE_RESPONSE,
                                "retry token=raw-attempt-token"),
                        new ApiAttemptRecord(2, 200, Duration.ofMillis(8), ApiAttemptOutcome.SUCCESS, "")));

        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("RetryTests", "retry evidence", List.of("reporting"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest("secure call", HttpMethod.GET.value(), SECURE_PATH);
            request.execution(execution);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("retry-history-table")),
                () -> assertTrue(html.contains("RETRYABLE_RESPONSE")),
                () -> assertFalse(html.contains("raw-final-token")),
                () -> assertTrue(html.contains("token=***REDACTED***"),
                        "the URL may retain the query key while the secret value must be redacted"),
                () -> assertFalse(html.contains("raw-event-token")),
                () -> assertFalse(html.contains("raw-attempt-token")),
                () -> assertTrue(html.contains("<td>RETRY EVENTS</td><td>1</td>")));
    }

    @Test
    void failureCauseIsRedactedAtTheExtentPhaseBoundaryBeforePublication() throws Exception {
        Path report = TestReportPaths.report("extent-api-failure-cause-redaction.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("FailureTests", "redacted failure", List.of("security"));
            var body = scenario.startPhase("TEST BODY");
            body.detail("Failure cause", "ApiValidationException: token=raw-failure-token; mode=full");
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("<td>Failure cause</td>"),
                        "the actual diagnostic row must exist, not merely a label in bundled JavaScript"),
                () -> assertTrue(html.contains("token=***REDACTED***")),
                () -> assertTrue(html.contains("mode=full")),
                () -> assertFalse(html.contains("raw-failure-token")));
    }

    @Test
    void authorizationInFailureCauseIsRedactedBeforePublication() throws Exception {
        Path report = TestReportPaths.report("extent-api-authorization-failure-cause-redaction.html");
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("FailureTests", "redacted authorization failure", List.of("security"));
            var body = scenario.startPhase("TEARDOWN");
            body.detail("Failure cause",
                    "teardown cleanup failed: Authorization: Bearer teardown-secret-token-1234567890");
            body.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
        }

        String html = Files.readString(report);
        assertAll(
                () -> assertTrue(html.contains("teardown cleanup failed")),
                () -> assertTrue(html.contains("Authorization: Bearer ***REDACTED***")),
                () -> assertFalse(html.contains("teardown-secret-token-1234567890")));
    }

    private static int divDepthAt(String html, int markerIndex) {
        String prefix = html.substring(0, markerIndex);
        int depth = 0;
        for (int index = 0; index < prefix.length(); index++) {
            if (prefix.startsWith("<div", index)) depth++;
            if (prefix.startsWith("</div>", index)) depth--;
        }
        return depth;
    }

    private static boolean containsJavaScriptSignature(String html, String signature) {
        return compactJavaScript(html).contains(compactJavaScript(signature));
    }

    private static String compactJavaScript(String value) {
        return value.replaceAll("\\s+", "");
    }
}


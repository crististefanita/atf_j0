package atf.reporting.extent.session;
import atf.reporting.console.AtfConsoleSink;
import atf.testkit.AtfUnitTest;

import atf.reporting.support.TestReportPaths;
import atf.reporting.support.ApiReportTestUrls;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.console.AtfConsoleEvent;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.security.SensitiveDataRedactor;
import com.aventstack.extentreports.ExtentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ExtentReportSessionLifecycleTest {
    @Test
    void semanticStartFailureRollsBackExtentScenarioAndAllowsPublication() throws Exception {
        Path root = Files.createTempDirectory("atf-scenario-start-rollback-");
        Path report = root.resolve("scenario-rollback.html");
        try {
            var cause = new IllegalStateException("scenario semantic start failed");
            var probe = new LifecycleStartProbe() {
                private boolean first = true;
                @Override public void beforeScenarioSemantic() {
                    if (first) { first = false; throw cause; }
                }
                @Override public void beforePhaseSemantic(ExtentTest phase) { }
            };
            try (var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                    new SensitiveDataRedactor(), event -> { }, Map.of(),
                    ReportUiActionDetailsMode.INCLUDED_COLLAPSED, probe)) {
                assertSame(cause, assertThrows(IllegalStateException.class, () ->
                        session.startScenario("RollbackTests", "orphan scenario", List.of())));
                assertEquals(0, session.semanticSnapshot().scenarioCount());
                var valid = session.startScenario("RollbackTests", "valid scenario", List.of());
                valid.complete(AtfReportStatus.PASS);
                assertEquals(1, session.semanticSnapshot().scenarioCount());
            }
            String html = Files.readString(report);
            assertTrue(html.contains("valid scenario"));
            assertFalse(html.contains("orphan scenario"));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void semanticPhaseFailureRollsBackExtentChildAndKeepsScenarioUsable() throws Exception {
        Path root = Files.createTempDirectory("atf-phase-start-rollback-");
        Path report = root.resolve("phase-rollback.html");
        try {
            var cause = new IllegalStateException("phase semantic start failed");
            var probe = new LifecycleStartProbe() {
                private boolean first = true;
                @Override public void beforeScenarioSemantic() { }
                @Override public void beforePhaseSemantic(ExtentTest phase) {
                    if (first) { first = false; throw cause; }
                }
            };
            try (var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                    new SensitiveDataRedactor(), event -> { }, Map.of(),
                    ReportUiActionDetailsMode.INCLUDED_COLLAPSED, probe)) {
                var scenario = session.startScenario("RollbackTests", "phase recovery", List.of());
                assertSame(cause, assertThrows(IllegalStateException.class,
                        () -> scenario.startPhase("ORPHAN PHASE")));
                assertEquals(0, session.semanticSnapshot().phaseCount());
                var valid = scenario.startPhase("TEST BODY");
                valid.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
                assertEquals(1, session.semanticSnapshot().phaseCount());
            }
            String html = Files.readString(report);
            assertTrue(html.contains("TEST BODY"));
            assertFalse(html.contains("ORPHAN PHASE"));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void rollbackFailureIsSuppressedWithoutMaskingTheOriginalStartFailure() throws Exception {
        Path root = Files.createTempDirectory("atf-phase-rollback-secondary-");
        Path report = root.resolve("phase-secondary.html");
        try {
            var cause = new IllegalStateException("semantic start is the primary failure");
            var probe = new LifecycleStartProbe() {
                @Override public void beforeScenarioSemantic() { }
                @Override public void beforePhaseSemantic(ExtentTest phase) {
                    // Simulate an already-detached vendor node so compensation itself fails.
                    phase.getModel().getParent().getChildren().remove(phase.getModel());
                    throw cause;
                }
            };
            try (var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                    new SensitiveDataRedactor(), event -> { }, Map.of(),
                    ReportUiActionDetailsMode.INCLUDED_COLLAPSED, probe)) {
                var scenario = session.startScenario("RollbackTests", "secondary failure", List.of());
                var failure = assertThrows(IllegalStateException.class,
                        () -> scenario.startPhase("DETACHED PHASE"));
                assertSame(cause, failure);
                assertEquals(1, failure.getSuppressed().length);
                assertTrue(failure.getSuppressed()[0].getMessage()
                        .contains("Extent phase was not attached"));
                scenario.complete(AtfReportStatus.FAIL);
            }
            assertFalse(Files.readString(report).contains("DETACHED PHASE"));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void invalidTagCollectionCannotLeaveAnOrphanExtentOrSemanticScenario() throws Exception {
        Path root = Files.createTempDirectory("atf-invalid-scenario-tags-");
        Path report = root.resolve("atomic-start.html");
        try {
            try (var session = new ExtentReportSession(report)) {
                var invalidTags = new AbstractCollection<String>() {
                    @Override public Iterator<String> iterator() {
                        throw new IllegalStateException("tag source unavailable");
                    }
                    @Override public int size() { return 1; }
                };

                assertThrows(IllegalStateException.class, () ->
                        session.startScenario("AtomicStartTests", "must not appear", invalidTags));
                assertEquals(0, session.semanticSnapshot().scenarioCount());

                var valid = session.startScenario("AtomicStartTests", "valid scenario", List.of());
                valid.complete(AtfReportStatus.PASS);
                assertEquals(1, session.semanticSnapshot().scenarioCount());
            }

            String html = Files.readString(report);
            assertAll(
                    () -> assertTrue(html.contains("valid scenario")),
                    () -> assertFalse(html.contains("must not appear")));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void consoleSinkFailureCannotInterruptScenarioLifecycleOrReportPublication() throws Exception {
        Path root = Files.createTempDirectory("atf-console-sink-lifecycle-");
        Path report = root.resolve("console-failure.html");
        try {
            var throwingConsole = (AtfConsoleSink) event -> {
                throw new IllegalStateException("console unavailable");
            };
            try (var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                    new SensitiveDataRedactor(), throwingConsole)) {
                var scenario = session.startScenario("ConsoleFailureTests", "report lifecycle survives",
                        List.of("reporting"));
                var phase = scenario.startPhase("TEST BODY");
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }

            String html = Files.readString(report);
            assertAll(
                    () -> assertTrue(Files.isRegularFile(report)),
                    () -> assertTrue(html.contains("report lifecycle survives")),
                    () -> assertTrue(html.contains("ConsoleFailureTests")));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void extentJsonBodyUsesConfiguredLimitAfterRedactionAndKeepsCodeBlockContract() throws Exception {
        Path root = Files.createTempDirectory("atf-extent-body-limit-");
        Path report = root.resolve("bounded.html");
        try {
            try (var session = new ExtentReportSession(report, new ReportingSettings(240, 40))) {
                var scenario = session.startScenario("BodyTests", "bounded response", List.of("api"));
                var phase = scenario.startPhase("TEST BODY");
                var request = phase.startRequest("large response", "GET", ApiReportTestUrls.GENERIC_USERS);
                request.json("RESPONSE JSON", "{\"api_key\":\"raw-token\",\"payload\":\"012345678901234567890123456789\"}");
                request.complete(AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);
            }

            var html = Files.readString(report);

            assertAll(
                    () -> assertTrue(html.contains("response-json-body")),
                    () -> assertTrue(html.contains("helm-scroll-block-body")),
                    () -> assertTrue(html.contains("… [truncated; showing first 40 of ")),
                    () -> assertTrue(html.contains("***REDACTED***")),
                    () -> assertFalse(html.contains("raw-token")),
                    () -> assertFalse(html.contains("012345678901234567890123456789\"}")));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void flushKeepsTheFinalPathPrivateUntilClosePublishesTheCompleteArtifact() throws Exception {
        Path root = Files.createTempDirectory("atf-incomplete-flush-");
        Path report = root.resolve("report.html");
        try {
            try (var session = new ExtentReportSession(report)) {
                var scenario = session.startScenario("FlushTests", "incomplete flush", List.of("reporting"));
                scenario.complete(AtfReportStatus.PASS);
                session.flush();

                assertFalse(Files.exists(report), "flush must not expose the private staged report");
                try (var children = Files.list(root)) {
                    assertTrue(children.anyMatch(path ->
                            path.getFileName().toString().startsWith(".atf-extent-session-")));
                }
            }
            assertTrue(Files.exists(report), "close must publish the finalized report");
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void publishedArtifactEventIsEmittedOnceAfterTheFinalFileExists() throws Exception {
        Path root = Files.createTempDirectory("atf-report-published-event-");
        Path report = root.resolve("published.html");
        List<AtfConsoleEvent> events = new ArrayList<>();
        try {
            var session = new ExtentReportSession(report, ReportingSettings.defaults(),
                    new SensitiveDataRedactor(), event -> {
                        if (event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED) {
                            assertTrue(Files.isRegularFile(report), "notification must follow final publication");
                        }
                        events.add(event);
                    });
            var scenario = session.startScenario("PublishedEventTests", "published event", List.of());
            scenario.complete(AtfReportStatus.PASS);

            session.close();
            session.close();

            var published = events.stream()
                    .filter(event -> event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED)
                    .toList();
            assertEquals(1, published.size());
            assertEquals(report.toAbsolutePath().normalize().toString(), published.getFirst().detailValue());
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void failedPublicationDoesNotEmitPublishedArtifactEvent() throws Exception {
        Path root = Files.createTempDirectory("atf-report-published-event-failure-");
        Path blockedDestination = Files.createDirectory(root.resolve("blocked.html"));
        List<AtfConsoleEvent> events = new ArrayList<>();
        try {
            var session = new ExtentReportSession(blockedDestination, ReportingSettings.defaults(),
                    new SensitiveDataRedactor(), events::add);
            var scenario = session.startScenario("PublishedEventTests", "blocked publication", List.of());
            scenario.complete(AtfReportStatus.PASS);

            assertThrows(IllegalStateException.class, session::close);
            assertTrue(events.stream().noneMatch(event ->
                    event.type() == AtfConsoleEvent.Type.REPORT_PUBLISHED));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void configuredSessionPublishesOneTimestampedExtentArtifact() throws Exception {
        Path root = Files.createTempDirectory("atf-configured-timestamped-report-");
        try {
            Path config = root.resolve("atf.yaml");
            Files.writeString(config, """
                    reporting:
                      extent:
                        outputDirectory: reports
                        fileName: api-atf-extent-report.html
                        timestampEnabled: true
                        timestampPattern: yyyy-MM-dd_HH-mm-ss-SSS
                        timestampZone: UTC
                    """);

            Path reportPath;
            try (var session = ExtentReportSession.openConfigured(config)) {
                reportPath = session.reportPath();
                var scenario = session.startScenario("ConfiguredReportTests", "timestamped report", List.of("reporting"));
                scenario.complete(AtfReportStatus.PASS);

                assertTrue(reportPath.getFileName().toString()
                        .matches("api-atf-extent-report_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3}\\.html"));
                assertFalse(Files.exists(reportPath), "the final artifact is published only on close");
            }

            assertTrue(Files.exists(reportPath));
            assertTrue(Files.readString(reportPath).contains("timestamped report"));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void parallelSessionsPublishIndependentAutonomousReports() throws Exception {
        Path root = Files.createTempDirectory("atf-parallel-reports-");
        Path first = root.resolve("parallel-one.html");
        Path second = root.resolve("parallel-two.html");
        try {
            try (var executor = Executors.newFixedThreadPool(2)) {
                var firstRun = executor.submit(() -> publishParallelScenario(first, "parallel one"));
                var secondRun = executor.submit(() -> publishParallelScenario(second, "parallel two"));
                firstRun.get(30, TimeUnit.SECONDS);
                secondRun.get(30, TimeUnit.SECONDS);
            }

            String firstHtml = Files.readString(first);
            String secondHtml = Files.readString(second);
            boolean sessionDirectoriesRemain;
            try (var children = Files.list(root)) {
                sessionDirectoriesRemain = children.anyMatch(path ->
                        path.getFileName().toString().startsWith(".atf-extent-session-"));
            }
            assertTrue(Files.exists(first));
            assertTrue(Files.exists(second));
            assertAll(
                    () -> assertTrue(firstHtml.contains("parallel one")),
                    () -> assertTrue(secondHtml.contains("parallel two")),
                    () -> assertFalse(firstHtml.contains("spark/")),
                    () -> assertFalse(secondHtml.contains("spark/")),
                    () -> assertFalse(sessionDirectoriesRemain));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void sessionRejectsCrossThreadMutationInsteadOfRacingExtentState() throws Exception {
        Path report = TestReportPaths.report("extent-thread-confined.html");
        try (var session = new ExtentReportSession(report);
             var executor = Executors.newSingleThreadExecutor()) {
            var failure = executor.submit(() -> assertThrows(IllegalStateException.class,
                    () -> session.startScenario("ParallelTests", "cross-thread mutation", List.of("parallel"))))
                    .get(10, TimeUnit.SECONDS);
            assertNotNull(failure);
        }
    }

    @Test
    void competingSessionsKeepTheSharedFinalPathAtomic() throws Exception {
        Path root = Files.createTempDirectory("atf-shared-report-");
        Path report = root.resolve("shared.html");
        try {
            try (var executor = Executors.newFixedThreadPool(2)) {
                var firstRun = executor.submit(() -> publishParallelEvidenceScenario(report, "shared one"));
                var secondRun = executor.submit(() -> publishParallelEvidenceScenario(report, "shared two"));
                firstRun.get(30, TimeUnit.SECONDS);
                secondRun.get(30, TimeUnit.SECONDS);
            }

            var inspection = ReportArtifactInspector.inspect(report);
            assertTrue(inspection.isCompliant(), () -> "Shared report violations: " + inspection.violations());
            String html = Files.readString(report);
            assertTrue(html.contains("shared one") || html.contains("shared two"));
            try (var children = Files.list(root)) {
                assertFalse(children.anyMatch(path ->
                        path.getFileName().toString().startsWith(".atf-extent-session-")));
            }
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void flushKeepsIntermediateStatePrivateUntilClosePublishesTheAutonomousArtifact() throws Exception {
        Path root = Files.createTempDirectory("atf-flush-boundary-");
        Path report = root.resolve("flush-boundary.html");
        try {
            var session = new ExtentReportSession(report);
            try {
                var scenario = session.startScenario("FlushTests", "flush does not publish partial output",
                        List.of("reporting", "lifecycle"));
                var phase = scenario.startPhase("TEST BODY");
                phase.startRequest("health", "GET", ApiReportTestUrls.GENERIC_HEALTH).complete(AtfReportStatus.PASS);
                phase.complete(AtfReportStatus.PASS);
                scenario.complete(AtfReportStatus.PASS);

                session.flush();

                assertFalse(Files.exists(report), "flush must not publish a partial final artifact");
                try (var children = Files.list(root)) {
                    assertTrue(children.anyMatch(path ->
                            path.getFileName().toString().startsWith(".atf-extent-session-")),
                            "flush should keep the private staging workspace alive until close");
                }
            } finally {
                session.close();
            }

            assertTrue(Files.isRegularFile(report));
            assertTrue(Files.readString(report).contains("flush does not publish partial output"));
            assertFalse(Files.exists(root.resolve("spark")));
            try (var children = Files.list(root)) {
                assertFalse(children.anyMatch(path ->
                        path.getFileName().toString().startsWith(".atf-extent-session-")));
            }
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void failedPublicationStillCleansPrivateWorkspaceAndPreservesBlockedDestination() throws Exception {
        Path root = Files.createTempDirectory("atf-blocked-publication-");
        Path blockedDestination = root.resolve("blocked-report");
        Files.createDirectories(blockedDestination);
        Path sentinel = blockedDestination.resolve("sentinel.txt");
        Files.writeString(sentinel, "destination-owned");
        try {
            var session = new ExtentReportSession(blockedDestination);
            var scenario = session.startScenario("PublicationTests", "blocked destination", List.of("reporting"));
            scenario.complete(AtfReportStatus.PASS);

            assertThrows(IllegalStateException.class, session::close,
                    "a non-empty destination directory must not be replaced by a report file");

            assertTrue(Files.isDirectory(blockedDestination));
            assertEquals("destination-owned", Files.readString(sentinel));
            try (var children = Files.list(root)) {
                assertFalse(children.anyMatch(path ->
                        path.getFileName().toString().startsWith(".atf-extent-session-")),
                        "a failed publication must not leak the private workspace");
            }
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void cleanupFailureIsExplicitButNeverLeavesAPartiallyPromotedReport() throws Exception {
        Path root = Files.createTempDirectory("atf-asset-cleanup-failure-");
        Path report = root.resolve("report.html");
        Path spark = Files.createDirectory(root.resolve("spark"));
        Files.writeString(spark.resolve("spark-style.css"), ".spark{color:red}");
        Files.writeString(report, "<!doctype html><html><head>"
                + "<link rel=\"stylesheet\" href=\"spark/spark-style.css\">"
                + "</head><body class=\"spa -report dark\">"
                + "<div class=\"test-wrapper row view test-view\"></div>"
                + "<div class=\"test-wrapper row view category-view attributes-view\"></div>"
                + "<div class=\"container-fluid p-4 view dashboard-view\"></div>"
                + "</body></html>");
        try {
            var failure = assertThrows(AccessDeniedException.class, () ->
                    ExtentReportPublicationPipeline.publish(report, assetDirectory -> {
                        throw new AccessDeniedException(assetDirectory.toString(), null,
                                "simulated locked vendor asset");
                    }));

            String published = Files.readString(report);
            assertTrue(failure.getMessage().contains(spark.toString()));
            assertAll(
                    () -> assertTrue(published.startsWith("<!doctype html>")),
                    () -> assertTrue(published.contains("data-atf-inline=\"spark-style.css\"")),
                    () -> assertTrue(published.contains("id=\"atf-report-theme\"")),
                    () -> assertTrue(published.contains("id=\"atf-report-behaviour\"")),
                    () -> assertTrue(published.endsWith("</html>")),
                    () -> assertTrue(Files.exists(spark.resolve("spark-style.css"))),
                    () -> assertNoPublicationTemporaryFiles(root));
        } finally {
            deleteTree(root);
        }
    }

    private static void assertNoPublicationTemporaryFiles(Path root) throws Exception {
        try (var children = Files.list(root)) {
            assertFalse(children.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")),
                    "atomic publication must remove its temporary output after cleanup failure");
        }
    }

    private static void publishParallelScenario(Path report, String name) {
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ParallelTests", name, List.of("parallel"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest("health", "GET", ApiReportTestUrls.GENERIC_HEALTH);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }

    private static void publishParallelEvidenceScenario(Path report, String name) {
        try (var session = new ExtentReportSession(report)) {
            var scenario = session.startScenario("ParallelTests", name, List.of("parallel"));
            var body = scenario.startPhase("TEST BODY");
            var request = body.startRequest("health", "GET", ApiReportTestUrls.GENERIC_HEALTH);
            request.detail("response.duration.ms", "1");
            request.json("RESPONSE JSON", "{\"status\":\"ok\"}");
            var validation = request.startValidation("Validate health response");
            validation.assertions(List.of(new AtfAssertion("HTTP status", "200", "200", true)));
            validation.complete(AtfReportStatus.PASS);
            request.complete(AtfReportStatus.PASS);
            body.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException error) {
                    throw new RuntimeException(error);
                }
            });
        }
    }

}


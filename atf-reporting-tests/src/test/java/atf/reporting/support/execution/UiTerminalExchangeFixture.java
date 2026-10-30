package atf.reporting.support.execution;

import atf.reporting.api.*;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.TestReportPaths;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.reporting.contract.AtfReportStatus.*;

/** Immutable captured facts; this fixture never starts an HTTP client. */
public final class UiTerminalExchangeFixture {
    public static final String SECRET = "private-terminal-fixture-password";
    public enum Mode { CHECKPOINT, SETUP, FINAL, TRACE, UNIFIED, CROSS_PHASE }
    public record Case(Mode mode, String outcome, AtfReportStatus expected, int attempts) {
        public String name() { return mode + " terminal " + (outcome.isEmpty() ? "unclassified" : outcome) + " " + expected; }
    }
    public record Artifact(Path path, AtfReportSemanticSnapshot snapshot, List<Case> cases) { }

    private UiTerminalExchangeFixture() { }

    public static Artifact publish(boolean fallback, int limit) {
        var file = TestReportPaths.report("ui-terminal-" + fallback + "-" + limit + "-" + UUID.randomUUID() + ".html");
        var cases = new ArrayList<Case>();
        for (var mode : Mode.values()) cases.add(new Case(mode, "IO_ERROR", FAIL, 2));
        cases.addAll(List.of(new Case(Mode.TRACE, "TIMEOUT", FAIL, 2), new Case(Mode.TRACE, "CANCELLED", SKIP, 0),
                new Case(Mode.TRACE, "", WARNING, 2), new Case(Mode.TRACE, "IO_ERROR", PASS, 2)));
        AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, new ReportingSettings(limit))
                : new ExtentReportSession(file, new ReportingSettings(limit));
        try (session) {
            for (var item : cases) publish(session, item);
        }
        var snapshot = fallback ? ((HtmlReportSession) session).semanticSnapshot()
                : ((ExtentReportSession) session).semanticSnapshot();
        return new Artifact(file, snapshot, List.copyOf(cases));
    }

    private static void publish(AtfReportPorts.Session session, Case item) {
        var profile = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.NONE)
                .flowMode(item.mode() == Mode.UNIFIED || item.mode() == Mode.CROSS_PHASE
                        ? ReportEvidenceFlowMode.UNIFIED_FLOW : ReportEvidenceFlowMode.SCREEN_CHECKPOINTS).build();
        var raw = session.startScenario("UiTerminalExchange", item.name(), List.of("component", "synthetic-terminal"));
        var scenario = UiExtentReportSession.adaptScenario(raw, profile, item.name());
        if (item.mode() == Mode.CROSS_PHASE) {
            scenario.unifyAcrossPhases();
            try (var setup = scenario.startPhase("SETUP")) {
                setup.setup(THEN, "the local fixture is ready", "/fixture", List.of(), PASS);
                setup.complete(PASS);
            }
        }
        var evidence = terminal(item);
        var comparisons = item.expected() == PASS
                ? List.of(new AtfAssertion("Expected terminal outcome", "IO_ERROR", evidence.termination().orElseThrow().outcome(), true))
                : List.<AtfAssertion>of();
        var terminal = item.expected() == PASS ? new UiApiValidationEvidence.Exchange(evidence, comparisons)
                : new UiApiValidationEvidence.Exchange(evidence, comparisons, item.expected());
        var api = new UiApiValidationEvidence(List.of(), List.of(sibling("/before"), terminal, sibling("/after")));
        try (var phase = scenario.startPhase(item.mode() == Mode.SETUP ? "SETUP" : "TEST BODY")) {
            switch (item.mode()) {
                case SETUP -> phase.setup(THEN, "terminal API evidence is retained", "/fixture", List.of(), List.of(), api, PASS);
                case FINAL -> phase.uiValidation("terminal API evidence is retained", "/fixture", "", List.of(), List.of(),
                        api, ReportScreenshotPolicy.CapturePoint.FINAL, PASS);
                case TRACE -> phase.apiTrace(THEN, "terminal API evidence is retained", "/fixture", api, PASS);
                default -> phase.step(THEN, "terminal API evidence is retained", "/fixture", "", List.of(), List.of(),
                        api, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, PASS);
            }
            if (item.mode() == Mode.CROSS_PHASE) phase.completeFinal(PASS);
            else phase.complete(PASS);
        }
        scenario.complete(PASS);
    }

    private static UiApiValidationEvidence.Exchange sibling(String path) {
        var response = new ReportResponse(200, "{\"ok\":true}", Map.of("Content-Type", "application/json"),
                Duration.ofMillis(3), "https://fixture.invalid" + path);
        var evidence = new ReportExecutionEvidence(response, 1, Duration.ofMillis(3), List.of(), List.of(),
                ReportRequest.builder("GET", path).build(), ReportTransportProvenance.ACCEPTANCE_MOCK);
        return new UiApiValidationEvidence.Exchange(evidence, List.of());
    }

    private static ReportExecutionEvidence terminal(Case item) {
        var evidence = new ReportExecutionEvidence(null, item.attempts(), Duration.ofMillis(item.attempts() == 0 ? 0 : 14),
                item.attempts() == 0 ? List.of() : List.of(new ReportRetryEvent(2, 503, "", Duration.ofMillis(5), "configured retry")),
                item.attempts() == 0 ? List.of() : List.of(
                        new ReportAttemptRecord(1, 503, Duration.ofMillis(2), "RETRYABLE_RESPONSE", "configured retry"),
                        new ReportAttemptRecord(2, -1, Duration.ofMillis(7), "FAILED", "no terminal response")),
                ReportRequest.builder("GET", "/terminal").build(), ReportTransportProvenance.LIVE_HTTP);
        return item.outcome().isEmpty() ? evidence : evidence.withTermination(item.outcome(),
                "controlled terminal failure; password=" + SECRET + "; " + "safe diagnostic context ".repeat(35));
    }
}

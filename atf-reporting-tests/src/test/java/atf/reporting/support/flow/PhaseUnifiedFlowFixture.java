package atf.reporting.support.flow;

import atf.reporting.api.ReportModelAdapters;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.support.MockApiReportContractFixtures;
import atf.reporting.support.MockApiReportExecution;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Phase-local aggregation remains distinct from cross-phase compaction. */
public final class PhaseUnifiedFlowFixture {
    public enum Variant { UI, UI_FAILURE, API, API_FAILURE }
    private PhaseUnifiedFlowFixture() { }

    public static BodyOnlyUnifiedFlowFixture.Published publish(Path root, boolean fallback,
                                                               int limit, Variant variant) throws Exception {
        Files.createDirectories(root);
        Path file = root.resolve("phase-flow-" + fallback + "-" + variant + "-" + System.nanoTime() + ".html");
        var profile = ReportEvidenceProfile.builder().flowMode(ReportEvidenceFlowMode.UNIFIED_FLOW)
                .screenshotPolicy(ReportScreenshotPolicy.FINAL_ONLY).build();
        var settings = new ReportingSettings(limit);
        var status = variant == Variant.UI_FAILURE || variant == Variant.API_FAILURE
                ? AtfReportStatus.FAIL : AtfReportStatus.PASS;
        atf.reporting.semantic.AtfReportSemanticSnapshot snapshot;
        Path published;
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file, settings)
                : new ExtentReportSession(file, settings)) {
            published = session.reportPath();
            var scenario = UiExtentReportSession.adaptScenario(session.startScenario("PhaseFlowFixture",
                    "atf.reporting.fixture", "phase-local purchase", List.of("component")), profile, "phase-local purchase");
            var body = scenario.startPhase("TEST BODY");
            body.step(AtfGherkinKeyword.WHEN, "product is selected", "#product", "",
                    List.of(new AtfAssertion("Product retained", "SKU-42", "SKU-42", true)),
                    List.of(action(1, "Select product", false), action(2, "Observe quantity", true)),
                    apiEvidence(variant), ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            String value = "v".repeat(600);
            body.uiValidation("purchase is confirmed", "#confirmation", ReportTestScreenshots.SECOND_PIXEL_PNG,
                    List.of(new AtfAssertion("Order retained", value + "expected-tail",
                            value + (variant == Variant.UI_FAILURE ? "rejected-tail" : "expected-tail"),
                            variant != Variant.UI_FAILURE)), List.of(action(3, "Confirm order", false)),
                    ReportScreenshotPolicy.CapturePoint.FINAL,
                    variant == Variant.UI_FAILURE ? AtfReportStatus.FAIL : AtfReportStatus.PASS);
            body.completeFinal(status);
            scenario.complete(status);
            snapshot = session instanceof ExtentReportSession extent ? extent.semanticSnapshot()
                    : ((HtmlReportSession) session).semanticSnapshot();
        }
        System.out.println("[ATF] Phase-local flow report: " + published.toAbsolutePath());
        return new BodyOnlyUnifiedFlowFixture.Published(published, snapshot, Files.readString(published));
    }

    private static AtfUiActionRecord action(int sequence, String title, boolean observation) {
        return new AtfUiActionRecord(sequence, title, "#target", "ready", "ready", 3, AtfReportStatus.PASS,
                observation ? AtfUiEvidenceSource.OBSERVE : AtfUiEvidenceSource.ACTION);
    }

    /** A conflicting compact marker must never hide an actual failed HTTP exchange. */
    public static BodyOnlyUnifiedFlowFixture.Published publishConflictingContext(Path root, boolean fallback) throws Exception {
        Files.createDirectories(root);
        Path file = root.resolve("conflicting-flow-" + fallback + "-" + System.nanoTime() + ".html");
        var exchange = apiEvidence(Variant.API_FAILURE).exchanges().getFirst();
        atf.reporting.semantic.AtfReportSemanticSnapshot snapshot;
        Path published;
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(file) : new ExtentReportSession(file)) {
            published = session.reportPath();
            var scenario = session.startScenario("ConflictingFlowFixture", "HTTP evidence must survive", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("Observed exchange", "GET", "/demo");
            request.detail("evidence.type", "api-trace");
            request.detail("evidence.type", "unified-ui-flow");
            request.detail("flow.presentation", "compact-ui-only");
            ApiExchangeEvidence.publish(request, exchange.evidence());
            var validation = request.startValidation("API assertions");
            validation.assertions(exchange.assertions());
            validation.complete(AtfReportStatus.FAIL);
            request.complete(AtfReportStatus.FAIL);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            snapshot = session instanceof ExtentReportSession extent ? extent.semanticSnapshot()
                    : ((HtmlReportSession) session).semanticSnapshot();
        }
        return new BodyOnlyUnifiedFlowFixture.Published(published, snapshot, Files.readString(published));
    }

    private static UiApiValidationEvidence apiEvidence(Variant variant) {
        if (variant != Variant.API && variant != Variant.API_FAILURE) return UiApiValidationEvidence.empty();
        var route = MockApiReportContractFixtures.transportGetRoute();
        var configuration = variant == Variant.API ? MockApiReportContractFixtures.configuration(route)
                : MockApiReportContractFixtures.configuration();
        var exchange = MockApiReportExecution.execute(configuration, route, "", MockApiReportContractFixtures.BASE_URL);
        var evidence = ReportModelAdapters.execution(exchange.execution());
        var check = new AtfAssertion("API status", "200", Integer.toString(evidence.response().status()),
                evidence.response().status() == 200);
        return new UiApiValidationEvidence(List.of(), List.of(new UiApiValidationEvidence.Exchange(evidence, List.of(check))));
    }
}

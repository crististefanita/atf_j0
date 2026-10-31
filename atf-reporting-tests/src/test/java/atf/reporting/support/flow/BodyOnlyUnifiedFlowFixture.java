package atf.reporting.support.flow;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.ReportTestScreenshots;
import atf.reporting.support.MockApiReportContractFixtures;
import atf.reporting.support.MockApiReportExecution;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import atf.reporting.ui.validation.UiApiValidationEvidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Real UI composition for one body phase; both renderers receive the same source facts. */
public final class BodyOnlyUnifiedFlowFixture {
    public enum Variant { SUCCESS, FAILURE, SKIP, API, API_FAILURE, MULTIPLE_CAPTURES, SCREEN_CHECKPOINTS }
    public record Published(Path path, AtfReportSemanticSnapshot snapshot, String html) { }
    private BodyOnlyUnifiedFlowFixture() { }

    public static Published publish(Path output, boolean fallback, ReportScreenshotPolicy policy,
                                    int limit, Variant variant) throws Exception {
        Files.createDirectories(output);
        Path file = output.resolve("body-only-" + fallback + "-" + variant + "-" + System.nanoTime() + ".html");
        var settings = new ReportingSettings(limit);
        var profile = ReportEvidenceProfile.builder().screenshotPolicy(policy)
                .flowMode(variant == Variant.SCREEN_CHECKPOINTS
                        ? ReportEvidenceFlowMode.SCREEN_CHECKPOINTS : ReportEvidenceFlowMode.UNIFIED_FLOW).build();
        AtfReportStatus status = variant == Variant.FAILURE ? AtfReportStatus.FAIL
                : variant == Variant.SKIP ? AtfReportStatus.SKIP : AtfReportStatus.PASS;
        AtfReportSemanticSnapshot snapshot;
        try (AtfReportPorts.Session session = fallback
                ? new HtmlReportSession(file, settings) : new ExtentReportSession(file, settings)) {
            var scenario = UiExtentReportSession.adaptScenario(session.startScenario(
                    "BodyOnlyFlowFixture", "atf.reporting.fixture", "one body owns every checkpoint", List.of("component")),
                    profile, "one body owns every checkpoint");
            if (variant != Variant.SCREEN_CHECKPOINTS) scenario.unifyAcrossPhases();
            var body = scenario.startPhase("TEST BODY");
            body.step(AtfGherkinKeyword.GIVEN, "configuration is retained", "local configuration", "",
                    List.of(new AtfAssertion("Configuration retained", "ready", "ready", true)), AtfReportStatus.PASS);
            var api = apiEvidence(variant);
            body.step(AtfGherkinKeyword.WHEN, "product is selected", "#product",
                    variant == Variant.MULTIPLE_CAPTURES ? ReportTestScreenshots.ONE_PIXEL_PNG : "",
                    List.of(new AtfAssertion("Product retained", "SKU-42", "SKU-42", true)),
                    List.of(action(1, "Open product", false), action(2, "Select product", false),
                            action(3, "Observe price", true)), api,
                    ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.PASS);
            body.step(AtfGherkinKeyword.AND, "cart is checked", "#cart", "",
                    List.of(new AtfAssertion("Quantity retained", "2", "2", true)),
                    List.of(action(4, "Add to cart", false), action(5, "Observe quantity", true)), AtfReportStatus.PASS);
            String prefix = "v".repeat(600);
            body.uiValidation("one purchase is confirmed", "#confirmation", ReportTestScreenshots.SECOND_PIXEL_PNG,
                    List.of(new AtfAssertion("Order retained", prefix + "source-tail",
                            prefix + (variant == Variant.FAILURE ? "rejected-tail" : "source-tail"), variant != Variant.FAILURE)),
                    List.of(action(6, "Confirm order", false)), ReportScreenshotPolicy.CapturePoint.FINAL, status);
            body.completeFinal(status);
            scenario.complete(status);
            snapshot = session instanceof ExtentReportSession extent ? extent.semanticSnapshot()
                    : ((HtmlReportSession) session).semanticSnapshot();
        }
        System.out.println("[ATF] Body-only flow report: " + file.toAbsolutePath());
        return new Published(file, snapshot, Files.readString(file));
    }

    private static AtfUiActionRecord action(int sequence, String name, boolean observation) {
        return new AtfUiActionRecord(sequence, name, "#target", "expected", "observed", 1,
                AtfReportStatus.PASS, observation ? AtfUiEvidenceSource.OBSERVE : AtfUiEvidenceSource.ACTION);
    }

    private static UiApiValidationEvidence apiEvidence(Variant variant) {
        if (variant != Variant.API && variant != Variant.API_FAILURE) return UiApiValidationEvidence.empty();
        var route = MockApiReportContractFixtures.transportGetRoute();
        var configuration = variant == Variant.API ? MockApiReportContractFixtures.configuration(route)
                : MockApiReportContractFixtures.configuration();
        var exchange = MockApiReportExecution.execute(configuration, route, "", MockApiReportContractFixtures.BASE_URL);
        var evidence = ReportModelAdapters.execution(exchange.execution());
        var status = new AtfAssertion("API retained", "200", Integer.toString(evidence.response().status()),
                evidence.response().status() == 200);
        return new UiApiValidationEvidence(List.of(),
                List.of(new UiApiValidationEvidence.Exchange(evidence, List.of(status))));
    }
}

package atf.ui.tests.practiceshop.api.network;

import atf.api.reporting.ui.validation.UiApiValidationEvidence;

import atf.api.core.http.request.ApiRequest;
import atf.api.core.http.request.HttpMethod;
import atf.api.core.http.protocol.ApiUris;
import atf.api.core.http.protocol.HttpHeaderProfiles;
import atf.api.core.http.response.ApiExecutionResult;
import atf.api.core.http.response.ApiResponse;
import atf.api.core.http.transport.ApiTransportProvenance;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.extent.session.ExtentReportSession;
import atf.api.reporting.configuration.ReportingSettings;
import atf.api.reporting.console.AtfConsoleSink;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.reporting.security.SensitiveDataRedactor;
import atf.api.reporting.semantic.AtfReportSemanticSnapshot.RequestSnapshot;
import atf.api.reporting.ui.UiExtentReportOptions;
import atf.api.reporting.ui.UiExtentReportSession;
import atf.ui.practiceshop.api.ShopApiRoutes;
import atf.ui.practiceshop.api.ShopApiOperation;
import atf.ui.practiceshop.config.ShopConfigurationLoader;
import atf.ui.practiceshop.support.reporting.ShopApiReportPublisher;
import atf.ui.practiceshop.support.api.ShopApiNetworkJournal;
import atf.ui.practiceshop.support.api.ShopApiEvidenceMapper;
import atf.ui.practiceshop.support.api.ShopApiReportValidation;

import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static atf.api.reporting.contract.AtfGherkinKeyword.THEN;
import static atf.api.reporting.evidence.ReportScreenshotPolicy.CapturePoint.CHECKPOINT;
import static atf.api.reporting.contract.AtfReportStatus.FAIL;
import static atf.api.reporting.contract.AtfReportStatus.PASS;

/** Controlled captured snapshots, not a live API run or a browser journey. */
final class ShopApiMethodFidelityFixture {
    static final String QUERY = ShopApiOperation.OBSERVED_QUERY_METHOD;
    static final List<String> METHODS = List.of(QUERY, HttpMethod.GET.value(), "CUSTOM");
    static final String REQUEST_JSON = "{\"query\":\"pliers\"}";
    static final String RESPONSE_JSON = "{\"data\":[{\"id\":\"fixture-product\",\"name\":\"Pliers\"}]}";
    static final Map<String, String> JSON_HEADERS = HttpHeaderProfiles.JSON_CONTENT;
    private static final String URL = ApiUris.httpsUrl(
            "api.example.test", ShopApiRoutes.PRODUCT_SEARCH);
    private static final List<String> TAGS = List.of("component", "method-fidelity", "captured-snapshot");
    private static final String ORIGIN = "Deterministic captured snapshots; this test executes no network";

    private ShopApiMethodFidelityFixture() { }

    static ShopApiNetworkJournal.Exchange exchange(String method) {
        return new ShopApiNetworkJournal.Exchange(method, ShopApiRoutes.PRODUCT_SEARCH, URL, 200,
                REQUEST_JSON, RESPONSE_JSON, JSON_HEADERS, JSON_HEADERS);
    }

    static ReportExecutionEvidence browserEvidence(String method) {
        return ShopApiEvidenceMapper.toExecution(exchange(method));
    }

    static ApiExecutionResult directExecution(String method) {
        var request = new ApiRequest(HttpRequest.BodyPublishers.ofString(REQUEST_JSON), method,
                ShopApiRoutes.PRODUCT_SEARCH, Map.of(), JSON_HEADERS, "none", null, REQUEST_JSON);
        var response = new ApiResponse(200, RESPONSE_JSON, JSON_HEADERS, Duration.ofMillis(17), URL);
        return new ApiExecutionResult(response, 1, Duration.ofMillis(43), List.of(), List.of(),
                request, ApiTransportProvenance.LIVE_HTTP);
    }

    static Artifacts publish() {
        var configuration = ShopConfigurationLoader.load();
        String stamp = Instant.now().toString().replace(":", "-");
        Path api = configuration.apiReportDirectory().resolve("api-atf-shop-method-fidelity_" + stamp + ".html");
        List<RequestSnapshot> requests = new ArrayList<>();
        try (var session = new ExtentReportSession(api, ReportingSettings.defaults(),
                new SensitiveDataRedactor(), AtfConsoleSink.noOp(), Map.of("Evidence origin", ORIGIN))) {
            for (String method : METHODS) {
                for (boolean observed : List.of(true, false)) {
                    String mode = observed ? "observed publisher" : "standalone publisher";
                    var scenario = session.startScenario("ShopMethodFidelity",
                            "Component fixture — " + method + " — " + mode, TAGS);
                    var phase = scenario.startPhase("TEST BODY");
                    var execution = directExecution(method);
                    if (observed) {
                        var adapted = ShopApiEvidenceMapper.toExecution(execution);
                        ShopApiReportPublisher.publishObserved(phase, execution, 1,
                                new ShopApiReportValidation("Snapshot fidelity", List.of(
                                        new AtfAssertion("Captured method", method, adapted.request().method(),
                                                method.equals(adapted.request().method())))));
                    } else {
                        ShopApiReportPublisher.publish(phase, execution);
                    }
                    phase.complete(PASS);
                    scenario.complete(PASS);
                }
            }
            for (var owner : session.semanticSnapshot().classes()) {
                for (var scenario : owner.scenarios()) {
                    for (var phase : scenario.phases()) {
                        for (var node : phase.nodes()) {
                            if (node instanceof RequestSnapshot request) requests.add(request);
                        }
                    }
                }
            }
        }
        var options = UiExtentReportOptions.builder(configuration.uiReportDirectory())
                .fileName("ui-atf-shop-method-fidelity.html")
                .systemInfo(Map.of("Evidence origin", ORIGIN))
                .build();
        Path ui;
        try (var session = UiExtentReportSession.openConfigured(options)) {
            ui = session.reportPath();
            for (String method : METHODS) {
                var evidence = browserEvidence(method);
                boolean preserved = method.equals(evidence.request().method());
                var status = preserved ? PASS : FAIL;
                var scenario = session.startScenario("ShopMethodFidelity",
                        ShopApiMethodFidelityFixture.class.getPackageName(),
                        "Component fixture — captured " + method, TAGS);
                var phase = scenario.startPhase("TEST BODY");
                var validation = new UiApiValidationEvidence(List.of(),
                        List.of(new UiApiValidationEvidence.Exchange(evidence, List.of())));
                phase.step(THEN, "captured " + method + " is preserved by the API evidence adapter",
                        "component-fixture", null,
                        List.of(new AtfAssertion("Captured method", method,
                                evidence.request().method(), preserved)), List.of(), validation, CHECKPOINT, status);
                phase.complete(status);
                scenario.complete(status);
            }
        }
        return new Artifacts(api, ui, List.copyOf(requests));
    }

    record Artifacts(Path api, Path ui, List<RequestSnapshot> apiRequests) { }
}

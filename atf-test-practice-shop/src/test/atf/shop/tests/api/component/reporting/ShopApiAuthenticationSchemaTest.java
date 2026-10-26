package atf.shop.tests.api.component.reporting;

import atf.core.http.protocol.ApiUris;
import atf.core.http.protocol.HttpHeaderProfiles;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.HttpHeader;
import atf.core.http.response.ApiExecutionResult;
import atf.core.http.response.ApiResponse;
import atf.core.http.transport.ApiTransportProvenance;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.api.ReportAuthMethod;
import atf.reporting.api.ReportAuthSource;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.UiNetworkObservation;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.config.ShopValidationProfile;
import atf.shop.support.api.ShopApiEvidenceMapper;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.reporting.ShopApiReportPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopApiAuthenticationSchemaTest {
    @TempDir Path directory;

    @Test
    void directAndAuditedPublishersKeepTheProfileSeparateFromTheCapturedMechanism() throws Exception {
        var request = new ApiRequest(HttpRequest.BodyPublishers.noBody(), HttpMethod.GET.value(), "/private", Map.of(),
                Map.of(HttpHeader.AUTHORIZATION.value(), "Bearer auth-schema-private-token"),
                "shop-user", null);
        var response = new ApiResponse(200, "{}",
                HttpHeaderProfiles.JSON_CONTENT,
                Duration.ofMillis(1), ApiUris.httpsUrl("api.example.test", "/private"));
        var execution = new ApiExecutionResult(response, 1, Duration.ofMillis(1),
                List.of(), List.of(), request, ApiTransportProvenance.ACCEPTANCE_MOCK);
        var adapted = ShopApiEvidenceMapper.toExecution(execution).request();
        assertEquals("shop-user", adapted.authProfile());
        assertEquals(ReportAuthMethod.BEARER, adapted.authMethod());
        assertEquals(ReportAuthSource.HEADER, adapted.authObservation().source());
        assertFalse(adapted.headers().toString().contains("auth-schema-private-token"));
        var standalone = ShopApiReportPublisher.publishStandalone(directory.resolve("standalone.html"),
                "AuthContract", getClass().getPackageName(), "auth", List.of("component"),
                List.of(execution), AtfConsoleSink.noOp());
        Path audited = directory.resolve("audited.html");
        try (var report = new ExtentReportSession(audited)) {
            var scenario = report.startScenario("AuthAudit", "auth", List.of("component"));
            var phase = scenario.startPhase("TEST BODY");
            ShopApiReportPublisher.publishObserved(phase, execution, 1, null);
            phase.complete(AtfReportStatus.INFO);
            scenario.complete(AtfReportStatus.INFO);
        }
        for (Path path : List.of(standalone, audited)) {
            String html = Files.readString(path);
            assertTrue(html.contains("shop-user"), "the Shop profile must reach both adapter outputs");
            assertTrue(html.contains("bearer"), "the captured mechanism must reach both Shop adapter outputs");
            assertFalse(html.contains("auth-schema-private-token"), "neither Shop artifact may leak credentials");
        }
    }

    @Test
    void browserCaptureDoesNotInventAProfileAndRetainsTheMechanismAfterRedaction() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "registration"));
        var inspector = new UiNetworkInspector(
                UiNetworkInspectionConfiguration.forHosts(List.of("api.example.test")));
        journal.attach(inspector);
        inspector.capture(new UiNetworkObservation(1, Instant.EPOCH, HttpMethod.POST.value(),
                ApiUris.httpsUrl("api.example.test", "/users/register"),
                "/users/register", 201,
                Map.of(HttpHeader.AUTHORIZATION.value(), "Bearer auth-schema-private-token"), Map.of(),
                "{}", "{}", "bearer", "header", "api.example.test", "UI_BROWSER", "", true));
        journal.registrationConfirmed(0, ApiUris.httpsEndpoint("api.example.test"));
        var request = journal.evidence(ShopApiCheckpoint.REGISTRATION).getFirst().request();
        assertEquals("none", request.authProfile(), "a browser capture does not know a configured API profile");
        assertEquals(ReportAuthMethod.BEARER, request.authMethod());
        assertEquals(ReportAuthSource.HEADER, request.authObservation().source());
        assertFalse(request.headers().toString().contains("auth-schema-private-token"));
    }
}

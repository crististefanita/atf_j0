package atf.shop.tests.component.evidence;

import atf.api.core.http.protocol.HttpHeaderProfiles;
import atf.api.core.http.request.HttpMethod;
import atf.api.reporting.api.ReportAttemptRecord;
import atf.api.reporting.api.ReportRequest;
import atf.api.reporting.api.ReportResponse;
import atf.api.reporting.api.ReportTransportProvenance;
import atf.api.reporting.contract.AtfAssertion;
import atf.api.reporting.contract.AtfGherkinKeyword;
import atf.api.reporting.model.ReportExecutionEvidence;
import atf.api.testkit.AtfComponentTest;
import atf.api.ui.playwright.PlaywrightActionSequence;
import atf.api.ui.playwright.UiScreenValidation;
import atf.shop.support.ShopAuditObservations;
import atf.shop.support.ShopPurchaseCheckpoint;
import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import atf.shop.routes.ShopUiRoutes;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopAuditObservationsTest {
    private static final ShopConfiguration CONFIGURATION = ShopConfigurationLoader.load();

    @Test
    void recordsOneScreenAuditAndOneObservationPerApiExchange() {
        PlaywrightActionSequence actions = new PlaywrightActionSequence();
        Page page = page(CONFIGURATION.uri(ShopUiRoutes.product("1")).toString());
        var checkpoint = new ShopPurchaseCheckpoint(
                AtfGherkinKeyword.GIVEN,
                "product screen displays configured fields",
                List.of(new AtfAssertion("Product", "ready", "ready", true)),
                List.of(execution(HttpMethod.GET.value(), "/products/1", 200)));

        ShopAuditObservations.recordCheckpoint(actions, page, checkpoint,
                new UiScreenValidation.Evidence("product", "", false));

        var observations = actions.snapshot();
        assertEquals(3, observations.size());
        assertTrue(observations.stream().allMatch(row -> row.source().name().equals("OBSERVE")));
        assertEquals("Replay checkpoint", observations.get(0).operation());
        assertTrue(observations.get(0).expected().contains("perform the actions above"));
        assertTrue(observations.get(0).observed().contains("visible assertions=1/1"));
        assertEquals("Audit screen contract", observations.get(1).operation());
        assertEquals("Audit API exchange #1", observations.get(2).operation());
        assertTrue(observations.get(2).observed().contains("status=200"));
        assertTrue(observations.get(2).observed().contains("transport=LIVE / HTTP"));
    }

    @Test
    void ignoresMalformedApiEvidenceWithoutInventingAnAuditExchange() {
        PlaywrightActionSequence actions = new PlaywrightActionSequence();
        Page page = page(CONFIGURATION.uri(ShopUiRoutes.CHECKOUT).toString());
        var checkpoint = new ShopPurchaseCheckpoint(
                AtfGherkinKeyword.THEN,
                "confirmation screen is displayed",
                List.of(new AtfAssertion("Confirmation", "visible", "visible", true)),
                List.of(new ReportExecutionEvidence(null, 1, Duration.ZERO, List.of(), List.of(), null,
                        ReportTransportProvenance.LIVE_HTTP)));

        ShopAuditObservations.recordCheckpoint(actions, page, checkpoint,
                new UiScreenValidation.Evidence("confirmation", "", false));

        assertEquals(2, actions.snapshot().size());
        assertEquals("Replay checkpoint", actions.snapshot().get(0).operation());
        assertEquals("Audit screen contract", actions.snapshot().get(1).operation());
    }

    @Test
    void marksOnlyTheThenCheckpointAsTheTerminalReplayCondition() {
        PlaywrightActionSequence actions = new PlaywrightActionSequence();
        Page page = page(CONFIGURATION.uri(ShopUiRoutes.CHECKOUT).toString());
        var checkpoint = new ShopPurchaseCheckpoint(
                AtfGherkinKeyword.THEN,
                "order confirmation is displayed",
                List.of(new AtfAssertion("Confirmation", "visible", "visible", true)),
                List.of());

        ShopAuditObservations.recordCheckpoint(actions, page, checkpoint,
                new UiScreenValidation.Evidence("confirmation", "", false));

        var replay = actions.snapshot().getFirst();
        assertEquals("Replay checkpoint", replay.operation());
        assertTrue(replay.observed().contains("terminal=success condition"));
        assertTrue(replay.expected().contains("order confirmation is displayed"));
    }

    @Test
    void recordsScreenAuditForTheAccountCatalogCompatibilityPath() {
        PlaywrightActionSequence actions = new PlaywrightActionSequence();

        ShopAuditObservations.recordScreenContract(
                actions,
                page(CONFIGURATION.uri(ShopUiRoutes.ACCOUNT).toString()),
                "account screen is displayed",
                List.of(
                        new AtfAssertion("Account route", ShopUiRoutes.ACCOUNT,
                                ShopUiRoutes.ACCOUNT, true),
                        new AtfAssertion("Account marker", "visible", "visible", true)),
                new UiScreenValidation.Evidence("account", "", false),
                true);

        assertEquals(1, actions.snapshot().size());
        assertEquals("Audit screen contract", actions.snapshot().get(0).operation());
        assertTrue(actions.snapshot().get(0).observed().contains("assertions=2"));
        assertTrue(actions.snapshot().get(0).observed().contains("url="
                + CONFIGURATION.uri(ShopUiRoutes.ACCOUNT)));
    }

    private static ReportExecutionEvidence execution(String method, String path, int status) {
        var request = new ReportRequest(method, path, Map.of(),
                HttpHeaderProfiles.JSON_CONTENT, "bearer", "");
        var response = new ReportResponse(status, "{}",
                HttpHeaderProfiles.JSON_CONTENT, Duration.ofMillis(3),
                ShopApiConfiguration.defaults().endpoint().resolve(path.substring(1)).toString());
        return new ReportExecutionEvidence(response, 1, Duration.ofMillis(3), List.of(),
                List.of(new ReportAttemptRecord(1, status, Duration.ofMillis(3), "SUCCESS", "final response")),
                request, ReportTransportProvenance.LIVE_HTTP);
    }

    private static Page page(String url) {
        return (Page) Proxy.newProxyInstance(
                Page.class.getClassLoader(),
                new Class<?>[]{Page.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "url" -> url;
                    case "toString" -> "page(" + url + ")";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}

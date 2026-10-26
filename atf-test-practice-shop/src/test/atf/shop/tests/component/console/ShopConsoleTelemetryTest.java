package atf.shop.tests.component.console;

import atf.core.config.ApiTransportMode;
import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.AtfComponentTest;
import atf.shop.api.ShopApiClient;
import atf.shop.config.model.ShopCredentials;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopConsoleTelemetryTest {
    @Test
    void detailedApiTelemetryRedactsCredentialsPersonalBodiesAndQueryValues() {
        var events = new ArrayList<AtfConsoleEvent>();
        try (var client = ShopApiClient.forTransport(ShopApiScenarioFixture.API_ENDPOINT,
                ShopApiComponentFixture.transport(), ApiTransportMode.ACCEPTANCE, events::add)) {
            client.login(new ShopCredentials("private-customer@example.test", "private-password", "First", "Last"));
            client.execute(client.requests().postcodeLookup("PrivateCountry", "PrivatePostal", "PrivateHouse"));
        }
        String published = events.toString();
        for (String secret : new String[]{"private-customer", "private-password", "token-1",
                "PrivateCountry", "PrivatePostal", "PrivateHouse"}) {
            assertFalse(published.contains(secret), secret);
        }
        assertTrue(published.contains("redacted"));
    }

    @Test
    void apiClientPublishesDetailedEventsThroughTheInjectedConsole() {
        var events = new ArrayList<AtfConsoleEvent>();
        try (var client = ShopApiClient.forTransport(ShopApiScenarioFixture.API_ENDPOINT,
                ShopApiComponentFixture.transport(), ApiTransportMode.ACCEPTANCE, events::add)) {
            client.execute(client.requests().search("Pliers"));
        }
        assertTrue(events.stream().anyMatch(event -> event.type() == AtfConsoleEvent.Type.DETAIL));
        assertTrue(events.stream().anyMatch(event -> event.type() == AtfConsoleEvent.Type.CODE
                && "response.body".equals(event.detailName())));
    }
}

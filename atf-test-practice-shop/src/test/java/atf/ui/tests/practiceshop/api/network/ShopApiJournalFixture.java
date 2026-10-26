package atf.ui.tests.practiceshop.api.network;

import atf.api.core.http.protocol.ApiUris;
import atf.api.core.http.protocol.HttpHeaderProfiles;
import atf.api.core.http.request.HttpHeader;
import atf.api.core.http.request.MediaTypes;
import atf.api.core.http.response.ApiBodyEvidence;
import atf.api.ui.config.UiNetworkInspectionConfiguration;
import atf.api.ui.network.UiNetworkCapturePolicy;
import atf.api.ui.network.UiNetworkInspector;
import atf.api.ui.network.UiNetworkObservation;
import atf.ui.practiceshop.support.api.ShopApiNetworkJournal;
import atf.ui.practiceshop.support.api.ShopApiNetworkJournal.Exchange;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Controlled observations enter through the same public inspector subscription as browser evidence. */
final class ShopApiJournalFixture {
    private ShopApiJournalFixture() { }

    static void capture(ShopApiNetworkJournal journal, Exchange exchange) {
        String host = URI.create(exchange.url()).getHost();
        var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(List.of(host)));
        journal.attach(inspector);
        var observation = new UiNetworkObservation(1, Instant.EPOCH, exchange.method(), exchange.url(),
                exchange.path(), exchange.status(), exchange.requestHeaders(), exchange.responseHeaders(),
                exchange.requestBody(), exchange.responseBody(), exchange.authType(), "", host,
                "COMPONENT_CAPTURE", exchange.signature(), !ApiBodyEvidence.isUnavailable(exchange.responseBody()),
                exchange.failureReason(), UiNetworkCapturePolicy.ResourceKind.FETCH);
        if (inspector.capture(observation).isEmpty()) {
            throw new AssertionError("Fixture observation was excluded by the network capture policy");
        }
    }

    static Exchange exchange(String method, String path, String requestBody, String responseBody) {
        return exchange("shop.test", method, path, requestBody, responseBody);
    }

    static Exchange exchange(String host, String method, String path, String requestBody, String responseBody) {
        return new Exchange(method, path, ApiUris.httpsUrl(host, path), 200, requestBody, responseBody,
                Map.of(HttpHeader.ACCEPT.value(), MediaTypes.APPLICATION_JSON), HttpHeaderProfiles.JSON_CONTENT);
    }
}

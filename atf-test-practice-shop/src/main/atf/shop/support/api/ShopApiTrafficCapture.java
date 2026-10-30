package atf.shop.support.api;

import atf.core.http.response.ApiBodyEvidence;
import atf.shop.support.ShopJsonEvidence;
import com.microsoft.playwright.Response;

import java.util.Optional;
import java.net.URI;
import atf.ui.recovery.UiRecoveryOperation;

/** Captures and sanitizes one Practice Shop HTTP response from the current UI session. */
final class ShopApiTrafficCapture {
    private static final String API_HOST = "api.practicesoftwaretesting.com";
    private final String origin;

    ShopApiTrafficCapture() { origin = null; }

    ShopApiTrafficCapture(URI endpoint) {
        origin = UiRecoveryOperation.origin(java.util.Objects.requireNonNull(endpoint, "endpoint"));
    }

    Optional<ShopApiNetworkJournal.Exchange> capture(Response response) {
        if (response == null) return Optional.empty();
        var request = response.request();
        if (request == null || !isShopApi(request.url())) {
            return Optional.empty();
        }
        String path = path(request.url());
        String responseBody;
        try {
            // Response callbacks can arrive before Chromium has released the
            // compressed CORS payload. Wait for the response lifecycle to settle
            // before reading the body; otherwise a valid JSON response can be
            // reported as transport-unavailable evidence.
            response.finished();
            responseBody = ShopApiEvidenceMapper.redactJson(new String(response.body()));
        } catch (RuntimeException unavailable) {
            // Keep the limitation explicit when the browser still cannot expose
            // the payload after the lifecycle wait; assertions must not treat it
            // as an empty or successful JSON body.
            responseBody = ApiBodyEvidence.unavailable(
                    "Chromium response body", unavailable);
        }
        return Optional.of(new ShopApiNetworkJournal.Exchange(
                request.method(), path, request.url(), response.status(),
                ShopApiEvidenceMapper.redactJson(request.postData()), responseBody,
                ShopJsonEvidence.redactHeaders(request.headers()),
                ShopJsonEvidence.redactHeaders(response.headers())));
    }

    private boolean isShopApi(String url) {
        try {
            URI actual = URI.create(url);
            return origin == null ? API_HOST.equals(actual.getHost())
                    : origin.equals(UiRecoveryOperation.origin(actual));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String path(String url) {
        java.net.URI uri = java.net.URI.create(url);
        return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

}

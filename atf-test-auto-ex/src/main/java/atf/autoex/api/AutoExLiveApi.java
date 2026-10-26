package atf.autoex.api;

import atf.core.config.ApiSettings;
import atf.core.config.ApiTransportMode;
import atf.core.config.ApiTransportPolicy;
import atf.core.http.auth.AuthProfiles;
import atf.core.http.auth.ConfiguredApiAuthenticator;
import atf.core.http.execution.ApiExecutor;
import atf.core.http.transport.ApiLiveTransportCatalog;
import atf.core.http.transport.UrlConnectionTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/** Explicit, single-attempt transport shared by live API contracts and real customer setup. */
public final class AutoExLiveApi {
    private static final String PROFILE = "autoex-live";

    private AutoExLiveApi() { }

    public static ApiExecutor open(URI baseUrl, Duration timeout) {
        var catalog = ApiLiveTransportCatalog.single(PROFILE, baseUrl, AuthProfiles.NONE, timeout,
                (endpoint, authenticator) -> new UrlConnectionTransport(authenticator, endpoint.timeout()));
        var transport = catalog.open(PROFILE, ApiTransportPolicy.explicitLive(),
                new ConfiguredApiAuthenticator(Map.of()));
        var settings = new ApiSettings(baseUrl, AuthProfiles.NONE, timeout, Set.of(), 1);
        return new ApiExecutor(transport, settings.snapshot(), ApiTransportMode.LIVE);
    }
}

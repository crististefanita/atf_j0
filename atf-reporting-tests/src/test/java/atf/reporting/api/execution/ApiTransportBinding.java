package atf.reporting.api.execution;
import atf.core.http.transport.ApiTransport;
import atf.core.config.ApiTransportMode;

import java.util.Objects;

/** Immutable, typed transport policy supplied to one API module. */
public record ApiTransportBinding(ApiTransportMode mode, ApiTransport transport,
                                  boolean liveOptIn) {
    public ApiTransportBinding {
        mode = Objects.requireNonNull(mode, "mode");
        transport = Objects.requireNonNull(transport, "transport");
        if (mode == ApiTransportMode.LIVE && !liveOptIn) {
            throw new IllegalArgumentException(
                    "LIVE transport is opt-in; construct the binding with liveOptIn=true");
        }
        if (mode == ApiTransportMode.ACCEPTANCE && liveOptIn) {
            throw new IllegalArgumentException("Acceptance transport cannot declare live opt-in");
        }
    }

    public static ApiTransportBinding acceptance(ApiTransport transport) {
        return new ApiTransportBinding(ApiTransportMode.ACCEPTANCE, transport, false);
    }

    public static ApiTransportBinding live(ApiTransport transport) {
        return new ApiTransportBinding(ApiTransportMode.LIVE, transport, true);
    }
}

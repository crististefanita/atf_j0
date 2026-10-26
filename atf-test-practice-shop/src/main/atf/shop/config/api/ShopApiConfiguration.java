package atf.shop.config.api;

import atf.core.config.ApiTransportMode;
import atf.core.http.protocol.ApiUris;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Explicit API composition policy; acceptance remains the safe default. */
public record ShopApiConfiguration(
        URI endpoint,
        ApiTransportMode mode,
        ShopApiPrecondition precondition,
        Duration timeout,
        ShopApiEvidenceBinding evidenceBinding) {

    /** Compatibility constructor for callers that do not customize checkpoints. */
    public ShopApiConfiguration(URI endpoint, ApiTransportMode mode,
                                        ShopApiPrecondition precondition,
                                        Duration timeout) {
        this(endpoint, mode, precondition, timeout, ShopApiEvidenceBinding.all());
    }

    public ShopApiConfiguration {
        endpoint = Objects.requireNonNull(endpoint, "endpoint");
        mode = Objects.requireNonNull(mode, "mode");
        precondition = Objects.requireNonNull(precondition, "precondition");
        timeout = Objects.requireNonNull(timeout, "timeout");
        evidenceBinding = Objects.requireNonNull(evidenceBinding, "evidenceBinding");
        if (!endpoint.isAbsolute() || endpoint.getHost() == null) {
            throw new IllegalArgumentException("API endpoint must be an absolute URI");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("API timeout must be positive");
        }
    }

    public static ShopApiConfiguration defaults() {
        return new ShopApiConfiguration(
                ApiUris.httpsEndpoint("api.practicesoftwaretesting.com"),
                ApiTransportMode.ACCEPTANCE,
                ShopApiPrecondition.AUTHENTICATED,
                Duration.ofSeconds(20), ShopApiEvidenceBinding.all());
    }

    public boolean authenticatedPrecondition() {
        return precondition == ShopApiPrecondition.AUTHENTICATED;
    }
}

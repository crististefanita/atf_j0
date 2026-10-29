package atf.ui.network.delivery;

import java.util.Objects;

/** An owned subscriber failure, distinct from retention or transport failure; never rendered implicitly. */
public record UiNetworkDeliveryFailure(long observationSequence, int subscriber, String requestSignature,
                                       RuntimeException cause) {
    public UiNetworkDeliveryFailure {
        if (subscriber < 0) throw new IllegalArgumentException("subscriber must not be negative");
        requestSignature = Objects.requireNonNull(requestSignature, "requestSignature");
        Objects.requireNonNull(cause, "cause");
    }
}

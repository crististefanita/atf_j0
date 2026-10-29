package atf.ui.network.identity;

import java.util.UUID;

/** Scope-local request-start order; response arrival is deliberately a separate sequence. */
public record UiRequestIdentity(UUID scope, long sequence) {
    public UiRequestIdentity {
        if (scope == null || sequence < 0) throw new IllegalArgumentException("request identity requires scope and nonnegative order");
    }
    public boolean isAfter(UiRequestIdentity marker) {
        return marker != null && scope.equals(marker.scope) && sequence > marker.sequence;
    }
    public UiRequestIdentity beginning() { return new UiRequestIdentity(scope, 0); }
}

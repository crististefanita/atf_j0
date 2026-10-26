package atf.ui.config;

import atf.ui.network.UiNetworkCapturePolicy;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Opt-in, fail-closed policy for browser network observations.
 *
 * <p>The host allowlist is intentional: the inspector is for API evidence,
 * not for indiscriminate collection of static assets, third-party traffic or
 * credentials. Bodies are bounded before they leave the browser boundary and
 * repeated signatures remain available as counts rather than duplicated
 * report nodes.</p>
 */
public record UiNetworkInspectionConfiguration(
        boolean enabled,
        Set<String> allowedHosts,
        int maxObservations,
        int maxBodyPreviewCharacters,
        boolean collapseConsecutiveSignatures,
        UiNetworkSettlementPolicy settlementPolicy,
        UiNetworkCapturePolicy capturePolicy) {

    public UiNetworkInspectionConfiguration {
        allowedHosts = normalizeHosts(allowedHosts);
        if (maxObservations < 1) {
            throw new IllegalArgumentException("ui.network.maxObservations must be positive");
        }
        if (maxBodyPreviewCharacters < 1) {
            throw new IllegalArgumentException(
                    "ui.network.maxBodyPreviewCharacters must be positive");
        }
        settlementPolicy = settlementPolicy == null
                ? UiNetworkSettlementPolicy.disabled() : settlementPolicy;
        capturePolicy = capturePolicy == null ? UiNetworkCapturePolicy.allResources() : capturePolicy;
        if (enabled && allowedHosts.isEmpty()) {
            throw new IllegalArgumentException(
                    "ui.network.allowedHosts must contain at least one host when enabled");
        }
    }

    public UiNetworkInspectionConfiguration(boolean enabled, Set<String> allowedHosts,
                                            int maxObservations, int maxBodyPreviewCharacters,
                                            boolean collapseConsecutiveSignatures,
                                            UiNetworkSettlementPolicy settlementPolicy) {
        this(enabled, allowedHosts, maxObservations, maxBodyPreviewCharacters,
                collapseConsecutiveSignatures, settlementPolicy,
                UiNetworkCapturePolicy.allResources());
    }

    public static UiNetworkInspectionConfiguration disabled() {
        return new UiNetworkInspectionConfiguration(false, Set.of(), 256, 12_000,
                true, UiNetworkSettlementPolicy.disabled());
    }

    public static UiNetworkInspectionConfiguration forHosts(Collection<String> hosts) {
        return new UiNetworkInspectionConfiguration(true, normalizeHosts(hosts), 256, 12_000,
                true, UiNetworkSettlementPolicy.disabled());
    }

    public boolean accepts(String url) {
        if (!enabled || url == null || url.isBlank()) return false;
        try {
            String host = java.net.URI.create(url).getHost();
            return host != null && allowedHosts.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("UI network observation received an invalid URL: " + url, error);
        }
    }

    public boolean accepts(String url, UiNetworkCapturePolicy.ResourceKind resourceKind) {
        return accepts(url) && capturePolicy.accepts(url, resourceKind);
    }

    public UiNetworkInspectionConfiguration withCapturePolicy(UiNetworkCapturePolicy policy) {
        return new UiNetworkInspectionConfiguration(enabled, allowedHosts, maxObservations,
                maxBodyPreviewCharacters, collapseConsecutiveSignatures, settlementPolicy, policy);
    }

    /** Enables capture without replacing the caller's selection or limits. */
    public UiNetworkInspectionConfiguration withEnabled(boolean enabled) {
        return new UiNetworkInspectionConfiguration(enabled, allowedHosts, maxObservations,
                maxBodyPreviewCharacters, collapseConsecutiveSignatures, settlementPolicy, capturePolicy);
    }

    public UiNetworkInspectionConfiguration withSettlementPolicy(UiNetworkSettlementPolicy policy) {
        return new UiNetworkInspectionConfiguration(enabled, allowedHosts, maxObservations,
                maxBodyPreviewCharacters, collapseConsecutiveSignatures, policy, capturePolicy);
    }

    private static Set<String> normalizeHosts(Collection<String> hosts) {
        if (hosts == null) return Set.of();
        Set<String> normalized = new LinkedHashSet<>();
        for (String host : hosts) {
            if (host == null || host.isBlank()) continue;
            String value = host.trim().toLowerCase(Locale.ROOT);
            if (value.contains("/") || value.contains(":") || value.contains(" ")) {
                throw new IllegalArgumentException("ui.network.allowedHosts must contain host names: " + host);
            }
            normalized.add(value);
        }
        return Set.copyOf(normalized);
    }
}

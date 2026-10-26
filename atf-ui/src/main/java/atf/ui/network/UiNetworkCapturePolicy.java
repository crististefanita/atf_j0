package atf.ui.network;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Browser-resource and route selection applied before response bodies are read. */
public record UiNetworkCapturePolicy(Set<ResourceKind> resourceKinds, List<String> pathPrefixes) {
    public enum ResourceKind {
        XHR, FETCH, DOCUMENT, STATIC, OTHER;

        public static ResourceKind fromPlaywright(String value) {
            return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
                case "xhr" -> XHR;
                case "fetch" -> FETCH;
                case "document" -> DOCUMENT;
                case "stylesheet", "script", "image", "font", "media", "manifest" -> STATIC;
                default -> OTHER;
            };
        }
    }

    public UiNetworkCapturePolicy {
        resourceKinds = Set.copyOf(resourceKinds == null ? Set.of() : resourceKinds);
        pathPrefixes = List.copyOf(pathPrefixes == null ? List.of() : pathPrefixes);
        if (resourceKinds.isEmpty()) throw new IllegalArgumentException("network resource kinds must not be empty");
        for (String prefix : pathPrefixes) {
            if (prefix == null || !prefix.startsWith("/") || prefix.contains("?") || prefix.contains("#")) {
                throw new IllegalArgumentException("network path prefix must be an absolute path: " + prefix);
            }
        }
    }

    /** Backward-compatible unrestricted policy for existing consumers. */
    public static UiNetworkCapturePolicy allResources() {
        return new UiNetworkCapturePolicy(Set.of(ResourceKind.values()), List.of());
    }

    /** First-party browser API calls, including HTML-returning XHR/fetch routes. */
    public static UiNetworkCapturePolicy browserApi(Collection<String> pathPrefixes) {
        return new UiNetworkCapturePolicy(Set.of(ResourceKind.XHR, ResourceKind.FETCH),
                List.copyOf(pathPrefixes));
    }

    public static UiNetworkCapturePolicy asynchronous() {
        return browserApi(List.of());
    }

    public UiNetworkCapturePolicy includingDocuments() {
        var kinds = new LinkedHashSet<>(resourceKinds);
        kinds.add(ResourceKind.DOCUMENT);
        return new UiNetworkCapturePolicy(kinds, pathPrefixes);
    }

    public UiNetworkCapturePolicy forPaths(Collection<String> prefixes) {
        return new UiNetworkCapturePolicy(resourceKinds, List.copyOf(prefixes));
    }

    /** Safe summary: route values are configuration, never diagnostic payload. */
    public String description() {
        String kinds = resourceKinds.stream().map(kind -> kind.name().toLowerCase(Locale.ROOT))
                .sorted().collect(java.util.stream.Collectors.joining(", "));
        return kinds + "; host allowlist" + (pathPrefixes.isEmpty() ? "" : "; configured path selection");
    }

    public boolean accepts(String url, ResourceKind kind) {
        if (!resourceKinds.contains(kind)) return false;
        if (pathPrefixes.isEmpty()) return true;
        String path = URI.create(url).getRawPath();
        return path != null && pathPrefixes.stream().anyMatch(path::startsWith);
    }

    public String scope() {
        return "resourceKinds=" + new LinkedHashSet<>(resourceKinds)
                + "; paths=" + (pathPrefixes.isEmpty() ? "all" : pathPrefixes);
    }
}


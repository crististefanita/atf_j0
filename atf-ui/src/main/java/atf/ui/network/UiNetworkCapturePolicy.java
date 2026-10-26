package atf.ui.network;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Browser-resource and route selection applied before response bodies are read. */
public record UiNetworkCapturePolicy(Set<ResourceKind> resourceKinds, List<String> pathPrefixes,
                                     List<String> documentPathPrefixes,
                                     Set<ResourceKind> responseBodyResourceKinds) {
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
        documentPathPrefixes = List.copyOf(documentPathPrefixes == null ? List.of() : documentPathPrefixes);
        responseBodyResourceKinds = Set.copyOf(responseBodyResourceKinds == null
                ? Set.of() : responseBodyResourceKinds);
        if (resourceKinds.isEmpty()) throw new IllegalArgumentException("network resource kinds must not be empty");
        validatePrefixes(pathPrefixes, "network path prefix");
        validatePrefixes(documentPathPrefixes, "network document path prefix");
    }

    /** Retains the original construction contract for existing consumers. */
    public UiNetworkCapturePolicy(Set<ResourceKind> resourceKinds, List<String> pathPrefixes) {
        this(resourceKinds, pathPrefixes, List.of(), resourceKinds);
    }

    /** Retains the three-axis construction contract. */
    public UiNetworkCapturePolicy(Set<ResourceKind> resourceKinds, List<String> pathPrefixes,
                                  List<String> documentPathPrefixes) {
        this(resourceKinds, pathPrefixes, documentPathPrefixes, resourceKinds);
    }

    /** Backward-compatible unrestricted policy for existing consumers. */
    public static UiNetworkCapturePolicy allResources() {
        return new UiNetworkCapturePolicy(Set.of(ResourceKind.values()), List.of(), List.of(),
                Set.of(ResourceKind.values()));
    }

    /** First-party browser API calls, including HTML-returning XHR/fetch routes. */
    public static UiNetworkCapturePolicy browserApi(Collection<String> pathPrefixes) {
        return new UiNetworkCapturePolicy(Set.of(ResourceKind.XHR, ResourceKind.FETCH),
                List.copyOf(pathPrefixes), List.of(), Set.of(ResourceKind.XHR, ResourceKind.FETCH));
    }

    public static UiNetworkCapturePolicy asynchronous() {
        return browserApi(List.of());
    }

    public UiNetworkCapturePolicy includingDocuments() {
        var kinds = new LinkedHashSet<>(resourceKinds);
        kinds.add(ResourceKind.DOCUMENT);
        var bodyKinds = new LinkedHashSet<>(responseBodyResourceKinds);
        bodyKinds.add(ResourceKind.DOCUMENT);
        return new UiNetworkCapturePolicy(kinds, pathPrefixes, List.of(), bodyKinds);
    }

    /** Adds document navigations/forms, constrained to the selected application routes. */
    public UiNetworkCapturePolicy includingDocumentsFor(Collection<String> prefixes) {
        List<String> selected = List.copyOf(prefixes == null ? List.of() : prefixes);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("document route selection must not be empty");
        }
        var kinds = new LinkedHashSet<>(resourceKinds);
        kinds.add(ResourceKind.DOCUMENT);
        var bodyKinds = new LinkedHashSet<>(responseBodyResourceKinds);
        bodyKinds.remove(ResourceKind.DOCUMENT);
        return new UiNetworkCapturePolicy(kinds, pathPrefixes, selected, bodyKinds);
    }

    /** Includes first-party request kinds that are not classified as XHR, fetch, document or static assets. */
    public UiNetworkCapturePolicy includingOtherRequests() {
        var kinds = new LinkedHashSet<>(resourceKinds);
        kinds.add(ResourceKind.OTHER);
        return new UiNetworkCapturePolicy(kinds, pathPrefixes, documentPathPrefixes,
                responseBodyResourceKinds);
    }

    public UiNetworkCapturePolicy forPaths(Collection<String> prefixes) {
        return new UiNetworkCapturePolicy(resourceKinds, List.copyOf(prefixes), documentPathPrefixes,
                responseBodyResourceKinds);
    }

    /** Safe summary: route values are configuration, never diagnostic payload. */
    public String description() {
        String kinds = resourceKinds.stream().map(kind -> kind.name().toLowerCase(Locale.ROOT))
                .sorted().collect(java.util.stream.Collectors.joining(", "));
        return kinds + "; host allowlist"
                + (pathPrefixes.isEmpty() ? "" : "; configured path selection")
                + (documentPathPrefixes.isEmpty() ? "" : "; selected document routes")
                + "; response bodies=" + responseBodyResourceKinds.stream()
                .map(kind -> kind.name().toLowerCase(Locale.ROOT)).sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    public boolean accepts(String url, ResourceKind kind) {
        if (!resourceKinds.contains(kind)) return false;
        String path = URI.create(url).getRawPath();
        if (path == null) return false;
        if (!pathPrefixes.isEmpty() && !matches(path, pathPrefixes)) return false;
        return kind != ResourceKind.DOCUMENT || documentPathPrefixes.isEmpty()
                || matches(path, documentPathPrefixes);
    }

    /** Whether the response body should be read after an observation is selected. */
    public boolean capturesResponseBody(ResourceKind kind) {
        return responseBodyResourceKinds.contains(kind);
    }

    public String scope() {
        return "resourceKinds=" + new LinkedHashSet<>(resourceKinds)
                + "; paths=" + (pathPrefixes.isEmpty() ? "all" : pathPrefixes)
                + "; documentPaths=" + (documentPathPrefixes.isEmpty() ? "all" : documentPathPrefixes)
                + "; responseBodies=" + new LinkedHashSet<>(responseBodyResourceKinds);
    }

    private static boolean matches(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(prefix -> path.equals(prefix)
                || (prefix.endsWith("/") ? path.startsWith(prefix) : path.startsWith(prefix + "/")));
    }

    private static void validatePrefixes(List<String> prefixes, String label) {
        for (String prefix : prefixes) {
            if (prefix == null || !prefix.startsWith("/") || prefix.contains("?") || prefix.contains("#")) {
                throw new IllegalArgumentException(label + " must be an absolute path: " + prefix);
            }
        }
    }
}


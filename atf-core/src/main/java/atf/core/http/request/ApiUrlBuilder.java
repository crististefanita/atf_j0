package atf.core.http.request;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class ApiUrlBuilder {
    private final URI baseUri;

    public ApiUrlBuilder(URI baseUri) {
        this.baseUri = baseUri;
    }

    public String build(ApiRequest request) {
        StringBuilder value = new StringBuilder(baseUri.toString());
        if (value.length() > 0 && value.charAt(value.length() - 1) == '/' && request.path().startsWith("/")) {
            value.deleteCharAt(value.length() - 1);
        }
        if (value.length() > 0 && value.charAt(value.length() - 1) != '/' && !request.path().startsWith("/")) {
            value.append('/');
        }
        value.append(request.path());
        if (!request.queryParameters().isEmpty()) {
            value.append('?');
            value.append(request.queryParameters().values().stream()
                    .sorted(java.util.Comparator.comparing(ApiQueryParameter::name)
                            .thenComparing(ApiQueryParameter::value))
                    .map(entry -> encode(entry.name()) + "=" + encode(entry.value()))
                    .collect(java.util.stream.Collectors.joining("&")));
        }
        return value.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

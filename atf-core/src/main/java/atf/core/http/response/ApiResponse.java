package atf.core.http.response;

import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.Map;

public record ApiResponse(int status, String body, Map<String, String> headers, Duration duration, String url) {
    public ApiResponse {
        body = body == null ? "" : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        duration = duration == null ? Duration.ZERO : duration;
    }

    public String header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static Map<String, String> flatten(HttpHeaders headers) {
        return headers.map().entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> String.join(", ", entry.getValue())));
    }
}

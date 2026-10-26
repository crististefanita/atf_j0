package atf.reporting.api;

import java.time.Duration;
import java.util.Map;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.api.evidence.ReportTransportTiming;

/** Null duration means not captured; zero is a measurement. Body availability is independent of JSON null. */
public record ReportResponse(int status, String body, Map<String,String> headers, Duration duration, String url,
                             ReportBodyAvailability bodyAvailability) {
    public ReportResponse(int status, String body, Map<String, String> headers, Duration duration, String url) {
        this(status, body, headers, duration, url, status < 0
                ? ReportBodyAvailability.NOT_CAPTURED
                : ReportBodyAvailability.from(body));
    }
    public ReportResponse {
        bodyAvailability = bodyAvailability == null
                ? ReportBodyAvailability.from(body) : bodyAvailability;
        body = body == null ? "" : body;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        ReportTransportTiming.validate(duration);
        url = url == null ? "" : url;
    }

    public String statusLabel() { return status < 0 ? "Unavailable" : Integer.toString(status); }
}

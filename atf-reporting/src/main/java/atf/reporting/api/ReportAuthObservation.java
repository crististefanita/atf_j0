package atf.reporting.api;

import java.util.Map;

/**
 * Safe auth projection for report summaries.  It intentionally contains only
 * the typed method and its source, never a header, cookie, query value or
 * credential.
 */
public record ReportAuthObservation(ReportAuthMethod method, ReportAuthSource source) {
    public ReportAuthObservation {
        method = method == null ? ReportAuthMethod.NONE : method;
        source = source == null ? ReportAuthSource.NONE : source;
    }

    public static ReportAuthObservation from(ReportRequest request) {
        if (request == null) return new ReportAuthObservation(ReportAuthMethod.NONE, ReportAuthSource.NONE);
        return request.authObservation();
    }

    /** Observed transport evidence wins; profile-name inference is compatibility-only. */
    public static ReportAuthObservation from(String authProfile, Map<String, String> headers,
                                              Map<String, String> queryParameters) {
        ReportAuthMethod header = ReportAuthMethod.fromHeaders(headers);
        if (header != ReportAuthMethod.NONE) {
            return new ReportAuthObservation(header, ReportAuthSource.HEADER);
        }

        ReportAuthMethod query = ReportAuthMethod.fromQuery(queryParameters);
        if (query != ReportAuthMethod.NONE) {
            return new ReportAuthObservation(query, ReportAuthSource.QUERY);
        }

        ReportAuthMethod profile = ReportAuthMethod.fromProfile(authProfile);
        if (profile != ReportAuthMethod.NONE) {
            return new ReportAuthObservation(profile, ReportAuthSource.PROFILE);
        }

        return new ReportAuthObservation(ReportAuthMethod.NONE, ReportAuthSource.NONE);
    }

    /** Compact summary suitable for a single network-inspector row. */
    public String summary() {
        return method.wireValue() + " (source=" + source.wireValue() + ")";
    }
}

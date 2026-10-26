package atf.reporting.api;

/** Canonical report field labels shared by every API evidence publisher. */
public enum ApiEvidenceField {
    DURATION("Duration"),
    URL("URL"),
    HTTP_STATUS("HTTP STATUS"),
    CONTENT_TYPE(ReportHttpHeaders.CONTENT_TYPE),
    ATTEMPTS("Attempt"),
    AUTH_PROFILE("Auth profile"),
    AUTH_TYPE("Auth type"),
    TRANSPORT("Transport"),
    MANUAL_REPLAY("Manual replay"),
    QUERY_PARAMS("Query params"),
    REQUEST_HEADERS("Request headers"),
    RESPONSE_HEADERS("Response headers"),
    JSON_CONTRACT("JSON CONTRACT");

    private final String label;

    ApiEvidenceField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

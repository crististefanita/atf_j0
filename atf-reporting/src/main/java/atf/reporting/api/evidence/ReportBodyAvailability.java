package atf.reporting.api.evidence;

/** Availability of captured bytes, independent of the JSON value inside them. */
public enum ReportBodyAvailability {
    AVAILABLE, ABSENT, NOT_CAPTURED;

    public static ReportBodyAvailability from(String body) {
        return body == null ? NOT_CAPTURED : body.isEmpty() ? ABSENT : AVAILABLE;
    }
}

package atf.core.http.transport;

import atf.core.config.ApiTransportMode;

/** Describes whether an API exchange contacted a network endpoint. */
public enum ApiTransportProvenance {
    UNKNOWN("UNKNOWN — transport provenance was not declared", false),
    ACCEPTANCE_MOCK("ACCEPTANCE / MOCK — network was not executed", false),
    LIVE_HTTP("LIVE / HTTP — network was executed", true);

    private final String reportLabel;
    private final boolean networkExecuted;

    ApiTransportProvenance(String reportLabel, boolean networkExecuted) {
        this.reportLabel = reportLabel;
        this.networkExecuted = networkExecuted;
    }

    public String reportLabel() {
        return reportLabel;
    }

    public boolean networkExecuted() {
        return networkExecuted;
    }

    public static ApiTransportProvenance from(ApiTransportMode mode) {
        if (mode == null) return UNKNOWN;
        return mode == ApiTransportMode.LIVE ? LIVE_HTTP : ACCEPTANCE_MOCK;
    }
}

package atf.reporting.api;

public enum ReportTransportProvenance {
    UNKNOWN("UNKNOWN", false), ACCEPTANCE_MOCK("ACCEPTANCE / MOCK — network was not executed", false), LIVE_HTTP("LIVE / HTTP — network was executed", true);
    private final String reportLabel; private final boolean networkExecuted;
    ReportTransportProvenance(String reportLabel, boolean networkExecuted) { this.reportLabel=reportLabel; this.networkExecuted=networkExecuted; }
    public String reportLabel() { return reportLabel; }
    public boolean networkExecuted() { return networkExecuted; }
}

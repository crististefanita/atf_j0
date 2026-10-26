package atf.reporting.ui.network;

/** Capture completeness is diagnostic, never a substitute for business assertions. */
public record ReportNetworkCaptureStatus(String scope, long accepted, long retained, long excluded,
                                         long dropped, long inFlight, boolean limitReached,
                                         long setupExchanges) {
    public ReportNetworkCaptureStatus {
        scope = scope == null ? "" : scope.trim();
        if (accepted < 0 || retained < 0 || excluded < 0 || dropped < 0 || inFlight < 0
                || setupExchanges < 0 || setupExchanges > retained
                || accepted != retained + dropped + inFlight) {
            throw new IllegalArgumentException("inconsistent network capture counters");
        }
    }

    public ReportNetworkCaptureStatus(String scope, int accepted, int retained,
                                      int dropped, int setupRetained, boolean limitReached) {
        this(scope, accepted, retained, 0, dropped, 0, limitReached, setupRetained);
        if (limitReached != (dropped > 0)) {
            throw new IllegalArgumentException("network limitReached must reflect dropped observations");
        }
    }

    /** Complete only in the declared scope at this snapshot, not for future browser traffic. */
    public boolean complete() { return dropped == 0 && inFlight == 0; }
    public String completeness() { return complete() ? "COMPLETE" : "INCOMPLETE"; }
    public long setupRetained() { return setupExchanges; }
    public long causalRetained() { return retained - setupExchanges; }
}


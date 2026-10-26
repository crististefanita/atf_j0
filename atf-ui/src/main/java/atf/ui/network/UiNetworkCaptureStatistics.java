package atf.ui.network;

/** Counts metadata only; append-only cursors remain valid when capacity is reached. */
public record UiNetworkCaptureStatistics(long accepted, long retained, long excluded,
                                         long dropped, long inFlight, boolean limitReached) {
    public boolean complete() { return dropped == 0 && inFlight == 0; }
}


package atf.autoex.runtime.navigation;

/** Honest infrastructure failure; it never changes a business verdict into PASS or SKIP. */
public final class AutoExStoreUnavailableException extends IllegalStateException {
    public AutoExStoreUnavailableException(String method, String path, int attempt, int maximum, boolean safe) {
        this("queue full", method, path, attempt, maximum, safe);
    }
    public AutoExStoreUnavailableException(String reason, String method, String path, int attempt, int maximum, boolean safe) {
        super("Automation Exercise store unavailable: " + reason + " at " + method + " " + path
                + "; document attempt " + attempt + "/" + maximum
                + (safe ? "; read-only recovery exhausted" : "; document is not an eligible read-only GET"));
    }
}

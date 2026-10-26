package atf.autoex.support.account;

/** A dismissed advert is not proof that deletion ran, nor permission to replay a started request. */
public record AutoExAccountDeletionProgress(boolean vignetteDismissed, int startedRequests,
                                           boolean deletionDestinationReached) {
    public AutoExAccountDeletionProgress {
        if (startedRequests < 0) throw new IllegalArgumentException("startedRequests must be non-negative");
    }

    public boolean mayResumeNavigation() {
        return vignetteDismissed && startedRequests == 0 && !deletionDestinationReached;
    }
}

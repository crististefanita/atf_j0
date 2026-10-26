package atf.autoex.support.account;

/** Maps observed account-state controls to an explicit cleanup action. */
public final class AutoExAccountCleanupDecision {
    private AutoExAccountCleanupDecision() { }

    public static AutoExAccountCleanupAction resolve(boolean creationConfirmed,
                                                     boolean continueAvailable,
                                                     boolean deleteAvailable) {
        if (creationConfirmed && continueAvailable) {
            return AutoExAccountCleanupAction.CONTINUE_THEN_DELETE;
        }
        if (deleteAvailable) return AutoExAccountCleanupAction.DELETE;
        if (creationConfirmed) return AutoExAccountCleanupAction.CONFIRMED_BUT_UNAVAILABLE;
        return AutoExAccountCleanupAction.NO_OP;
    }
}

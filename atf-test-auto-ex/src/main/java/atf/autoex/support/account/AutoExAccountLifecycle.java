package atf.autoex.support.account;

import java.util.Objects;

/** One execution owns one disposable identity; page instances never own mutation state. */
public final class AutoExAccountLifecycle {
    private AutoExTestAccount account;
    private State state = State.UNTRACKED;

    public void creationAttempted(AutoExTestAccount selected) {
        Objects.requireNonNull(selected, "account");
        if (account != null && !account.equals(selected)) {
            throw new IllegalStateException("This execution already owns another disposable account");
        }
        requireDeletionNotStarted();
        account = selected;
        if (state == State.UNTRACKED) state = State.CREATE_ATTEMPTED;
    }

    public void creationConfirmed() {
        if (state == State.CREATE_ATTEMPTED) state = State.CREATED;
    }

    public void requireOwner(AutoExTestAccount selected, String observedName) {
        if (account == null || !account.equals(selected) || !account.signupName().equals(observedName)) {
            throw new IllegalStateException("Refusing account deletion: identity is not owned by this execution");
        }
    }

    public void deletionStarted() {
        if (account == null) throw new IllegalStateException("Cannot delete an unowned account");
        if (state != State.DELETED) state = State.DELETE_STARTED;
    }

    public void deletionConfirmed() {
        if (account == null) throw new IllegalStateException("Cannot confirm deletion of an unowned account");
        state = State.DELETED;
    }

    public void requireDeletionNotStarted() {
        if (state == State.DELETE_STARTED || state == State.DELETED) {
            throw new IllegalStateException("Account deletion request already started without a new mutation permit; "
                    + "neither business continuation nor failure cleanup may replay it");
        }
    }

    public boolean creationWasAttempted() { return state != State.UNTRACKED; }
    public boolean creationWasConfirmed() { return state == State.CREATED; }
    public boolean deletionWasConfirmed() { return state == State.DELETED; }

    private enum State { UNTRACKED, CREATE_ATTEMPTED, CREATED, DELETE_STARTED, DELETED }
}

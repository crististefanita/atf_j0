package atf.core.sync;

import java.io.IOException;
import java.util.List;

/** Raised when a parallel edit changed a guarded live source before promotion. */
public final class AtfPromotionConflictException extends IOException {
    private final List<String> conflictingFiles;

    public AtfPromotionConflictException(List<String> conflictingFiles) {
        super("Promotion stopped because guarded live files changed: "
                + String.join(", ", conflictingFiles));
        this.conflictingFiles = List.copyOf(conflictingFiles);
    }

    public List<String> conflictingFiles() {
        return conflictingFiles;
    }
}

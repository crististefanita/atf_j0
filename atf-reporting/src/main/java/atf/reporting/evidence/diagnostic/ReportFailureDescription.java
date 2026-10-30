package atf.reporting.evidence.diagnostic;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

/** Safe display summary of an existing failure graph; it neither changes the cause nor determines a verdict. */
public final class ReportFailureDescription {
    private static final int MAX_FAILURES = 4;
    private ReportFailureDescription() { }

    public static String describe(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        var description = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var pending = new ArrayDeque<Throwable>();
        pending.add(failure);
        while (!pending.isEmpty() && seen.size() < MAX_FAILURES) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) continue;
            if (!description.isEmpty()) description.append(" <- ");
            description.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (message != null && !message.isBlank()) description.append(": ").append(message.trim());
            if (current.getCause() != null) pending.addLast(current.getCause());
            for (Throwable suppressed : current.getSuppressed()) pending.addLast(suppressed);
        }
        String safe = new SensitiveDataRedactor().redactText(description.toString());
        int limit = ReportingSettings.DEFAULT_ASSERTION_MAX_DISPLAY_CHARACTERS;
        return safe.length() <= limit ? safe : safe.substring(0, limit - 1) + "…";
    }
}

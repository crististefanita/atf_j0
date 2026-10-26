package atf.validation.evaluation;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Collects one response's assertions and publishes them as one aggregate. */
public final class ValidationTable {
    public static final int DEFAULT_MAX_DISPLAYED_CHARACTERS = 240;

    private final int maxDisplayedCharacters;
    private final List<AtfAssertion> rows = new ArrayList<>();
    private final Set<String> names = new HashSet<>();
    private boolean finished;

    public ValidationTable() {
        this(DEFAULT_MAX_DISPLAYED_CHARACTERS);
    }

    public ValidationTable(int maxDisplayedCharacters) {
        if (maxDisplayedCharacters <= 0) throw new IllegalArgumentException("display limit must be positive");
        this.maxDisplayedCharacters = maxDisplayedCharacters;
    }

    public void check(String name, String expected, String actual, BooleanSupplier predicate) {
        try {
            add(name, expected, actual, predicate.getAsBoolean());
        } catch (RuntimeException error) {
            addFailure(name, expected, actual, error);
        }
    }

    public void add(String name, String expected, String actual, boolean passed) {
        ensureWritable();
        addIdentity(name);
        rows.add(new AtfAssertion(name, expected, preview(actual), passed));
    }

    public void addFailure(String name, String expected, String actual, Exception error) {
        ensureWritable();
        addIdentity(name);
        rows.add(new AtfAssertion(name, expected,
                preview(actual + System.lineSeparator() + error.getClass().getSimpleName() + ": " + error.getMessage()),
                false, AtfReportStatus.FAIL));
    }

    public List<AtfAssertion> rows() {
        return List.copyOf(rows);
    }

    public String preview(String value) {
        if (value == null) return "null";
        return value.length() <= maxDisplayedCharacters
                ? value
                : value.substring(0, Math.max(0, maxDisplayedCharacters - 1)) + "…";
    }

    public boolean hasFailures() {
        return rows.stream().anyMatch(row -> !row.passed());
    }

    /** Compatibility entry point; new code should inject an explicit policy. */
    public ValidationDecision decide(boolean warningMode) {
        return (warningMode ? ValidationOutcomePolicy.warning() : ValidationOutcomePolicy.strict()).decide(this);
    }

    public boolean finished() { return finished; }

    void publish() {
        ensureWritable();
        finished = true;
    }

    private void ensureWritable() {
        if (finished) throw new IllegalStateException("ValidationTable is already published");
    }

    private void addIdentity(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("assertion name must not be blank");
        if (!names.add(name.trim().toLowerCase())) throw new IllegalArgumentException("duplicate assertion: " + name);
    }
}

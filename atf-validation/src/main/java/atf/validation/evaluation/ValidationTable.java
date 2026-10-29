package atf.validation.evaluation;
import atf.core.reporting.contract.AtfAssertion;
import atf.core.reporting.contract.AtfReportStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.Locale;
import java.util.Objects;

/** Collects one response's assertions and publishes them as one aggregate. */
public final class ValidationTable {
    public static final int DEFAULT_MAX_DISPLAYED_CHARACTERS = 240;

    private final int maxDisplayedCharacters;
    private final atf.core.security.SensitiveDataRedactor redactor =
            new atf.core.security.SensitiveDataRedactor();
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
        ensureWritable();
        ensureNewIdentity(name);
        Objects.requireNonNull(predicate, "predicate");
        boolean passed;
        try {
            passed = predicate.getAsBoolean();
        } catch (RuntimeException error) {
            addFailure(name, expected, actual, error);
            return;
        }
        add(name, expected, actual, passed);
    }

    public void add(String name, String expected, String actual, boolean passed) {
        ensureWritable();
        addIdentity(name);
        rows.add(new AtfAssertion(preview(name), fieldPreview(name, expected), fieldPreview(name, actual), passed));
    }

    public void addFailure(String name, String expected, String actual, Exception error) {
        ensureWritable();
        addIdentity(name);
        rows.add(new AtfAssertion(preview(name), fieldPreview(name, expected),
                fieldPreview(name, actual + System.lineSeparator() + error.getClass().getSimpleName() + ": " + error.getMessage()),
                false, AtfReportStatus.FAIL));
    }

    public List<AtfAssertion> rows() {
        return List.copyOf(rows);
    }

    public String preview(String value) {
        return bounded(redactor.redactText(value == null ? "null" : value));
    }

    private String fieldPreview(String name, String value) {
        return bounded(redactor.redactField(name, value == null ? "null" : value));
    }

    private String bounded(String value) {
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
        ensureNewIdentity(name);
        names.add(name.trim().toLowerCase(Locale.ROOT));
    }

    private void ensureNewIdentity(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("assertion name must not be blank");
        if (names.contains(name.trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("duplicate assertion: " + name);
    }
}

package atf.testkit.assertions;

import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** JUnit comparisons with immutable, renderer-independent evidence of evaluated checks. */
public final class RecordedAssertions {
    public static final int DEFAULT_MAX_DISPLAYED_CHARACTERS = 240;
    public record Check(String name, String expected, String actual, boolean passed) { }

    private final List<Check> checks = new ArrayList<>();
    private final int maxDisplayedCharacters;

    public RecordedAssertions() { this(DEFAULT_MAX_DISPLAYED_CHARACTERS); }

    public RecordedAssertions(int maxDisplayedCharacters) {
        if (maxDisplayedCharacters <= 0) throw new IllegalArgumentException("display limit must be positive");
        this.maxDisplayedCharacters = maxDisplayedCharacters;
    }

    void record(String name, Object expected, Object actual, boolean passed) {
        checks.add(new Check(preview(name), preview(expected), preview(actual), passed));
    }

    private String preview(Object value) {
        String text = value instanceof Object[] array ? java.util.Arrays.deepToString(array) : String.valueOf(value);
        return text.length() <= maxDisplayedCharacters ? text
                : text.substring(0, maxDisplayedCharacters - 1) + "…";
    }

    public void equals(String name, Object expected, Object actual) {
        record(name, expected, actual, Objects.equals(expected, actual));
        Assertions.assertEquals(expected, actual, name);
    }

    public void isTrue(String name, boolean actual) {
        record(name, "true", actual, actual);
        Assertions.assertTrue(actual, name);
    }

    public void isFalse(String name, boolean actual) {
        record(name, "false", actual, !actual);
        Assertions.assertFalse(actual, name);
    }

    public List<Check> checks() { return List.copyOf(checks); }
}

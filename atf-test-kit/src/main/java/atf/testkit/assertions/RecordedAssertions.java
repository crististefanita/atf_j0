package atf.testkit.assertions;

import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** JUnit comparisons with immutable, renderer-independent evidence of evaluated checks. */
public final class RecordedAssertions {
    public record Check(String name, String expected, String actual, boolean passed) { }

    private final List<Check> checks = new ArrayList<>();

    public void equals(String name, Object expected, Object actual) {
        checks.add(new Check(name, String.valueOf(expected), String.valueOf(actual),
                Objects.equals(expected, actual)));
        Assertions.assertEquals(expected, actual, name);
    }

    public void isTrue(String name, boolean actual) {
        checks.add(new Check(name, "true", Boolean.toString(actual), actual));
        Assertions.assertTrue(actual, name);
    }

    public void isFalse(String name, boolean actual) {
        checks.add(new Check(name, "false", Boolean.toString(actual), !actual));
        Assertions.assertFalse(actual, name);
    }

    public List<Check> checks() { return List.copyOf(checks); }
}

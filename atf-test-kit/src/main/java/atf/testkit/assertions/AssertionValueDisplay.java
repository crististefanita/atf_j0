package atf.testkit.assertions;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

/** Safe projection only: sanitize the complete value before enforcing the display budget. */
public final class AssertionValueDisplay {
    public static final int DEFAULT_LIMIT = 240;
    private final int limit;
    private final BiFunction<String, String, String> sanitizer;
    public enum Role { EXPECTED, ACTUAL }
    public record Projection(String name, Role role, String fullText, boolean truncated) { }
    private final java.util.function.Consumer<Projection> observer;

    @FunctionalInterface
    interface Value { String text(); }

    public AssertionValueDisplay(int limit, BiFunction<String, String, String> sanitizer) {
        this(limit, sanitizer, ignored -> { });
    }

    public AssertionValueDisplay(int limit, BiFunction<String, String, String> sanitizer,
                                 java.util.function.Consumer<Projection> observer) {
        if (limit <= 0) throw new IllegalArgumentException("display limit must be positive");
        this.limit = limit;
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public String display(String field, Object value) {
        return project(field, null, value);
    }

    public String comparison(String name, Role role, Object value) {
        return project(name, Objects.requireNonNull(role, "role"), value);
    }

    private String project(String field, Role role, Object value) {
        String safe;
        try {
            String text = value instanceof Value lazy ? lazy.text()
                    : value instanceof Object[] array ? Arrays.deepToString(array) : String.valueOf(value);
            safe = Objects.requireNonNull(sanitizer.apply(field, text), "sanitized value");
        } catch (RuntimeException | AssertionError | LinkageError | StackOverflowError unavailable) {
            // Never retry a broken toString/sanitizer or expose its potentially sensitive message.
            safe = "<display unavailable>";
        }
        if (role != null) observer.accept(new Projection(field, role, safe, safe.length() > limit));
        return safe.length() <= limit ? safe : safe.substring(0, limit - 1) + "…";
    }

    static Object prefix(String prefix, Object value) {
        return (Value) () -> prefix + (value instanceof Value lazy ? lazy.text() : String.valueOf(value));
    }

    static Object identity(Object value) {
        return (Value) () -> value == null ? "null"
                : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }

    static Object type(Class<?> value) { return (Value) () -> value == null ? "null" : value.getName(); }
}

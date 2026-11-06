package atf.testkit.assertions;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.util.function.Function;

/** Per-invocation audit binding; nested Jupiter launches and expected failures stay isolated. */
public final class AuditedAssertionScope implements AutoCloseable {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final StackWalker STACK = StackWalker.getInstance();
    private final Frame frame;
    private final Frame previous;

    private static final class Frame {
        final String owner;
        final String method;
        final RecordedAssertions assertions;
        int suppressed;
        boolean captureFailed;

        Frame(String owner, String method, RecordedAssertions assertions) {
            this.owner = owner;
            this.method = method;
            this.assertions = assertions;
        }
    }

    private AuditedAssertionScope(String owner, String method, RecordedAssertions assertions) {
        previous = CURRENT.get();
        frame = new Frame(owner, method, assertions);
        CURRENT.set(frame);
    }

    public static AuditedAssertionScope open(String owner, String method, RecordedAssertions assertions) {
        return new AuditedAssertionScope(java.util.Objects.requireNonNull(owner, "owner"),
                java.util.Objects.requireNonNull(method, "method"),
                java.util.Objects.requireNonNull(assertions, "assertions"));
    }

    @Override public void close() {
        if (CURRENT.get() != frame) throw new IllegalStateException("Assertion scopes must close in order");
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }

    private static Frame owned() {
        Frame frame = CURRENT.get();
        if (frame == null) return null;
        boolean owned = STACK.walk(frames -> frames
                .takeWhile(call -> !call.getClassName().startsWith("org.junit.jupiter.engine."))
                .anyMatch(call -> call.getClassName().equals(frame.owner) && call.getMethodName().equals(frame.method)));
        return owned ? frame : null;
    }

    private static Frame active() {
        Frame frame = owned();
        return frame == null || frame.suppressed > 0 ? null : frame;
    }

    static boolean isSuppressed() {
        Frame frame = owned();
        return frame != null && frame.suppressed > 0;
    }

    static void explicit(RecordedAssertions recorder, String name, Object expected, Object actual,
                         Executable executable) {
        Throwable failure = null;
        try { executable.execute(); } catch (Throwable error) { failure = error; }
        Throwable original = failure;
        failure = recordSafely(recorder, name, expected, actual, original, failure);
        Frame frame = active();
        if (frame != null && frame.assertions != recorder)
            failure = recordSafely(frame.assertions, name, expected, actual, original, failure);
        if (failure != null) AuditedAssertionScope.<Void, RuntimeException>rethrow(failure);
    }

    private static String name(String operation, String message) {
        String source = STACK.walk(frames -> frames
                .filter(call -> !call.getClassName().startsWith("atf.testkit.assertions.")
                        && !call.getClassName().startsWith("org.junit."))
                .findFirst().map(call -> call.getFileName() + ":" + call.getLineNumber()).orElse("unknown source"));
        return operation + " · " + source + (message == null || message.isBlank() ? "" : " · " + message);
    }

    static void check(String operation, Object expected, Object actual, String message, Executable executable) {
        execute(operation, expected, actual, message, () -> { executable.execute(); return null; }, null, false);
    }

    static <T> T value(String operation, Object expected, Object actual, String message,
                       ThrowingSupplier<T> executable, Function<T, Object> observed, boolean suppress) {
        return execute(operation, expected, actual, message, executable, observed, suppress);
    }

    private static <T> T execute(String operation, Object expected, Object actual, String message,
                                 ThrowingSupplier<T> executable, Function<T, Object> observed, boolean suppress) {
        Frame frame = active();
        String label = frame == null ? "" : name(operation, message);
        if (frame != null && suppress) frame.suppressed++;
        T result = null;
        Throwable failure = null;
        try { result = executable.get(); } catch (Throwable error) { failure = error; } finally {
            if (frame != null && suppress) frame.suppressed--;
        }
        if (frame != null) {
            T returned = result;
            Throwable thrown = failure;
            Object projection = observed == null ? actual : (AssertionValueDisplay.Value) () ->
                    String.valueOf(thrown == null ? observed.apply(returned) : thrown.getMessage());
            failure = recordSafely(frame.assertions, label, expected, projection, failure, failure);
        }
        if (failure != null) return rethrow(failure);
        return result;
    }

    static void group(String message, Executable executable) {
        Frame frame = active();
        int before = frame == null ? 0 : frame.assertions.checks().size();
        Throwable failure = null;
        try { executable.execute(); } catch (Throwable error) { failure = error; }
        if (frame != null && !frame.captureFailed && (failure == null ? frame.assertions.checks().size() == before
                : frame.assertions.checks().subList(before, frame.assertions.checks().size()).stream().allMatch(RecordedAssertions.Check::passed))) {
            Throwable original = failure;
            failure = recordSafely(frame.assertions, name("assertAll", message), "all checks succeed",
                    original == null ? "all checks succeeded" : (AssertionValueDisplay.Value) original::getMessage, original, failure);
        }
        if (failure != null) AuditedAssertionScope.<Void, RuntimeException>rethrow(failure);
    }

    private static Throwable recordSafely(RecordedAssertions recorder, String name, Object expected, Object actual,
                                          Throwable original, Throwable primary) {
        try { recorder.record(name, expected, actual, original == null, original); }
        catch (Throwable capture) {
            Frame frame = owned();
            if (frame != null) {
                frame.captureFailed = true;
                if (frame.assertions != recorder) frame.assertions.captureFailure(primary == null ? capture : primary);
            }
            if (primary == null) return capture;
            if (capture != primary) primary.addSuppressed(capture);
        }
        return primary;
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T rethrow(Throwable failure) throws E { throw (E) failure; }
}

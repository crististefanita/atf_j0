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
        return new AuditedAssertionScope(owner, method, assertions);
    }

    @Override public void close() {
        if (CURRENT.get() != frame) throw new IllegalStateException("Assertion scopes must close in order");
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }

    private static Frame active() {
        Frame frame = CURRENT.get();
        if (frame == null || frame.suppressed > 0) return null;
        boolean owned = STACK.walk(frames -> frames
                .takeWhile(call -> !call.getClassName().startsWith("org.junit.jupiter.engine."))
                .anyMatch(call -> call.getClassName().equals(frame.owner) && call.getMethodName().equals(frame.method)));
        return owned ? frame : null;
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
        try {
            T result = executable.get();
            if (frame != null) frame.assertions.record(label, expected,
                    observed == null ? actual : observed.apply(result), true);
            return result;
        } catch (Throwable error) {
            if (frame != null) frame.assertions.record(label, expected,
                    observed == null ? actual : error.getMessage(), false);
            return rethrow(error);
        } finally {
            if (frame != null && suppress) frame.suppressed--;
        }
    }

    static void group(String message, Executable executable) {
        Frame frame = active();
        int before = frame == null ? 0 : frame.assertions.checks().size();
        try {
            executable.execute();
            if (frame != null && frame.assertions.checks().size() == before)
                frame.assertions.record(name("assertAll", message), "all checks succeed", "all checks succeeded", true);
        } catch (Throwable error) {
            if (frame != null && frame.assertions.checks().subList(before, frame.assertions.checks().size())
                    .stream().allMatch(RecordedAssertions.Check::passed))
                frame.assertions.record(name("assertAll", message), "all checks succeed", error.getMessage(), false);
            AuditedAssertionScope.<Void, RuntimeException>rethrow(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T rethrow(Throwable failure) throws E { throw (E) failure; }
}

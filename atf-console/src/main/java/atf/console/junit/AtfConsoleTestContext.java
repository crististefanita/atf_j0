package atf.console.junit;

import atf.console.AtfConsoleReporter;
import atf.core.reporting.contract.AtfReportStatus;
import atf.core.reporting.console.AtfConsoleSink;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.TestAbortedException;

import java.util.Objects;

/** Thread-bound bridge from a directly running JUnit test to its console lifecycle. */
public final class AtfConsoleTestContext {
    private static final ThreadLocal<AtfConsoleTestLifecycle> CURRENT = new ThreadLocal<>();

    private AtfConsoleTestContext() { }

    static void install(AtfConsoleTestLifecycle lifecycle) { CURRENT.set(lifecycle); }
    static void clear() { CURRENT.remove(); }
    public static AtfConsoleTestLifecycle current() { return CURRENT.get(); }

    /** Reuses the active test's policy and sink, or falls back to configured process stdout. */
    public static AtfConsoleSink configuredSink() {
        AtfConsoleTestLifecycle lifecycle = CURRENT.get();
        return lifecycle == null ? AtfConsoleReporter.configuredStdout() : lifecycle.console();
    }

    /**
     * Creates a dynamic child with the same annotation/YAML console policy as an ordinary test.
     * Jupiter does not invoke {@code BeforeEachCallback} or {@code TestWatcher} for dynamic children,
     * so the child must be wrapped explicitly to get an auditable per-child lifecycle.
     */
    public static DynamicTest dynamicTest(Class<?> owner, String displayName, Executable body) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(body, "body");
        String name = requireText(displayName, "displayName");
        return DynamicTest.dynamicTest(name, () -> executeDynamic(owner, name, body));
    }

    private static void executeDynamic(Class<?> owner, String name, Executable body) throws Throwable {
        var lifecycle = AtfConsoleTestExtension.lifecycle(owner, owner.getSimpleName(), name);
        lifecycle.start();
        install(lifecycle);
        try {
            body.execute();
            lifecycle.complete(AtfReportStatus.PASS);
        } catch (TestAbortedException aborted) {
            lifecycle.complete(AtfReportStatus.SKIP);
            throw aborted;
        } catch (Throwable failure) {
            lifecycle.complete(AtfReportStatus.FAIL);
            throw failure;
        } finally {
            clear();
        }
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}

package atf.core.http.execution;

import java.util.concurrent.TimeUnit;

/**
 * Monotonic time boundary used by one API execution.
 *
 * <p>The production implementation delegates to the system clock; tests can
 * provide a deterministic implementation without wall-clock waits.</p>
 */
public interface ApiExecutionClock {
    long nanoTime();

    void sleepNanos(long nanos) throws InterruptedException;

    static ApiExecutionClock system() {
        return SystemClock.INSTANCE;
    }

    enum SystemClock implements ApiExecutionClock {
        INSTANCE;

        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public void sleepNanos(long nanos) throws InterruptedException {
            if (nanos > 0) {
                TimeUnit.NANOSECONDS.sleep(nanos);
            }
        }
    }
}

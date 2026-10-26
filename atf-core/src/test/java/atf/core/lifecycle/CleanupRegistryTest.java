package atf.core.lifecycle;
import atf.testkit.AtfUnitTest;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class CleanupRegistryTest {
    @Test
    void cleanupRunsInReverseRegistrationOrderAndContinuesAfterFailure() {
        var calls = new ArrayList<String>();
        var registry = new CleanupRegistry();
        registry.register(() -> calls.add("first"));
        registry.register(() -> { calls.add("second"); throw new IllegalStateException("demo"); });
        registry.register(() -> calls.add("third"));

        var result = registry.run();

        assertEquals(3, result.executed());
        assertEquals(java.util.List.of("third", "second", "first"), calls);
        assertEquals(1, result.failures().size());
    }

    @Test
    void concurrentRunExecutesCleanupOnceAndSharesTheResult() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var registry = new CleanupRegistry();
        registry.register(() -> {
            calls.incrementAndGet();
            started.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        });

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(registry::run);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            var second = executor.submit(registry::run);
            release.countDown();

            var firstResult = first.get(5, TimeUnit.SECONDS);
            var secondResult = second.get(5, TimeUnit.SECONDS);
            assertSame(firstResult, secondResult);
            assertEquals(1, calls.get());
        }
    }

    @Test
    void registrationAfterRunIsRejectedToProtectLifecycleOwnership() {
        var registry = new CleanupRegistry();
        registry.run();

        assertThrows(IllegalStateException.class, () -> registry.register(() -> { }));
        assertEquals(0, registry.run().executed());
    }
}

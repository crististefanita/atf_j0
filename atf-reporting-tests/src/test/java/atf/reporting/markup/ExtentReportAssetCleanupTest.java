package atf.reporting.markup;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentReportAssetCleanupTest {
    @AfterEach
    void clearTestInterrupt() {
        Thread.interrupted();
    }

    @ParameterizedTest
    @EnumSource(LockKind.class)
    void retriesTransientWindowsStyleLockAndRemovesOnlyGeneratedSparkAssets(LockKind lock, @TempDir Path root)
            throws Exception {
        Path outside = Files.writeString(root.resolve("keep.txt"), "outside generated assets");
        Path spark = Files.createDirectory(root.resolve(ExtentReportAssets.SPARK_DIRECTORY));
        Path asset = Files.writeString(spark.resolve("spark.js"), "generated");
        AtomicInteger attempts = new AtomicInteger();
        List<Long> pauses = new ArrayList<>();

        ExtentReportAssets.removeGeneratedAssets(spark, path -> {
            if (path.equals(asset) && attempts.incrementAndGet() <= 2) {
                throw lock.failure(path);
            }
            return Files.deleteIfExists(path);
        }, pauses::add);

        assertEquals(3, attempts.get(), "temporary access denial should retry with a bounded count");
        assertEquals(List.of(50L, 50L), pauses, "every retry uses the bounded 50 ms delay");
        assertFalse(Files.exists(spark), "generated Spark assets should be removed after unlock");
        assertTrue(Files.exists(outside), "cleanup must not escape its generated Spark directory");
    }

    @ParameterizedTest
    @EnumSource(LockKind.class)
    void reportsPersistentLockAfterBoundedRetriesAndKeepsPublishedOutputOutsideTheTarget(LockKind lock, @TempDir Path root)
            throws Exception {
        Path outside = Files.writeString(root.resolve("published-report.html"), "complete report");
        Path spark = Files.createDirectory(root.resolve(ExtentReportAssets.SPARK_DIRECTORY));
        Path asset = Files.writeString(spark.resolve("locked.js"), "generated");
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger pauses = new AtomicInteger();
        var original = lock.failure(asset);

        IOException failure = assertThrows(IOException.class, () ->
                ExtentReportAssets.removeGeneratedAssets(spark, path -> {
                    attempts.incrementAndGet();
                    throw original;
                }, millis -> {
                    assertEquals(50L, millis);
                    pauses.incrementAndGet();
                }));

        assertTrue(failure.getMessage().contains("after 4 attempts"));
        assertTrue(failure.getMessage().contains(asset.toString()));
        assertEquals(4, attempts.get(), "cleanup must stop after its configured attempt count");
        assertEquals(3, pauses.get(), "there is no delay after the last failed attempt");
        assertSame(original, failure.getCause(), "the last filesystem failure remains actionable");
        assertTrue(Files.exists(asset), "a locked generated file must not be claimed as deleted");
        assertTrue(Files.exists(outside), "cleanup failure must not alter the published report");
    }

    @Test
    void unrelatedIoFailureIsNotRetriedOrReplaced(@TempDir Path root) throws Exception {
        Path spark = Files.createDirectory(root.resolve(ExtentReportAssets.SPARK_DIRECTORY));
        AtomicInteger attempts = new AtomicInteger();
        var original = new IOException("unrelated I/O failure");

        IOException actual = assertThrows(IOException.class, () ->
                ExtentReportAssets.removeGeneratedAssets(spark, path -> {
                    attempts.incrementAndGet();
                    throw original;
                }, millis -> { throw new AssertionError("ordinary I/O must not be retried"); }));

        assertSame(original, actual);
        assertEquals(1, attempts.get());
    }

    @Test
    void interruptedRetryPreservesTheInterruptAndStopsCleanup(@TempDir Path root) throws Exception {
        Path spark = Files.createDirectory(root.resolve(ExtentReportAssets.SPARK_DIRECTORY));
        AtomicInteger attempts = new AtomicInteger();
        var interruption = new InterruptedException("cleanup interrupted");
        IOException actual = assertThrows(IOException.class, () ->
                ExtentReportAssets.removeGeneratedAssets(spark, path -> {
                    attempts.incrementAndGet();
                    throw LockKind.SHARING_VIOLATION.failure(path);
                }, millis -> { throw interruption; }));
        assertTrue(Thread.currentThread().isInterrupted());
        assertSame(interruption, actual.getCause());
        assertEquals(1, attempts.get());
    }

    @Test
    void successfulDeletionDoesNotPauseOrRetry(@TempDir Path root) throws Exception {
        Path spark = Files.createDirectory(root.resolve(ExtentReportAssets.SPARK_DIRECTORY));
        AtomicInteger attempts = new AtomicInteger();
        ExtentReportAssets.removeGeneratedAssets(spark, path -> {
            attempts.incrementAndGet();
            return Files.deleteIfExists(path);
        }, millis -> { throw new AssertionError("successful cleanup must not pause"); });
        assertEquals(1, attempts.get());
        assertFalse(Files.exists(spark));
    }

    @Test
    void nonSparkAndAbsentTargetsNeverInvokeDeletion(@TempDir Path root) throws Exception {
        Path outside = Files.createDirectory(root.resolve("consumer-owned"));
        Path absent = root.resolve(ExtentReportAssets.SPARK_DIRECTORY);
        for (Path target : List.of(outside, absent)) {
            ExtentReportAssets.removeGeneratedAssets(target,
                    path -> { throw new AssertionError("target is not a generated Spark directory"); },
                    millis -> { throw new AssertionError("no cleanup means no retry"); });
        }
        assertTrue(Files.isDirectory(outside));
        assertFalse(Files.exists(absent));
    }

    private enum LockKind {
        ACCESS_DENIED, SHARING_VIOLATION;

        FileSystemException failure(Path path) {
            return this == ACCESS_DENIED
                    ? new AccessDeniedException(path.toString(), null, "simulated Windows file lock")
                    : new FileSystemException(path.toString(), null, "file is being used by another process");
        }
    }
}

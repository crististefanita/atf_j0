package atf.reporting.publication;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportStagingWorkspaceTest {
    @Test
    void sweeperDeletesOnlyOldMarkedWorkspaces() throws Exception {
        Path root = Files.createTempDirectory("atf-workspace-sweeper-");
        try {
            Path stale = createMarkedWorkspace(root, Instant.parse("2020-01-01T00:00:00Z"));
            Path fresh = createMarkedWorkspace(root, Instant.parse("2026-10-14T10:59:00Z"));
            Path unmarked = Files.createDirectory(root.resolve(ReportStagingWorkspace.PREFIX + "unmarked"));

            var result = ReportStagingWorkspace.sweep(root, Duration.ofMinutes(5),
                    Instant.parse("2026-10-14T11:00:00Z"));

            assertAll(
                    () -> assertEquals(3, result.scanned()),
                    () -> assertEquals(1, result.deleted()),
                    () -> assertEquals(2, result.skipped()),
                    () -> assertTrue(Files.notExists(stale)),
                    () -> assertTrue(Files.exists(fresh)),
                    () -> assertTrue(Files.exists(unmarked)));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void sweeperNeverDeletesAnActiveWorkspace() throws Exception {
        Path root = Files.createTempDirectory("atf-active-workspace-");
        ReportStagingWorkspace workspace = ReportStagingWorkspace.create(root, root.resolve("report.html"));
        try {
            var result = ReportStagingWorkspace.sweep(root, Duration.ofNanos(1),
                    Instant.now().plusSeconds(10));
            assertEquals(1, result.scanned());
            assertEquals(0, result.deleted());
            assertEquals(1, result.skipped());
            assertTrue(Files.exists(workspace.directory()));
        } finally {
            workspace.close();
            deleteTree(root);
        }
    }

    @Test
    void closeReleasesLeaseAndRemovesWorkspace() throws Exception {
        Path root = Files.createTempDirectory("atf-workspace-close-");
        try {
            var workspace = ReportStagingWorkspace.create(root, root.resolve("report.html"));
            Path directory = workspace.directory();
            assertTrue(Files.exists(directory.resolve(".atf-workspace.properties")));
            workspace.close();
            workspace.close();
            assertTrue(Files.notExists(directory));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void closeIsOneShotWhenSeveralOwnersReleaseTheSameWorkspaceConcurrently() throws Exception {
        Path root = Files.createTempDirectory("atf-concurrent-close-");
        var workspace = ReportStagingWorkspace.create(root, root.resolve("report.html"));
        Path directory = workspace.directory();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 32; i++) {
                executor.submit(workspace::close);
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            assertTrue(Files.notExists(directory), "the owning workspace must be removed exactly once");
        } finally {
            workspace.close();
            deleteTree(root);
        }
    }

    @Test
    void sweeperHonoursTheCandidateLimitAndReportsThatItWasBounded() throws Exception {
        Path root = Files.createTempDirectory("atf-bounded-sweeper-");
        try {
            for (int i = 0; i < 5; i++) {
                createMarkedWorkspace(root, Instant.parse("2020-01-01T00:00:00Z").plusSeconds(i));
            }

            var result = ReportStagingWorkspace.sweep(root, Duration.ofMinutes(5),
                    Instant.parse("2026-10-14T11:00:00Z"), 2);

            assertAll(
                    () -> assertEquals(2, result.scanned()),
                    () -> assertEquals(2, result.deleted()),
                    () -> assertEquals(3, result.skipped()),
                    () -> assertTrue(result.limitReached()));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void sweeperRejectsInvalidAgeAndCandidateBounds() throws Exception {
        Path root = Files.createTempDirectory("atf-workspace-validation-");
        try {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> ReportStagingWorkspace.sweep(root, Duration.ZERO,
                                    Instant.now(), 1)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> ReportStagingWorkspace.sweep(root, Duration.ofSeconds(1),
                                    Instant.now(), 0)));
        } finally {
            deleteTree(root);
        }
    }

    private static Path createMarkedWorkspace(Path root, Instant createdAt) throws Exception {
        Path directory = Files.createDirectory(root.resolve(ReportStagingWorkspace.PREFIX + createdAt.toEpochMilli()));
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("createdAt", createdAt.toString());
        try (var output = Files.newOutputStream(directory.resolve(".atf-workspace.properties"))) {
            properties.store(output, "test");
        }
        Files.createFile(directory.resolve("report.html"));
        Files.createFile(directory.resolve(".atf-workspace.lock"));
        return directory;
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException error) {
                    throw new RuntimeException(error);
                }
            });
        }
    }
}

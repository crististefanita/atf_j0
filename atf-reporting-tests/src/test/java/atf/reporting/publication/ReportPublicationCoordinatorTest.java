package atf.reporting.publication;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportPublicationCoordinatorTest {
    @Test
    void serializesConcurrentWritersToOneFinalPathWithoutPartialContent() throws Exception {
        Path root = Files.createTempDirectory("atf-publication-lease-");
        Path destination = root.resolve("report.html");
        Path first = root.resolve("first.html");
        Path second = root.resolve("second.html");
        Files.writeString(first, "<html><body>first-complete</body></html>");
        Files.writeString(second, "<html><body>second-complete</body></html>");
        try {
            try (var executor = Executors.newFixedThreadPool(2)) {
                var firstWriter = executor.submit(() -> publish(first, destination));
                var secondWriter = executor.submit(() -> publish(second, destination));
                firstWriter.get(30, TimeUnit.SECONDS);
                secondWriter.get(30, TimeUnit.SECONDS);
            }

            String published = Files.readString(destination);
            assertTrue(published.equals("<html><body>first-complete</body></html>")
                    || published.equals("<html><body>second-complete</body></html>"));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (java.io.IOException error) { throw new RuntimeException(error); }
                });
            }
        }
    }

    @Test
    void refusesBlockedDestinationWithoutConsumingTheStagedArtifact() throws Exception {
        Path root = Files.createTempDirectory("atf-publication-blocked-");
        Path staged = root.resolve("staged.html");
        Path destination = root.resolve("blocked.html");
        Files.writeString(staged, "<html><body>complete</body></html>");
        Files.createDirectory(destination);
        try {
            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                    () -> ReportPublicationCoordinator.publishAtomic(staged, destination));
            assertTrue(Files.exists(staged), "blocked publication must preserve the staged artifact");
            assertTrue(Files.isDirectory(destination), "blocked publication must not replace the destination");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (java.io.IOException error) { throw new RuntimeException(error); }
                });
            }
        }
    }

    private static void publish(Path staged, Path destination) {
        try {
            ReportPublicationCoordinator.publishAtomic(staged, destination);
        } catch (java.io.IOException error) {
            throw new RuntimeException(error);
        }
    }
}

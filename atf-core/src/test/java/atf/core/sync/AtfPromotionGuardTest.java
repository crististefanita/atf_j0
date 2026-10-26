package atf.core.sync;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfPromotionGuardTest {
    @Test
    void manifestIsPortableAndDetectsChangedLiveContent() throws Exception {
        Path root = Files.createTempDirectory("atf-manifest-");
        try {
            Path source = root.resolve("src/main/Example.java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, "before");
            AtfPromotionManifest manifest = AtfPromotionManifest.capture(root,
                    List.of(Path.of("src/main/Example.java")));
            Path json = root.resolve("manifest.json");
            manifest.write(json);

            assertFalse(Files.readString(json).contains(root.toString()));
            assertTrue(manifest.mismatches(root).isEmpty());
            Files.writeString(source, "after");
            assertEquals(List.of("src/main/Example.java"), manifest.mismatches(root));
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void promotesOnlyApprovedFilesAndKeepsTheStagingSource() throws Exception {
        Path live = Files.createTempDirectory("atf-live-");
        Path stage = Files.createTempDirectory("atf-stage-");
        try {
            Path approved = live.resolve("docs/README.md");
            Path untouched = live.resolve("parallel-change.txt");
            Files.createDirectories(approved.getParent());
            Files.writeString(approved, "live-before");
            Files.writeString(untouched, "live-only");
            Path stagedApproved = stage.resolve("docs/README.md");
            Files.createDirectories(stagedApproved.getParent());
            Files.writeString(stagedApproved, "stage-after");
            Files.writeString(stage.resolve("parallel-change.txt"), "must-not-promote");

            AtfPromotionManifest baseline = AtfPromotionManifest.capture(live,
                    List.of(Path.of("docs/README.md")));
            AtfPromotionGuard.PromotionResult result =
                    AtfPromotionGuard.promote(stage, live, baseline);

            assertEquals(List.of("docs/README.md"), result.promotedFiles());
            assertEquals("stage-after", Files.readString(approved));
            assertEquals("live-only", Files.readString(untouched));
            assertEquals("stage-after", Files.readString(stagedApproved));
        } finally {
            deleteTree(stage);
            deleteTree(live);
        }
    }

    @Test
    void stopsBeforeCopyWhenAParallelLiveEditChangesTheBaseline() throws Exception {
        Path live = Files.createTempDirectory("atf-live-conflict-");
        Path stage = Files.createTempDirectory("atf-stage-conflict-");
        try {
            Path liveFile = live.resolve("README.md");
            Files.writeString(liveFile, "before");
            Path stagedFile = stage.resolve("README.md");
            Files.writeString(stagedFile, "stage");
            AtfPromotionManifest baseline = AtfPromotionManifest.capture(live,
                    List.of(Path.of("README.md")));
            Files.writeString(liveFile, "parallel-edit");

            AtfPromotionConflictException error = assertThrows(AtfPromotionConflictException.class,
                    () -> AtfPromotionGuard.promote(stage, live, baseline));
            assertEquals(List.of("README.md"), error.conflictingFiles());
            assertEquals("parallel-edit", Files.readString(liveFile));
            assertEquals("stage", Files.readString(stagedFile));
        } finally {
            deleteTree(stage);
            deleteTree(live);
        }
    }

    @Test
    void promotesApprovedNewFilesOnlyWhenLiveWasStillAbsent() throws Exception {
        Path live = Files.createTempDirectory("atf-live-new-");
        Path stage = Files.createTempDirectory("atf-stage-new-");
        try {
            Path stagedFile = stage.resolve("src/main/New.java");
            Files.createDirectories(stagedFile.getParent());
            Files.writeString(stagedFile, "new");

            AtfPromotionManifest baseline = AtfPromotionManifest.capture(live,
                    List.of(Path.of("src/main/New.java")));
            assertFalse(baseline.entries().getFirst().present());
            AtfPromotionGuard.promote(stage, live, baseline);

            assertEquals("new", Files.readString(live.resolve("src/main/New.java")));
        } finally {
            deleteTree(stage);
            deleteTree(live);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (java.io.IOException error) { throw new RuntimeException(error); }
            });
        }
    }
}

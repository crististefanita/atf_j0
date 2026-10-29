package atf.core.sync;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AtfPromotionPreflightTest {
    @TempDir Path root;

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void aMissingOrIneligibleLaterSourceRefusesEveryWrite(boolean directory) throws Exception {
        var live = Files.createDirectories(root.resolve("live")); var stage = Files.createDirectories(root.resolve("stage"));
        Files.writeString(live.resolve("a.txt"), "old-a"); Files.writeString(live.resolve("b.txt"), "old-b");
        Files.writeString(stage.resolve("a.txt"), "new-a");
        if (directory) Files.createDirectory(stage.resolve("b.txt"));
        var before = AtfPromotionManifest.capture(live, List.of(Path.of("a.txt"), Path.of("b.txt")));
        assertThrows(java.io.IOException.class, () -> AtfPromotionGuard.promote(stage, live, before));
        assertEquals("old-a", Files.readString(live.resolve("a.txt"))); assertEquals("old-b", Files.readString(live.resolve("b.txt")));
        assertTrue(before.mismatches(live).isEmpty()); assertEquals("new-a", Files.readString(stage.resolve("a.txt")));
    }

    @Test void aCompletePlanWritesOnlyTheApprovedFilesAndPreservesTheSources() throws Exception {
        var live = Files.createDirectories(root.resolve("live")); var stage = Files.createDirectories(root.resolve("stage"));
        Files.writeString(live.resolve("a.txt"), "old-a");
        Files.writeString(stage.resolve("a.txt"), "new-a"); Files.writeString(stage.resolve("b.txt"), "new-b");
        Files.writeString(stage.resolve("unapproved.txt"), "excluded");
        var before = AtfPromotionManifest.capture(live, List.of(Path.of("b.txt"), Path.of("a.txt")));
        var result = AtfPromotionGuard.promote(stage, live, before);
        assertEquals(List.of("a.txt", "b.txt"), result.promotedFiles());
        assertEquals("new-a", Files.readString(live.resolve("a.txt"))); assertEquals("new-b", Files.readString(live.resolve("b.txt")));
        assertFalse(Files.exists(live.resolve("unapproved.txt"))); assertEquals("new-b", Files.readString(stage.resolve("b.txt")));
    }

    @Test void aConcurrentCreationIsRefusedBeforeAnEarlierApprovedWrite() throws Exception {
        var live = Files.createDirectories(root.resolve("live")); var stage = Files.createDirectories(root.resolve("stage"));
        Files.writeString(live.resolve("a.txt"), "old-a");
        var before = AtfPromotionManifest.capture(live, List.of(Path.of("a.txt"), Path.of("b.txt")));
        Files.writeString(stage.resolve("a.txt"), "new-a"); Files.writeString(stage.resolve("b.txt"), "new-b");
        Files.writeString(live.resolve("b.txt"), "parallel");
        assertThrows(AtfPromotionConflictException.class, () -> AtfPromotionGuard.promote(stage, live, before));
        assertEquals("old-a", Files.readString(live.resolve("a.txt"))); assertEquals("parallel", Files.readString(live.resolve("b.txt")));
    }
}

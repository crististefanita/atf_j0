package atf.core.sync;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void anInvalidDestinationAncestorRefusesEveryWriteAndCreatesNoParents(boolean beforeCapture, boolean nested) throws Exception {
        var live = Files.createDirectories(root.resolve("live")); var stage = Files.createDirectories(root.resolve("stage"));
        Path blocked = Path.of(nested ? "z-blocked/nested/b.txt" : "z-blocked/b.txt");
        Files.writeString(live.resolve("a.txt"), "old-a"); Files.writeString(stage.resolve("a.txt"), "new-a");
        Files.createDirectories(stage.resolve(blocked).getParent()); Files.writeString(stage.resolve(blocked), "new-b");
        Files.createDirectories(stage.resolve("new")); Files.writeString(stage.resolve("new/created.txt"), "new-c");
        if (beforeCapture) Files.writeString(live.resolve("z-blocked"), "owned blocking file");
        var before = AtfPromotionManifest.capture(live, List.of(Path.of("a.txt"), Path.of("new/created.txt"), blocked));
        if (!beforeCapture) Files.writeString(live.resolve("z-blocked"), "owned blocking file");
        assertTrue(before.mismatches(live).isEmpty(), "File-content drift alone cannot detect an invalid ancestor");
        assertThrows(java.io.IOException.class, () -> AtfPromotionGuard.promote(stage, live, before));
        assertEquals("old-a", Files.readString(live.resolve("a.txt")), "Preflight rejects the complete plan before the first copy");
        assertEquals("owned blocking file", Files.readString(live.resolve("z-blocked")));
        assertFalse(Files.exists(live.resolve("new")), "Preflight never creates an otherwise eligible missing parent");
        assertEquals("new-a", Files.readString(stage.resolve("a.txt")));
        assertEquals("new-b", Files.readString(stage.resolve(blocked)));
        assertEquals("new-c", Files.readString(stage.resolve("new/created.txt")));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void anExistingOrAbsentDestinationParentAllowsTheApprovedNewFile(boolean existingParent) throws Exception {
        var live = Files.createDirectories(root.resolve("live")); var stage = Files.createDirectories(root.resolve("stage"));
        Path relative = Path.of("new/nested/b.txt");
        if (existingParent) Files.createDirectories(live.resolve(relative).getParent());
        Files.createDirectories(stage.resolve(relative).getParent()); Files.writeString(stage.resolve(relative), "new-b");
        var before = AtfPromotionManifest.capture(live, List.of(relative));
        assertEquals(List.of("new/nested/b.txt"), AtfPromotionGuard.promote(stage, live, before).promotedFiles());
        assertEquals("new-b", Files.readString(live.resolve(relative)));
        assertEquals("new-b", Files.readString(stage.resolve(relative)), "Promotion preserves its source");
    }
}

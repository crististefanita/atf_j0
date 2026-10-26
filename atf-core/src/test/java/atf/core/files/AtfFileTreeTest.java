package atf.core.files;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class AtfFileTreeTest {
    @TempDir
    Path temp;

    @Test
    void collectsSelectedFilesFromMultipleRelativeRootsInStableOrder() throws Exception {
        Path source = Files.createDirectories(temp.resolve("src/main"));
        Path tests = Files.createDirectories(temp.resolve("src/test"));
        Path sourceJava = Files.createFile(source.resolve("B.java"));
        Path testJava = Files.createFile(tests.resolve("A.java"));
        Files.createFile(source.resolve("README.md"));

        assertEquals(List.of(sourceJava, testJava).stream()
                        .map(Path::toAbsolutePath).map(Path::normalize).toList(),
                AtfFileTree.regularFiles(temp, List.of(Path.of("src/main"), Path.of("src/test")),
                        path -> path.getFileName().toString().endsWith(".java")));
    }

    @Test
    void rejectsRelativeRootsThatEscapeTheConfiguredRoot() {
        assertThrows(IllegalArgumentException.class, () -> AtfFileTree.regularFiles(
                temp, List.of(Path.of("..")), path -> true));
    }

    @Test
    void detectsFilesBySuffixAndKeepsMissingRootsNonFatal() throws Exception {
        Path source = Files.createDirectories(temp.resolve("src"));
        Files.createFile(source.resolve("Example.java"));

        assertEquals(true, AtfFileTree.containsRegularFileEndingWith(source, ".java"));
        assertEquals(false, AtfFileTree.containsRegularFileEndingWith(
                temp.resolve("missing"), ".java"));
    }
}

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
class AtfJavaSourceInventoryTest {
    @TempDir
    Path temp;

    @Test
    void groupsDeclaredPackagesInStableOrder() throws Exception {
        Path source = Files.createDirectories(temp.resolve("src"));
        Path second = Files.createFile(source.resolve("B.java"));
        Files.writeString(second, "package atf.sample; class B {}");
        Path first = Files.createFile(source.resolve("A.java"));
        Files.writeString(first, "package atf.sample; class A {}");
        Files.writeString(Files.createFile(source.resolve("Default.java")), "class Default {}");

        assertEquals(List.of(first, second), AtfJavaSourceInventory.packages(source)
                .get("atf.sample"));
    }

    @Test
    void rejectsMissingSourceRoots() {
        assertThrows(IllegalArgumentException.class,
                () -> AtfJavaSourceInventory.packages(temp.resolve("missing")));
    }
}

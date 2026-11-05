package atf.testkit.documentation.guides;

import atf.testkit.AtfUnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class GuideMavenCacheTest {
    @TempDir Path root;

    @Test void onlyVendorArtifactsAndTheSelectedPublicationEnterThePrivateCache() throws Exception {
        Path seed = Files.createDirectories(root.resolve("seed")), work = Files.createDirectories(root.resolve("work"));
        write(seed, "vendor/lib/1/lib-1.jar", "vendor");
        write(seed, "vendor/lib/1/lib-1.pom", "vendor-pom");
        write(seed, "vendor/lib/1/_remote.repositories", "old remote tracking");
        write(seed, "vendor/lib/1/lib-1.jar.lastUpdated", "not-found");
        write(seed, "atf/api/atf-core/snapshot/source.jar", "parallel producer");
        write(work, "published/atf/api/atf-core/release/atf-core-release.jar", "selected binary");
        write(work, "published/atf/api/atf-core/release/atf-core-release.pom", "selected pom");
        GuideMavenCache.prepare(work, seed);
        assertEquals("vendor", Files.readString(work.resolve("m2/vendor/lib/1/lib-1.jar")));
        assertEquals("selected binary", Files.readString(work.resolve("m2/atf/api/atf-core/release/atf-core-release.jar")));
        assertFalse(Files.exists(work.resolve("m2/atf/api/atf-core/snapshot")));
        assertFalse(Files.exists(work.resolve("m2/vendor/lib/1/_remote.repositories")));
        assertFalse(Files.exists(work.resolve("m2/vendor/lib/1/lib-1.jar.lastUpdated")));
    }

    @Test void missingSeedFailsInsteadOfFallingBackToAnOnlineOrHostClasspath() {
        assertThrows(IllegalArgumentException.class, () -> GuideMavenCache.prepare(root.resolve("work"), root.resolve("missing")));
    }

    private static void write(Path root, String relative, String value) throws Exception {
        Path file = root.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, value);
    }
}

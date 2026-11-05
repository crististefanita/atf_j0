package atf.testkit.documentation.guides;

import atf.testkit.AtfUnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class GuideBundleTest {
    @TempDir Path root;

    @Test void missingSelectionCannotBecomeSourceFallbackOrSkip() {
        String previous = System.getProperty("atf.guide.bundle");
        try {
            System.clearProperty("atf.guide.bundle");
            assertTrue(assertThrows(IllegalStateException.class, GuideBundle::selected).getMessage().contains("atf.guide.bundle"));
        } finally {
            if (previous != null) System.setProperty("atf.guide.bundle", previous);
        }
    }

    @Test void realPairsAreValidatedAndMetadataDriftFailsBeforeResolution() throws Exception {
        Path bundle = fixture();
        var artifacts = GuideBundle.publish(bundle, root.resolve("published"));
        assertEquals(4, artifacts.size());
        for (var artifact : artifacts) assertEquals(64, artifact.hash().length());
        Path pom = bundle.resolve("atf-core-fixture.pom");
        Files.writeString(pom, Files.readString(pom).replace("</project>", "<name>changed</name></project>"));
        assertTrue(assertThrows(IllegalStateException.class, () -> GuideBundle.publish(bundle, root.resolve("rejected")))
                .getMessage().contains("POM differ"));
    }

    @Test void missingPairDuplicateVersionAndProducerParentAreRejected() throws Exception {
        Path bundle = fixture();
        Path jar = bundle.resolve("atf-core-fixture.jar");
        byte[] bytes = Files.readAllBytes(jar); Files.delete(jar);
        assertThrows(IllegalStateException.class, () -> GuideBundle.publish(bundle, root.resolve("missing")));
        Files.write(jar, bytes);
        Path duplicate = bundle.resolve("atf-core-old.pom"); Files.copy(bundle.resolve("atf-core-fixture.pom"), duplicate);
        assertThrows(IllegalStateException.class, () -> GuideBundle.publish(bundle, root.resolve("duplicate")));
        Files.delete(duplicate);
        Path pom = bundle.resolve("atf-core-fixture.pom");
        Files.writeString(pom, Files.readString(pom).replace("</project>", "<parent/></project>"));
        assertThrows(IllegalStateException.class, () -> GuideBundle.publish(bundle, root.resolve("parent")));
    }

    private Path fixture() throws Exception {
        Path bundle = Files.createDirectory(root.resolve("lib"));
        for (String id : List.of("core", "reporting", "console", "mock")) {
            String pom = "<project><groupId>atf.api</groupId><artifactId>atf-" + id + "</artifactId><version>fixture</version></project>";
            Files.writeString(bundle.resolve("atf-" + id + "-fixture.pom"), pom);
            try (var output = new JarOutputStream(Files.newOutputStream(bundle.resolve("atf-" + id + "-fixture.jar")))) {
                output.putNextEntry(new JarEntry("META-INF/maven/atf.api/atf-" + id + "/pom.xml"));
                output.write(pom.getBytes(java.nio.charset.StandardCharsets.UTF_8)); output.closeEntry();
            }
        }
        return bundle;
    }
}

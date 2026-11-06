package atf.libraries;

import atf.testkit.AtfUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic artifacts exercise rejection paths; physical consumer CodeSource is verified separately. */
@AtfUnitTest
class PlatformLibraryIntegrityTest {
    @TempDir Path root;

    @Test void rejectsChangedJarPomAndParallelSourceWithoutSuppressingTheOriginalFailure() throws Exception {
        for (String artifact : List.of("console", "mock", "test-kit")) {
            prepare(artifact);
            var surface = new PlatformLibrarySurface(root, artifact); surface.inspect();
            Path jar = surface.jar(); byte[] original = Files.readAllBytes(jar);
            Files.writeString(jar, "changed binary");
            assertEquals("LIB-JAR-HASH", assertThrows(IllegalStateException.class, surface::inspect).getMessage());
            Files.write(jar, original);
            Path pom = jar.resolveSibling(jar.getFileName().toString().replace(".jar", ".pom"));
            String published = Files.readString(pom); Files.writeString(pom, published + "\n");
            assertEquals("LIB-POM-HASH", assertThrows(IllegalStateException.class, surface::inspect).getMessage());
            Files.writeString(pom, published);
            Path shadow = Files.createDirectory(root.resolve("atf-" + artifact));
            assertEquals("LIB-SOURCE-SHADOW", assertThrows(IllegalStateException.class, surface::inspect).getMessage());
            Files.delete(shadow); surface.inspect();
        }
    }

    @Test void rejectsUntrustedNativeCatalogAndEmbeddedPomEvenWhenOuterHashesAreUpdated() throws Exception {
        prepare("mock"); var surface = new PlatformLibrarySurface(root, "mock");
        var mapper = new ObjectMapper(); Path file = root.resolve("library-provenance.json");
        var metadata = mapper.readTree(file.toFile());
        var library = (com.fasterxml.jackson.databind.node.ObjectNode) metadata.path("platformLibraries").path("mock");
        ((com.fasterxml.jackson.databind.node.ObjectNode) library.path("producerInputs"))
                .putArray("nativeTests").addObject().put("path", "../outside.java").put("sha256", "a".repeat(64));
        refreshInputsDigest(library); mapper.writeValue(file.toFile(), metadata);
        assertEquals("LIB-NATIVE-CATALOG", assertThrows(IllegalStateException.class, surface::inspect).getMessage());
        prepare("mock"); pack("mock", "<project/>");
        metadata = mapper.readTree(file.toFile()); library = (com.fasterxml.jackson.databind.node.ObjectNode) metadata.path("platformLibraries").path("mock");
        library.put("binarySha256", PlatformLibrarySurface.hash(surface.jar())); mapper.writeValue(file.toFile(), metadata);
        assertEquals("LIB-EMBEDDED-POM", assertThrows(IllegalStateException.class, surface::inspect).getMessage());
    }

    @Test void consumerCannotBecomeTheLibrarySourceOrFeedbackOwner() throws Exception {
        for (String field : List.of("sourceOwner", "feedbackOwner")) {
            prepare("mock"); var mapper = new ObjectMapper(); Path file = root.resolve("library-provenance.json");
            var metadata = mapper.readTree(file.toFile());
            ((com.fasterxml.jackson.databind.node.ObjectNode) metadata.path("platformLibraryOwnership")).put(field, "atf_j4");
            mapper.writeValue(file.toFile(), metadata);
            assertEquals("LIB-OWNERSHIP", assertThrows(IllegalStateException.class,
                    () -> new PlatformLibrarySurface(root, "mock").inspect()).getMessage());
        }
    }

    private void prepare(String artifact) throws Exception {
        Files.createDirectories(root.resolve("lib"));
        Files.writeString(root.resolve("pom.xml"), "<project><properties><atf.core.version>fixture</atf.core.version><atf."
                + (artifact.equals("test-kit") ? "testkit" : artifact) + ".version>fixture</atf."
                + (artifact.equals("test-kit") ? "testkit" : artifact) + ".version></properties></project>");
        try (var files = Files.list(root.resolve("lib"))) { for (Path file : files.toList()) Files.delete(file); }
        for (String guide : List.of("README.md", "README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md", "README.TestKit.md"))
            Files.writeString(root.resolve("lib").resolve(guide), "# Library usage\n");
        for (String extension : List.of("jar", "pom")) Files.writeString(root.resolve("lib/atf-core-fixture." + extension), "fixture");
        String pom = "<project><groupId>atf.api</groupId><artifactId>atf-" + artifact
                + "</artifactId><version>fixture</version><properties><atf.core.version>fixture</atf.core.version></properties></project>";
        Path publishedPom = root.resolve("lib/atf-" + artifact + "-fixture.pom"); Files.writeString(publishedPom, pom); pack(artifact, pom);
        var mapper = new ObjectMapper(); var metadata = mapper.createObjectNode(); var library = metadata.putObject("platformLibraries").putObject(artifact);
        metadata.putObject("platformLibraryOwnership").put("sourceOwner", "atf_j0").put("feedbackOwner", "atf_j0")
                .put("feedbackRegistry", "atf_j0/docs/feedback/README.md").put("contract", "lib/README.md");
        library.put("coordinate", "atf.api:atf-" + artifact + ":fixture");
        library.put("binarySha256", PlatformLibrarySurface.hash(root.resolve("lib/atf-" + artifact + "-fixture.jar")));
        library.put("publishedPomSha256", PlatformLibrarySurface.hash(publishedPom));
        var inputs = library.putObject("producerInputs"); inputs.put("files", 1); inputs.put("sha256", "a".repeat(64));
        inputs.putArray("nativeTests").addObject().put("path", "src/test/java/NativeTest.java").put("sha256", "b".repeat(64));
        refreshInputsDigest(library); mapper.writeValue(root.resolve("library-provenance.json").toFile(), metadata);
    }
    private void pack(String artifact, String pom) throws Exception {
        try (var archive = new JarOutputStream(Files.newOutputStream(root.resolve("lib/atf-" + artifact + "-fixture.jar")))) {
            archive.putNextEntry(new JarEntry("META-INF/maven/atf.api/atf-" + artifact + "/pom.xml")); archive.write(pom.getBytes(java.nio.charset.StandardCharsets.UTF_8)); archive.closeEntry();
            archive.putNextEntry(new JarEntry((artifact.equals("test-kit") ? "atf/testkit/assertions/RecordedAssertions" : "atf/" + artifact + "/" + (artifact.equals("console") ? "AtfConsoleReporter" : "AtfMockTransport")) + ".class"));
            archive.write(new byte[]{1}); archive.closeEntry();
        }
    }
    private static void refreshInputsDigest(com.fasterxml.jackson.databind.node.ObjectNode library) throws Exception {
        library.put("producerInputsSha256", java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(library.path("producerInputs").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
}

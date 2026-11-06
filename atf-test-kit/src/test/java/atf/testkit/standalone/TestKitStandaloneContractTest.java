package atf.testkit.standalone;

import atf.testkit.AtfUnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source governance stays at J0; the same test catalog also executes against the physical publication. */
@AtfUnitTest
class TestKitStandaloneContractTest {
    private static Path source() {
        return Path.of(System.getProperty("atf.kit.sourceDirectory", ".")).toAbsolutePath().normalize();
    }

    @Test void publicationHasNoParentOrAtfDependency() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var project = factory.newDocumentBuilder().parse(source().resolve("pom.xml").toFile());
        assertEquals(0, project.getElementsByTagName("parent").getLength());
        var dependencies = project.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            var dependency = (org.w3c.dom.Element) dependencies.item(index);
            assertNotEquals("atf.api", dependency.getElementsByTagName("groupId").item(0).getTextContent());
            assertEquals(1, dependency.getElementsByTagName("version").getLength(), "Every vendor version is explicit");
        }
    }

    @Test void implementationImportsNoRendererOrConsumer() throws Exception {
        try (var files = Files.walk(source().resolve("src/main/java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var forbidden = Files.readAllLines(file).stream().filter(line -> line.matches(
                        "\\s*import\\s+(?:static\\s+)?(?:atf\\.(?!testkit\\.)|com\\.microsoft\\.playwright\\.).*")).toList();
                assertTrue(forbidden.isEmpty(), () -> file + ": " + forbidden);
            }
        }
    }

    @Test void physicalPublicationContainsOnlyProductionKitClassesAndItsExactPom() throws Exception {
        Path loaded = Path.of(atf.testkit.assertions.RecordedAssertions.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(loaded)) return;
        Path pom = Path.of(System.getProperty("atf.kit.publicationPom"));
        try (var jar = new JarFile(loaded.toFile())) {
            var classes = jar.stream().filter(e -> e.getName().endsWith(".class")).toList();
            assertFalse(classes.isEmpty());
            assertTrue(classes.stream().allMatch(e -> e.getName().startsWith("atf/testkit/")));
            assertTrue(jar.stream().noneMatch(e -> e.getName().endsWith(".java")));
            Path nativeRoot = source().resolve("src/test/java");
            try (var files = Files.walk(nativeRoot)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList())
                    assertNull(jar.getJarEntry(nativeRoot.relativize(file).toString().replace('\\', '/').replaceAll("\\.java$", ".class")),
                            "Native test/helper leaked into production JAR: " + file);
            }
            var entry = jar.getJarEntry("META-INF/maven/atf.api/atf-test-kit/pom.xml");
            assertNotNull(entry);
            try (var input = jar.getInputStream(entry)) { assertTrue(Arrays.equals(Files.readAllBytes(pom), input.readAllBytes())); }
        }
    }
}

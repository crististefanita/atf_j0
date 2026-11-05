package atf.libraries;

import atf.console.AtfConsoleReporter;
import atf.mock.AtfMockTransport;
import atf.testkit.AtfUnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class PlatformLibraryContractTest {
    @Test void commonLibrariesRetainIndependentPomsAndPhysicalConsumerCodeSources() throws Exception {
        Path root = PlatformLibrarySurface.workspace();
        for (String artifact : List.of("console", "mock")) {
            var surface = new PlatformLibrarySurface(root, artifact);
            surface.inspect();
            surface.assertLoaded(artifact.equals("console") ? AtfConsoleReporter.class : AtfMockTransport.class);
            if (Files.exists(root.resolve("library-provenance.json"))) assertFalse(surface.source(), "Every binary consumer declares both platform libraries");
        }
    }

    @Test void flatLayoutRejectsExtraVersionsMissingPairsAndNestedRepositories(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><properties><atf.console.version>fixture</atf.console.version>"
                + "<atf.mock.version>fixture</atf.mock.version></properties></project>");
        Path lib = Files.createDirectory(root.resolve("lib"));
        for (String guide : List.of("README.md", "README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md")) Files.writeString(lib.resolve(guide), "# Library usage\n");
        for (String artifact : List.of("console", "mock")) for (String extension : List.of("jar", "pom"))
            Files.writeString(lib.resolve("atf-" + artifact + "-fixture." + extension), "fixture");
        PlatformLibrarySurface.assertFlatLayout(root);
        Path old = lib.resolve("atf-console-obsolete.jar"); Files.writeString(old, "old");
        assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root));
        Files.delete(old); Path nested = Files.createDirectory(lib.resolve("atf"));
        assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root));
        Files.delete(nested); Files.delete(lib.resolve("atf-mock-fixture.pom"));
        assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root));
    }

    @Test void theDedicatedGuidesAreRequiredAndDoNotPermitOtherDocumentation(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project><properties><atf.mock.version>fixture</atf.mock.version></properties></project>");
        Path lib = Files.createDirectory(root.resolve("lib"));
        for (String extension : List.of("jar", "pom")) Files.writeString(lib.resolve("atf-mock-fixture." + extension), "fixture");
        assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root), "The usage guides are required");
        var guides = List.of("README.md", "README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md");
        for (String guide : guides) Files.writeString(lib.resolve(guide), "# Usage\n");
        PlatformLibrarySurface.assertFlatLayout(root);
        for (String guide : guides) {
            Path file = lib.resolve(guide); Files.delete(file);
            assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root), "Missing guide: " + guide);
            Files.createDirectory(file);
            assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root), "A directory cannot substitute a guide");
            Files.delete(file); Files.writeString(file, "# Usage\n");
        }
        Path releaseNote = lib.resolve("verification.md"); Files.writeString(releaseNote, "release history");
        assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.assertFlatLayout(root), "Only the index and four named guides are allowed");
    }

    @Test void unsafeAndDuplicateVersionsAreRejectedBeforeResolvingLibraryPaths(@TempDir Path root) throws Exception {
        for (String value : List.of("../outside", "one</atf.mock.version><atf.mock.version>two")) {
            Files.writeString(root.resolve("pom.xml"), "<project><properties><atf.mock.version>" + value + "</atf.mock.version></properties></project>");
            assertThrows(IllegalStateException.class, () -> PlatformLibrarySurface.version(root, "mock"));
        }
    }

    @Test void ordinaryConsoleAndMockUsageDoesNotRequireKitJUnitOrAReportingRenderer() throws Exception {
        var urls = new java.util.LinkedHashSet<java.net.URL>();
        for (String name : List.of("atf.core.http.request.ApiRequest", "atf.console.AtfConsoleReporter", "atf.mock.AtfMockTransport",
                "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core.JsonFactory",
                "com.fasterxml.jackson.annotation.JsonProperty", "com.fasterxml.jackson.dataformat.yaml.YAMLFactory", "org.yaml.snakeyaml.Yaml"))
            urls.add(Class.forName(name).getProtectionDomain().getCodeSource().getLocation());
        try (var isolated = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new), ClassLoader.getPlatformClassLoader())) {
            for (String absent : List.of("atf.testkit.AtfUnitTest", "org.junit.jupiter.api.Test", "atf.reporting.extent.session.ExtentReportSession"))
                assertThrows(ClassNotFoundException.class, () -> isolated.loadClass(absent), absent);
            var settings = isolated.loadClass("atf.console.ConsoleSettings");
            var output = new java.io.ByteArrayOutputStream();
            var console = isolated.loadClass("atf.console.AtfConsoleReporter");
            Object reporter = console.getConstructor(java.io.PrintStream.class, settings)
                    .newInstance(new java.io.PrintStream(output), settings.getMethod("defaults").invoke(null));
            console.getMethod("info", String.class).invoke(reporter, "Standalone console contract");
            assertTrue(output.toString(java.nio.charset.StandardCharsets.UTF_8).contains("Standalone console contract"));
            var mock = isolated.loadClass("atf.mock.AtfMockTransport");
            Object transport = mock.getMethod("defaults").invoke(null);
            Object configuration = mock.getMethod("configuration").invoke(transport);
            assertEquals(9, ((List<?>) configuration.getClass().getMethod("routes").invoke(configuration)).size());
        }
    }
}

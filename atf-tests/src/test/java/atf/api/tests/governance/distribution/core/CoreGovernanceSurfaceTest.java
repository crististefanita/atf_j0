package atf.api.tests.governance.distribution.core;

import atf.api.tests.governance.AtfGovernanceTestSupport;
import atf.api.tests.governance.distribution.GovernanceDeployment;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
public class CoreGovernanceSurfaceTest {
    @TempDir Path root;
    @Test void liveCoreUsesItsDeclaredStandaloneSurface() throws Exception {
        var surface = CoreGovernanceSurface.read(AtfGovernanceTestSupport.locateProjectRoot());
        surface.assertStandalonePom();
        if (!surface.source()) {
            surface.assertBinaryLayout();
            assertTrue(surface.forbiddenReferences(List.of("atf.reporting", "atf.validation", "org.junit", "com.aventstack")).isEmpty());
        }
    }
    @Test void binaryCoreRequiresAnExplicitCoordinateAndRejectsShadowSource() throws Exception {
        new BinaryCoreFixture(root);
        assertFalse(GovernanceDeployment.read(root).coreSource());
        assertEquals(9, GovernanceDeployment.read(root).sourceModules().size());
        assertEquals(List.of("docs/ATF_Core_Consumer.md"), GovernanceDeployment.read(root).documentationTargets("atf-core/README.md"));
        var pom = root.resolve("pom.xml"); var original = Files.readString(pom);
        Files.writeString(pom, original.replace("<atf.core.version>fixture</atf.core.version>", ""));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-REACTOR:"));
        Files.writeString(pom, original); Files.createDirectory(root.resolve("atf-core"));
        assertTrue(assertThrows(IllegalStateException.class, () -> GovernanceDeployment.read(root)).getMessage().startsWith("GOV-CORE-SHADOW:"));
    }
    @ParameterizedTest @ValueSource(strings = {"jar", "pom", "inputs"})
    void rejectsAlteredPublicationBytes(String part) throws Exception {
        var fixture = new BinaryCoreFixture(root);
        if (part.equals("inputs")) {
            Path metadata = root.resolve("library-provenance.json");
            Files.writeString(metadata, Files.readString(metadata).replace("NativeTest.java", "AlteredTest.java"));
        } else Files.write(part.equals("jar") ? fixture.jar : fixture.pom, new byte[]{1}, java.nio.file.StandardOpenOption.APPEND);
        String reason = switch (part) { case "jar" -> "GOV-CORE-HASH"; case "pom" -> "GOV-CORE-POM-HASH"; default -> "GOV-CORE-INPUTS-HASH"; };
        assertEquals(reason, assertThrows(IllegalStateException.class, () -> CoreGovernanceSurface.read(root)).getMessage());
    }
    @Test void acceptsExactlyOneFlatPairPerLibraryAndRejectsNestedOrExtraVersions() throws Exception {
        var fixture = new BinaryCoreFixture(root); CoreGovernanceSurface.read(root);
        Path extra = root.resolve("lib/atf-core-obsolete.jar"); Files.writeString(extra, "old");
        assertEquals("GOV-CORE-FLAT-LIB", assertThrows(IllegalStateException.class, () -> CoreGovernanceSurface.read(root)).getMessage());
        Files.delete(extra); Path nested = Files.createDirectory(root.resolve("lib/atf"));
        assertEquals("GOV-CORE-FLAT-LIB", assertThrows(IllegalStateException.class, () -> CoreGovernanceSurface.read(root)).getMessage());
        Files.move(fixture.jar, nested.resolve(fixture.jar.getFileName()));
        assertThrows(java.nio.file.NoSuchFileException.class, () -> CoreGovernanceSurface.read(root));
    }
    @Test void dependencyDirectionIsCheckedInPomAndBytecode() throws Exception {
        var fixture = new BinaryCoreFixture(root);
        Files.writeString(fixture.pom, Files.readString(fixture.pom).replace("</project>", "<dependencies><dependency>"
                + "<groupId>atf.api</groupId><artifactId>atf-validation</artifactId><version>1</version></dependency></dependencies></project>"));
        fixture.pack(false);
        assertEquals("GOV-CORE-PRODUCTION-DEPENDENCY", assertThrows(IllegalStateException.class, () -> CoreGovernanceSurface.read(root)).getMessage());
        Files.writeString(fixture.pom, Files.readString(fixture.pom).replace("<dependencies>", "<dependencies><!--").replace("</dependencies>", "--></dependencies>"));
        fixture.pack(false); assertTrue(CoreGovernanceSurface.read(root).forbiddenReferences(List.of("java.sql")).isEmpty());
        fixture.pack(true); assertFalse(CoreGovernanceSurface.read(root).forbiddenReferences(List.of("java.sql")).isEmpty());
    }
    @Test void ownershipCannotSubstituteLoadedIdentityOrInventAnAbsentClass() throws Exception {
        new BinaryCoreFixture(root); var surface = CoreGovernanceSurface.read(root);
        assertTrue(surface.owns(root.resolve("atf-core/src/main/java/atf/core/config/module/AtfModuleIds.java")));
        assertFalse(surface.owns(root.resolve("atf-core/src/main/java/atf/core/Absent.java")));
        assertEquals("GOV-CORE-CODESOURCE", assertThrows(IllegalStateException.class, surface::assertBinaryLayout).getMessage());
    }
    @Test void rehashingAnEmptyNativeCatalogCannotAuthorizeIt() throws Exception {
        new BinaryCoreFixture(root); var mapper = new ObjectMapper(); var file = root.resolve("library-provenance.json");
        var metadata = (ObjectNode) mapper.readTree(file.toFile()); var core = (ObjectNode) metadata.path("coreLibrary");
        var inputs = (ObjectNode) core.path("producerInputs"); inputs.remove("nativeTests");
        core.put("producerInputsSha256", BinaryCoreFixture.digest(inputs)); mapper.writeValue(file.toFile(), metadata);
        assertEquals("GOV-CORE-NATIVE-TESTS", assertThrows(IllegalStateException.class, () -> CoreGovernanceSurface.read(root)).getMessage());
    }
}

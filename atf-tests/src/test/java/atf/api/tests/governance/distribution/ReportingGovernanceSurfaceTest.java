package atf.api.tests.governance.distribution;

import atf.api.tests.governance.AtfGovernanceTestSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
public class ReportingGovernanceSurfaceTest {
    @TempDir Path root;

    @Test void flatDistributionCannotSubstituteANestedArtifact() throws Exception {
        var fixture = new BinaryReportingFixture(root);
        assertEquals(root.resolve("lib"), fixture.jar.getParent());
        ReportingGovernanceSurface.read(root).assertStandalonePom();
        Path nested = Files.createDirectories(root.resolve("lib/atf/api/atf-reporting/fixture"));
        Files.move(fixture.jar, nested.resolve(fixture.jar.getFileName()));
        assertThrows(java.nio.file.NoSuchFileException.class, () -> ReportingGovernanceSurface.read(root));
    }

    @Test void aDirectoryCannotSatisfyARequiredSourceFile() throws Exception {
        DeploymentFixture.prepare(root, true);
        Path impostor = root.resolve("atf-reporting/src/main/java/atf/reporting/Impostor.java");
        Files.createDirectories(impostor);
        assertFalse(ReportingGovernanceSurface.read(root).owns(impostor));
    }
    @Test void liveReportingUsesItsDeclaredSurface() throws Exception {
        var surface = ReportingGovernanceSurface.read(AtfGovernanceTestSupport.locateProjectRoot());
        surface.assertStandalonePom();
        if (surface.source()) assertTrue(Files.isRegularFile(AtfGovernanceTestSupport.locateProjectRoot().resolve("atf-reporting/src/main/java/atf/reporting/contract/AtfReportPorts.java")));
        else surface.assertBinaryLayout();
    }
    @ParameterizedTest @ValueSource(strings = {"jar", "pom", "manifest"})
    void alteredPublishedBytesCannotReuseTheOriginalProvenance(String part) throws Exception {
        var fixture = new BinaryReportingFixture(root);
        Path file = switch (part) { case "jar" -> fixture.jar; case "pom" -> fixture.pom; default -> fixture.manifest; };
        Files.write(file, new byte[]{1}, java.nio.file.StandardOpenOption.APPEND);
        String expected = switch (part) { case "jar" -> "GOV-BINARY-HASH"; case "pom" -> "GOV-BINARY-POM-HASH"; default -> "GOV-BINARY-MANIFEST-HASH"; };
        assertEquals(expected, assertThrows(IllegalStateException.class, () -> ReportingGovernanceSurface.read(root)).getMessage());
    }
    @Test void aCoherentButWrongCoordinateIsRejectedBeforeLoading() throws Exception {
        new BinaryReportingFixture(root);
        var pom = root.resolve("pom.xml"); Files.writeString(pom, Files.readString(pom).replace(
                "<atf.reporting.version>fixture</atf.reporting.version>", "<atf.reporting.version>wrong</atf.reporting.version>"));
        assertEquals("GOV-BINARY-COORDINATE", assertThrows(IllegalStateException.class, () -> ReportingGovernanceSurface.read(root)).getMessage());
    }
    @Test void coherentPackagingDoesNotAuthorizeAnAtfProductionDependency() throws Exception {
        var fixture = new BinaryReportingFixture(root); fixture.withAtfDependency();
        assertEquals("GOV-REPORTING-ATF-DEPENDENCY", assertThrows(IllegalStateException.class, () -> ReportingGovernanceSurface.read(root)).getMessage());
    }
    @Test void undeclaredCoreReferencesAreDetectedInTheBytecode() throws Exception {
        var fixture = new BinaryReportingFixture(root);
        assertTrue(ReportingGovernanceSurface.read(root).forbiddenReferences(List.of("atf.core")).isEmpty());
        fixture.withHiddenReference();
        assertFalse(ReportingGovernanceSurface.read(root).forbiddenReferences(List.of("atf.core")).isEmpty(),
                "A clean POM cannot hide a forbidden class dependency");
    }
    @Test void packagedOwnershipAndLoadedIdentityAreIndependentControls() throws Exception {
        new BinaryReportingFixture(root); var surface = ReportingGovernanceSurface.read(root);
        assertTrue(surface.owns(root.resolve("atf-reporting/src/main/java/atf/reporting/contract/AtfReportPorts.java")));
        assertFalse(surface.owns(root.resolve("atf-reporting/src/main/java/atf/reporting/Absent.java")));
        assertEquals("GOV-BINARY-CODESOURCE", assertThrows(IllegalStateException.class, surface::assertBinaryLayout).getMessage());
        assertEquals(java.util.Set.of("NativeTest.java"), surface.nativeTestManifest());
    }
}
